package com.trianglz_task_ahmedhussin.feature.movies.domain.usecase

import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import com.trianglz_task_ahmedhussin.feature.movies.domain.model.Movie
import com.trianglz_task_ahmedhussin.feature.movies.domain.repository.MovieSearchRepository
import com.trianglz_task_ahmedhussin.testing.TestData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchMoviesUseCaseTest {

    private class FakeMovieSearchRepository(private val results: List<Movie>) : MovieSearchRepository {
        val queries = mutableListOf<String>()

        override fun searchMovies(query: String): Flow<PagingData<Movie>> {
            queries += query
            return flowOf(PagingData.from(results))
        }
    }

    @Test
    fun `a valid query returns the repository's results in order`() = runTest {
        val results = TestData.movies(count = 3)
        val useCase = SearchMoviesUseCase(FakeMovieSearchRepository(results))

        assertEquals(results, useCase("coco").asSnapshot())
    }

    @Test
    fun `the repository receives the cleaned-up query`() = runTest {
        val repository = FakeMovieSearchRepository(emptyList())

        SearchMoviesUseCase(repository)("  spider   man ")

        assertEquals(listOf("spider man"), repository.queries)
    }

    @Test
    fun `a blank query returns nothing and never reaches the repository`() = runTest {
        val repository = FakeMovieSearchRepository(TestData.movies(count = 2))

        val shown = SearchMoviesUseCase(repository)("   ").asSnapshot()

        assertTrue(shown.isEmpty())
        assertEquals(emptyList<String>(), repository.queries)
    }

    @Test
    fun `an oversized query is capped before it reaches the repository`() = runTest {
        val repository = FakeMovieSearchRepository(emptyList())

        SearchMoviesUseCase(repository)("x".repeat(500))

        assertEquals(listOf("x".repeat(SearchQueryValidator.MAX_LENGTH)), repository.queries)
    }
}
