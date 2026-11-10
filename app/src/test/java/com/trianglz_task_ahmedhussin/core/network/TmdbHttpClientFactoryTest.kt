package com.trianglz_task_ahmedhussin.core.network

import com.trianglz_task_ahmedhussin.core.common.locale.LocaleProvider
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.http.GET
import java.util.Locale

/**
 * Verifies the real TMDb client configuration against a local MockWebServer:
 * required content filters (R24–R26), authentication, language and safe logging.
 */
class TmdbHttpClientFactoryTest {

    private lateinit var server: MockWebServer
    private var locale: Locale = Locale.US
    private val logLines = mutableListOf<String>()

    private val client: OkHttpClient by lazy {
        TmdbHttpClientFactory.okHttpClient(
            accessToken = TOKEN,
            localeProvider = LocaleProvider { locale },
            httpLogger = { logLines += it },
        )
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `every request carries the three required content filter parameters`() {
        val request = execute("/3/discover/movie?page=2")

        assertEquals("false", request.url.queryParameter("include_adult"))
        assertEquals("US", request.url.queryParameter("certification_country"))
        assertEquals("PG-13", request.url.queryParameter("certification.lte"))
        assertEquals("2", request.url.queryParameter("page"))
    }

    @Test
    fun `content filters are enforced even when a caller passes different values`() {
        val request = execute("/3/search/movie?query=up&include_adult=true&certification.lte=R")

        assertEquals(listOf("false"), request.url.queryParameterValues("include_adult"))
        assertEquals(listOf("PG-13"), request.url.queryParameterValues("certification.lte"))
        assertEquals("up", request.url.queryParameter("query"))
    }

    @Test
    fun `requests are authenticated with the bearer token`() {
        val request = execute("/3/movie/550")

        assertEquals("Bearer $TOKEN", request.headers["Authorization"])
        assertEquals("application/json", request.headers["Accept"])
        assertFalse(request.url.toString().contains(TOKEN))
    }

    @Test
    fun `english locale requests en-US content`() {
        locale = Locale.UK

        assertEquals("en-US", execute("/3/movie/550").url.queryParameter("language"))
    }

    @Test
    fun `arabic locale requests ar-SA content`() {
        locale = Locale.forLanguageTag("ar-EG")

        assertEquals("ar-SA", execute("/3/movie/550").url.queryParameter("language"))
    }

    @Test
    fun `unsupported locale falls back to en-US`() {
        locale = Locale.FRANCE

        assertEquals("en-US", execute("/3/movie/550").url.queryParameter("language"))
    }

    @Test
    fun `language follows a locale change between requests`() {
        assertEquals("en-US", execute("/3/movie/1").url.queryParameter("language"))

        locale = Locale.forLanguageTag("ar")

        assertEquals("ar-SA", execute("/3/movie/1").url.queryParameter("language"))
    }

    @Test
    fun `http logs never contain the access token`() {
        execute("/3/movie/550")

        assertTrue(logLines.any { it.startsWith("Authorization") })
        assertFalse(logLines.any { it.contains(TOKEN) })
    }

    @Test
    fun `retrofit parses json leniently, ignoring unknown keys and coercing nulls to defaults`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body("""{"id": 7, "title": null, "unexpected_field": "ignored"}""")
                .build(),
        )
        val api = TmdbHttpClientFactory.retrofit(server.url("/3/").toString(), client)
            .create(SampleApi::class.java)

        val result = api.sample()

        assertEquals(SampleDto(id = 7, title = ""), result)
    }

    private fun execute(path: String): RecordedRequest {
        server.enqueue(MockResponse.Builder().code(200).body("{}").build())
        client.newCall(Request.Builder().url(server.url(path)).build()).execute().close()
        return server.takeRequest()
    }

    @Serializable
    private data class SampleDto(val id: Int, val title: String = "")

    private interface SampleApi {
        @GET("sample")
        suspend fun sample(): SampleDto
    }

    private companion object {
        const val TOKEN = "test-token-123"
    }
}
