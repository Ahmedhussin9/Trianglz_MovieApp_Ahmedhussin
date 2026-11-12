package com.trianglz_task_ahmedhussin.feature.details.domain.usecase

import app.cash.turbine.test
import com.trianglz_task_ahmedhussin.core.domain.error.DomainError
import com.trianglz_task_ahmedhussin.core.domain.result.AppResult
import com.trianglz_task_ahmedhussin.feature.details.domain.model.MovieDetails
import com.trianglz_task_ahmedhussin.feature.details.domain.repository.MovieDetailsRepository
import com.trianglz_task_ahmedhussin.testing.TestData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetMovieDetailsUseCaseTest {

    private class FakeMovieDetailsRepository(
        private vararg val emissions: AppResult<MovieDetails>,
    ) : MovieDetailsRepository {
        val requestedIds = mutableListOf<Int>()

        override fun movieDetails(id: Int): Flow<AppResult<MovieDetails>> {
            requestedIds += id
            return flowOf(*emissions)
        }
    }

    @Test
    fun `asks the repository for the requested movie`() = runTest {
        val repository = FakeMovieDetailsRepository(AppResult.Success(TestData.movieDetails(id = 42)))

        GetMovieDetailsUseCase(repository)(movieId = 42).test {
            assertEquals(AppResult.Success(TestData.movieDetails(id = 42)), awaitItem())
            awaitComplete()
        }
        assertEquals(listOf(42), repository.requestedIds)
    }

    @Test
    fun `passes cached then fresh details through in order`() = runTest {
        val cached = TestData.movieDetails(title = "Cached title")
        val fresh = TestData.movieDetails(title = "Fresh title")
        val useCase = GetMovieDetailsUseCase(FakeMovieDetailsRepository(AppResult.Success(cached), AppResult.Success(fresh)))

        useCase(movieId = 1).test {
            assertEquals(AppResult.Success(cached), awaitItem())
            assertEquals(AppResult.Success(fresh), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `passes a failed refresh after cached details through unchanged`() = runTest {
        val cached = TestData.movieDetails()
        val useCase = GetMovieDetailsUseCase(
            FakeMovieDetailsRepository(AppResult.Success(cached), AppResult.Failure(DomainError.NoInternet)),
        )

        useCase(movieId = 1).test {
            assertEquals(AppResult.Success(cached), awaitItem())
            assertEquals(AppResult.Failure(DomainError.NoInternet), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `passes a failure without cache through unchanged`() = runTest {
        val useCase = GetMovieDetailsUseCase(FakeMovieDetailsRepository(AppResult.Failure(DomainError.Server)))

        useCase(movieId = 1).test {
            assertEquals(AppResult.Failure(DomainError.Server), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `a non-positive id fails as NotFound without asking the repository`() = runTest {
        val repository = FakeMovieDetailsRepository(AppResult.Success(TestData.movieDetails()))
        val useCase = GetMovieDetailsUseCase(repository)

        for (invalidId in listOf(0, -1)) {
            useCase(invalidId).test {
                assertEquals(AppResult.Failure(DomainError.NotFound), awaitItem())
                awaitComplete()
            }
        }
        assertEquals(emptyList<Int>(), repository.requestedIds)
    }

    @Test
    fun `each invocation asks the repository for a fresh stream`() {
        val repository = FakeMovieDetailsRepository(AppResult.Success(TestData.movieDetails()))
        val useCase = GetMovieDetailsUseCase(repository)

        useCase(movieId = 1)
        useCase(movieId = 1)

        assertEquals(listOf(1, 1), repository.requestedIds)
    }
}
