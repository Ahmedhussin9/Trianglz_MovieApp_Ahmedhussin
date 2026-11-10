package com.trianglz_task_ahmedhussin.core.network.error

import com.trianglz_task_ahmedhussin.core.domain.error.DomainError
import com.trianglz_task_ahmedhussin.core.domain.result.AppResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.net.UnknownHostException

class SafeApiCallTest {

    @Test
    fun `successful call returns Success with the value`() = runTest {
        assertEquals(AppResult.Success(42), safeApiCall { 42 })
    }

    @Test
    fun `failing call returns Failure with the mapped domain error`() = runTest {
        val result = safeApiCall<Int> { throw UnknownHostException() }

        assertEquals(AppResult.Failure(DomainError.NoInternet), result)
    }

    @Test
    fun `cancellation is rethrown instead of being reported as an error`() = runTest {
        try {
            safeApiCall<Int> { throw CancellationException("screen closed") }
            fail("CancellationException must propagate")
        } catch (e: CancellationException) {
            assertEquals("screen closed", e.message)
        }
    }
}
