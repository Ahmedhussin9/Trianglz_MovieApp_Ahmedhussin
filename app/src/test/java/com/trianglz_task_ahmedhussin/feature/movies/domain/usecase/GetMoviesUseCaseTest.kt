package com.trianglz_task_ahmedhussin.feature.movies.domain.usecase

import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import com.trianglz_task_ahmedhussin.feature.movies.domain.model.Movie
import com.trianglz_task_ahmedhussin.feature.movies.domain.repository.MoviesRepository
import com.trianglz_task_ahmedhussin.testing.TestData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetMoviesUseCaseTest {

    private class FakeMoviesRepository(private val movies: List<Movie>) : MoviesRepository {
        var calls = 0
            private set

        override fun popularMovies(): Flow<PagingData<Movie>> {
            calls++
            return flowOf(PagingData.from(movies))
        }
    }

    @Test
    fun `returns the repository's movies unchanged and in order`() = runTest {
        val movies = TestData.movies(count = 3)
        val useCase = GetMoviesUseCase(FakeMoviesRepository(movies))

        val snapshot = useCase().asSnapshot()

        assertEquals(movies, snapshot)
    }

    @Test
    fun `an empty repository yields an empty list`() = runTest {
        val useCase = GetMoviesUseCase(FakeMoviesRepository(emptyList()))

        assertTrue(useCase().asSnapshot().isEmpty())
    }

    @Test
    fun `each invocation asks the repository for a fresh stream`() {
        val repository = FakeMoviesRepository(TestData.movies(count = 1))
        val useCase = GetMoviesUseCase(repository)

        useCase()
        useCase()

        assertEquals(2, repository.calls)
    }
}
