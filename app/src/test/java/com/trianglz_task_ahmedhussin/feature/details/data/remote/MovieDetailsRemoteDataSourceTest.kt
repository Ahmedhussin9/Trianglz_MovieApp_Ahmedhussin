package com.trianglz_task_ahmedhussin.feature.details.data.remote

import com.trianglz_task_ahmedhussin.core.common.locale.LocaleProvider
import com.trianglz_task_ahmedhussin.core.domain.error.DomainError
import com.trianglz_task_ahmedhussin.core.domain.result.AppResult
import com.trianglz_task_ahmedhussin.core.network.TmdbHttpClientFactory
import com.trianglz_task_ahmedhussin.feature.details.domain.model.Genre
import com.trianglz_task_ahmedhussin.feature.details.domain.model.MovieDetails
import com.trianglz_task_ahmedhussin.feature.details.domain.model.ReleaseStatus
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Headers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

/** Real Retrofit + JSON + interceptors against a local server returning TMDb-shaped responses. */
class MovieDetailsRemoteDataSourceTest {

    private lateinit var server: MockWebServer
    private lateinit var dataSource: MovieDetailsRemoteDataSource

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val client = TmdbHttpClientFactory.okHttpClient(accessToken = "test", localeProvider = LocaleProvider { Locale.US })
        val api = TmdbHttpClientFactory.retrofit(server.url("/3/").toString(), client).create(MovieDetailsApi::class.java)
        dataSource = MovieDetailsRemoteDataSource(api)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `requests the details endpoint for the movie with auth and language`() = runTest {
        enqueue(200, fixture("tmdb/movie_details.json"))

        dataSource.movieDetails(movieId = 862)

        val request = server.takeRequest()
        assertEquals("/3/movie/862", request.url.encodedPath)
        assertEquals("en-US", request.url.queryParameter("language"))
        assertEquals("Bearer test", request.headers["Authorization"])
    }

    @Test
    fun `parses a real-shaped TMDb response into validated details`() = runTest {
        enqueue(200, fixture("tmdb/movie_details.json"))

        val result = dataSource.movieDetails(movieId = 862)

        assertEquals(
            AppResult.Success(
                MovieDetails(
                    id = 862,
                    title = "Toy Story",
                    tagline = "Hang on for the comedy that goes to infinity and beyond!",
                    overview = "Led by Woody, Andy's toys live happily in his room until Andy's birthday brings Buzz Lightyear onto the scene.",
                    posterUrl = "https://image.tmdb.org/t/p/w500/uXDfjJbdP4ijW5hWSBrPrlKpxab.jpg",
                    backdropUrl = "https://image.tmdb.org/t/p/w1280/3f92DMBTFqr3wgXpfxzrb0R8Kfv.jpg",
                    releaseDate = LocalDate.of(1995, 10, 30),
                    runtimeMinutes = 81,
                    rating = 7.97,
                    voteCount = 18924,
                    genres = listOf(Genre(16, "Animation"), Genre(12, "Adventure"), Genre(10751, "Family"), Genre(35, "Comedy")),
                    originalLanguage = "en",
                    status = ReleaseStatus.Released,
                ),
            ),
            result,
        )
    }

    @Test
    fun `404 becomes NotFound`() = runTest {
        enqueue(404, """{"success":false,"status_code":34,"status_message":"The resource you requested could not be found."}""")

        assertEquals(AppResult.Failure(DomainError.NotFound), dataSource.movieDetails(movieId = 999_999_999))
    }

    @Test
    fun `401 becomes Unauthorized`() = runTest {
        enqueue(401, """{"success":false,"status_code":7,"status_message":"Invalid API key."}""")

        assertEquals(AppResult.Failure(DomainError.Unauthorized), dataSource.movieDetails(movieId = 862))
    }

    @Test
    fun `response without a usable title becomes Unknown`() = runTest {
        enqueue(200, """{"id":862,"title":"  "}""")

        assertEquals(AppResult.Failure(DomainError.Unknown), dataSource.movieDetails(movieId = 862))
    }

    @Test
    fun `malformed JSON becomes Unknown`() = runTest {
        enqueue(200, """{"id":862,"title":""")

        assertEquals(AppResult.Failure(DomainError.Unknown), dataSource.movieDetails(movieId = 862))
    }

    private fun enqueue(code: Int, body: String) {
        server.enqueue(
            MockResponse.Builder()
                .code(code)
                .headers(Headers.headersOf("Content-Type", "application/json"))
                .body(body)
                .build(),
        )
    }

    private fun fixture(path: String): String =
        requireNotNull(javaClass.classLoader?.getResource(path)) { "missing fixture $path" }.readText()
}
