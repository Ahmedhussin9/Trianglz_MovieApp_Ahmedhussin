package com.trianglz_task_ahmedhussin.core.network.error

import com.trianglz_task_ahmedhussin.core.domain.error.DomainError
import com.trianglz_task_ahmedhussin.core.domain.error.DomainException
import kotlinx.serialization.SerializationException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ErrorMapperTest {

    @Test
    fun `unknown host maps to NoInternet`() {
        assertEquals(DomainError.NoInternet, UnknownHostException("api.themoviedb.org").toDomainError())
    }

    @Test
    fun `refused connection maps to NoInternet`() {
        assertEquals(DomainError.NoInternet, ConnectException().toDomainError())
    }

    @Test
    fun `any other IOException maps to NoInternet`() {
        assertEquals(DomainError.NoInternet, IOException("unexpected end of stream").toDomainError())
    }

    @Test
    fun `socket timeout maps to Timeout, not NoInternet`() {
        assertEquals(DomainError.Timeout, SocketTimeoutException().toDomainError())
    }

    @Test
    fun `okhttp call timeout maps to Timeout`() {
        assertEquals(DomainError.Timeout, InterruptedIOException("timeout").toDomainError())
    }

    @Test
    fun `http 401 and 403 map to Unauthorized`() {
        assertEquals(DomainError.Unauthorized, httpException(401).toDomainError())
        assertEquals(DomainError.Unauthorized, httpException(403).toDomainError())
    }

    @Test
    fun `http 404 maps to NotFound`() {
        assertEquals(DomainError.NotFound, httpException(404).toDomainError())
    }

    @Test
    fun `http 429 maps to RateLimited`() {
        assertEquals(DomainError.RateLimited, httpException(429).toDomainError())
    }

    @Test
    fun `http 5xx maps to Server`() {
        listOf(500, 502, 503, 599).forEach { code ->
            assertEquals("HTTP $code", DomainError.Server, httpException(code).toDomainError())
        }
    }

    @Test
    fun `other http errors map to Unknown`() {
        listOf(400, 405, 422).forEach { code ->
            assertEquals("HTTP $code", DomainError.Unknown, httpException(code).toDomainError())
        }
    }

    @Test
    fun `malformed json maps to Unknown`() {
        assertEquals(DomainError.Unknown, SerializationException("bad json").toDomainError())
    }

    @Test
    fun `unexpected exception maps to Unknown`() {
        assertEquals(DomainError.Unknown, IllegalStateException().toDomainError())
    }

    @Test
    fun `DomainException keeps its error instead of being remapped`() {
        assertEquals(DomainError.NotFound, DomainException(DomainError.NotFound).toDomainError())
    }

    @Test
    fun `toDomainException wraps the mapped error and keeps the cause`() {
        val cause = UnknownHostException()

        val exception = cause.toDomainException()

        assertEquals(DomainError.NoInternet, exception.error)
        assertSame(cause, exception.cause)
    }

    @Test
    fun `toDomainException returns an existing DomainException unchanged`() {
        val original = DomainException(DomainError.Server)

        assertSame(original, original.toDomainException())
    }

    private fun httpException(code: Int) =
        HttpException(Response.error<Any>(code, "{}".toResponseBody()))
}
