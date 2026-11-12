package com.trianglz_task_ahmedhussin.core.network.error

import android.util.Log
import com.trianglz_task_ahmedhussin.core.domain.error.DomainError
import com.trianglz_task_ahmedhussin.core.domain.error.DomainException
import com.trianglz_task_ahmedhussin.testing.RecordingTree
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import timber.log.Timber
import java.net.UnknownHostException

/** Failures must reach Logcat with their root cause; non-failures must not add noise. */
class ErrorLoggingTest {

    private val tree = RecordingTree()

    @Before
    fun setUp() {
        Timber.plant(tree)
    }

    @After
    fun tearDown() {
        Timber.uprootAll()
    }

    @Test
    fun `failed api call logs one warning with the original exception and mapped error`() = runTest {
        val cause = UnknownHostException("api.themoviedb.org")

        safeApiCall<Int> { throw cause }

        val entry = tree.entries.single()
        assertEquals(Log.WARN, entry.priority)
        assertSame(cause, entry.throwable)
        assertTrue(entry.message, entry.message.startsWith("API call failed → NoInternet"))
    }

    @Test
    fun `successful api call logs nothing`() = runTest {
        safeApiCall { 1 }

        assertTrue(tree.entries.isEmpty())
    }

    @Test
    fun `cancellation is not logged as a failure`() = runTest {
        runCatching { safeApiCall<Int> { throw CancellationException("left screen") } }

        assertTrue(tree.entries.isEmpty())
    }

    @Test
    fun `toDomainException logs a raw failure once`() {
        val cause = UnknownHostException()

        cause.toDomainException()

        val entry = tree.entries.single()
        assertEquals(Log.WARN, entry.priority)
        assertSame(cause, entry.throwable)
        assertTrue(entry.message, entry.message.startsWith("Load failed → NoInternet"))
    }

    @Test
    fun `an existing DomainException is not logged again`() {
        DomainException(DomainError.Server).toDomainException()

        assertTrue(tree.entries.isEmpty())
    }
}
