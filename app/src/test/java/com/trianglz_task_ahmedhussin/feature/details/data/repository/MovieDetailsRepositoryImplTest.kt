package com.trianglz_task_ahmedhussin.feature.details.data.repository

import app.cash.turbine.test
import com.trianglz_task_ahmedhussin.core.domain.error.DomainError
import com.trianglz_task_ahmedhussin.core.domain.result.AppResult
import com.trianglz_task_ahmedhussin.feature.details.data.local.MovieDetailsDao
import com.trianglz_task_ahmedhussin.feature.details.data.local.MovieDetailsEntity
import com.trianglz_task_ahmedhussin.feature.details.data.mapper.toEntity
import com.trianglz_task_ahmedhussin.feature.details.data.remote.MovieDetailsApi
import com.trianglz_task_ahmedhussin.feature.details.data.remote.MovieDetailsRemoteDataSource
import com.trianglz_task_ahmedhussin.feature.details.data.remote.dto.GenreDto
import com.trianglz_task_ahmedhussin.feature.details.data.remote.dto.MovieDetailsDto
import com.trianglz_task_ahmedhussin.testing.TestData
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

/**
 * Checks every row of the cache/network emission contract with an in-memory DAO and a fake
 * API behind the real remote data source (so the DTO mapping and error mapping run too).
 */
class MovieDetailsRepositoryImplTest {

    private class FakeMovieDetailsDao : MovieDetailsDao {
        val rows = mutableMapOf<Int, MovieDetailsEntity>()
        var failReads = false
        var failWrites = false

        override suspend fun get(movieId: Int): MovieDetailsEntity? {
            if (failReads) throw IllegalStateException("database is broken")
            return rows[movieId]
        }

        override suspend fun upsert(details: MovieDetailsEntity) {
            if (failWrites) throw IllegalStateException("disk is full")
            rows[details.id] = details
        }
    }

    private class FakeMovieDetailsApi : MovieDetailsApi {
        var response: MovieDetailsDto? = null
        var failure: Throwable? = null
        var calls = 0
            private set

        override suspend fun movieDetails(movieId: Int): MovieDetailsDto {
            calls++
            failure?.let { throw it }
            return requireNotNull(response) { "no response set" }
        }
    }

    private val dao = FakeMovieDetailsDao()
    private val api = FakeMovieDetailsApi()
    private val repository = MovieDetailsRepositoryImpl(MovieDetailsRemoteDataSource(api), dao)

    /** What the fake API returns, and the domain model it must map to. */
    private val freshDto = MovieDetailsDto(
        id = 1,
        title = "Fresh title",
        tagline = "A tagline",
        overview = "A synopsis of movie 1.",
        posterPath = "/poster1.jpg",
        backdropPath = "/backdrop1.jpg",
        releaseDate = "2024-03-12",
        runtime = 120,
        voteAverage = 7.5,
        voteCount = 1_000,
        genres = listOf(GenreDto(28, "Action")),
        originalLanguage = "en",
        status = "Released",
    )
    private val fresh = TestData.movieDetails(
        title = "Fresh title",
        posterUrl = "https://image.tmdb.org/t/p/w500/poster1.jpg",
        backdropUrl = "https://image.tmdb.org/t/p/w1280/backdrop1.jpg",
        releaseDate = LocalDate.of(2024, 3, 12),
    )
    private val cached = TestData.movieDetails(title = "Cached title")

    @Test
    fun `no cache and network ok emits fresh details and caches them`() = runTest {
        api.response = freshDto

        repository.movieDetails(1).test {
            assertEquals(AppResult.Success(fresh), awaitItem())
            awaitComplete()
        }
        assertEquals(fresh.toEntity(), dao.rows[1])
    }

    @Test
    fun `no cache and network fails emits only the error`() = runTest {
        api.failure = IOException("offline")

        repository.movieDetails(1).test {
            assertEquals(AppResult.Failure(DomainError.NoInternet), awaitItem())
            awaitComplete()
        }
        assertNull(dao.rows[1])
    }

    @Test
    fun `cache and network ok emits cached then fresh and updates the cache`() = runTest {
        dao.rows[1] = cached.toEntity()
        api.response = freshDto

        repository.movieDetails(1).test {
            assertEquals(AppResult.Success(cached), awaitItem())
            assertEquals(AppResult.Success(fresh), awaitItem())
            awaitComplete()
        }
        assertEquals(fresh.toEntity(), dao.rows[1])
    }

    @Test
    fun `cache and network fails emits cached then the error and keeps the cache`() = runTest {
        dao.rows[1] = cached.toEntity()
        api.failure = IOException("offline")

        repository.movieDetails(1).test {
            assertEquals(AppResult.Success(cached), awaitItem())
            assertEquals(AppResult.Failure(DomainError.NoInternet), awaitItem())
            awaitComplete()
        }
        assertEquals(cached.toEntity(), dao.rows[1])
    }

    @Test
    fun `only the requested movie is read from the cache`() = runTest {
        dao.rows[2] = TestData.movieDetails(id = 2).toEntity()
        api.failure = IOException("offline")

        repository.movieDetails(1).test {
            assertEquals(AppResult.Failure(DomainError.NoInternet), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `an unreadable cache is treated as empty and the network still serves the screen`() = runTest {
        dao.failReads = true
        api.response = freshDto

        repository.movieDetails(1).test {
            assertEquals(AppResult.Success(fresh), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `failing to save fresh details still emits them`() = runTest {
        dao.failWrites = true
        api.response = freshDto

        repository.movieDetails(1).test {
            assertEquals(AppResult.Success(fresh), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `each collection starts a new refresh`() = runTest {
        api.response = freshDto
        val details = repository.movieDetails(1)

        assertEquals(listOf(AppResult.Success(fresh)), details.toList())
        // The second run finds the first run's result in the cache, then refreshes again.
        assertEquals(listOf(AppResult.Success(fresh), AppResult.Success(fresh)), details.toList())
        assertEquals(2, api.calls)
    }
}
