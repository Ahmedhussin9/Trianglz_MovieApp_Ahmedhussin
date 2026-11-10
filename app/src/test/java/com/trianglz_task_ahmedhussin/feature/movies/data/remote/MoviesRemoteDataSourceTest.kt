package com.trianglz_task_ahmedhussin.feature.movies.data.remote

import com.trianglz_task_ahmedhussin.core.common.locale.LocaleProvider
import com.trianglz_task_ahmedhussin.core.domain.error.DomainError
import com.trianglz_task_ahmedhussin.core.network.TmdbHttpClientFactory
import com.trianglz_task_ahmedhussin.core.network.error.toDomainError
import com.trianglz_task_ahmedhussin.feature.movies.domain.model.Movie
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.Locale

/** Real Retrofit + JSON + interceptors against a local server returning TMDb-shaped responses. */
class MoviesRemoteDataSourceTest {

    private lateinit var server: MockWebServer
    private lateinit var dataSource: MoviesRemoteDataSource

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val client = TmdbHttpClientFactory.okHttpClient(accessToken = "test", localeProvider = LocaleProvider { Locale.US })
        val api = TmdbHttpClientFactory.retrofit(server.url("/3/").toString(), client).create(MoviesApi::class.java)
        dataSource = MoviesRemoteDataSource(api)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `requests the discover endpoint sorted by popularity for the given page`() = runTest {
        enqueueJson(page(page = 3, totalPages = 10))

        dataSource.discoverMovies(page = 3)

        val url = server.takeRequest().url
        assertEquals("/3/discover/movie", url.encodedPath)
        assertEquals("3", url.queryParameter("page"))
        assertEquals("popularity.desc", url.queryParameter("sort_by"))
        assertEquals("PG-13", url.queryParameter("certification.lte"))
    }

    @Test
    fun `parses a real-shaped TMDb page and keeps only valid movies in order`() = runTest {
        enqueueJson(fixture("tmdb/discover_movie_page.json"))

        val result = dataSource.discoverMovies(page = 1)

        assertEquals(
            listOf(
                Movie(862, "Toy Story", "https://image.tmdb.org/t/p/w342/uXDfjJbdP4ijW5hWSBrPrlKpxab.jpg", 1995, 7.97),
                Movie(1000001, "A Film Without a Poster", posterUrl = null, releaseYear = null, rating = 0.0),
                Movie(863, "Toy Story 2", "https://image.tmdb.org/t/p/w342/yFWQkz2ynjwsazT6xQiIXEUsyuh.jpg", 1999, 7.6),
            ),
            result.movies,
        )
        assertEquals(1, result.page)
        assertFalse(result.isLastPage)
    }

    @Test
    fun `duplicate ids within a page are kept once`() = runTest {
        enqueueJson(page(page = 1, totalPages = 5, movies = """{"id":1,"title":"A"},{"id":1,"title":"A again"},{"id":2,"title":"B"}"""))

        val result = dataSource.discoverMovies(page = 1)

        assertEquals(listOf(1, 2), result.movies.map { it.id })
        assertEquals("A", result.movies.first().title)
    }

    @Test
    fun `page equal to total_pages is the last page`() = runTest {
        enqueueJson(page(page = 3, totalPages = 3))

        assertTrue(dataSource.discoverMovies(page = 3).isLastPage)
    }

    @Test
    fun `pagination stops at TMDb's 500 page cap even when more pages are reported`() = runTest {
        enqueueJson(page(page = 499, totalPages = 1001))
        enqueueJson(page(page = 500, totalPages = 1001))

        assertFalse(dataSource.discoverMovies(page = 499).isLastPage)
        assertTrue(dataSource.discoverMovies(page = 500).isLastPage)
    }

    @Test
    fun `empty result is an empty last page`() = runTest {
        enqueueJson("""{"page":1,"results":[],"total_pages":0,"total_results":0}""")

        val result = dataSource.discoverMovies(page = 1)

        assertTrue(result.movies.isEmpty())
        assertTrue(result.isLastPage)
    }

    @Test
    fun `http errors propagate and map to the matching domain error`() = runTest {
        server.enqueue(MockResponse.Builder().code(401).body("""{"status_code":7,"status_message":"Invalid API key"}""").build())

        assertEquals(DomainError.Unauthorized, failureOf { dataSource.discoverMovies(page = 1) })
    }

    @Test
    fun `malformed json propagates and maps to Unknown`() = runTest {
        enqueueJson("""{"page": "not-a-number", "results": [}""")

        assertEquals(DomainError.Unknown, failureOf { dataSource.discoverMovies(page = 1) })
    }

    private suspend fun failureOf(block: suspend () -> Unit): DomainError {
        try {
            block()
        } catch (e: Exception) {
            return e.toDomainError()
        }
        fail("expected an exception")
        error("unreachable")
    }

    private fun enqueueJson(body: String) {
        server.enqueue(MockResponse.Builder().code(200).addHeader("Content-Type", "application/json").body(body).build())
    }

    private fun page(page: Int, totalPages: Int, movies: String = """{"id":1,"title":"A"}""") =
        """{"page":$page,"results":[$movies],"total_pages":$totalPages,"total_results":${totalPages * 20}}"""

    private fun fixture(path: String): String =
        requireNotNull(javaClass.classLoader?.getResource(path)) { "missing fixture $path" }.readText()
}
