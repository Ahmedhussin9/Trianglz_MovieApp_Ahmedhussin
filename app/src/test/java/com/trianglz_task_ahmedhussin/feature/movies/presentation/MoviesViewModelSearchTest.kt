package com.trianglz_task_ahmedhussin.feature.movies.presentation

import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import com.trianglz_task_ahmedhussin.feature.movies.domain.model.Movie
import com.trianglz_task_ahmedhussin.feature.movies.domain.repository.MovieSearchRepository
import com.trianglz_task_ahmedhussin.feature.movies.domain.repository.MoviesRepository
import com.trianglz_task_ahmedhussin.feature.movies.domain.usecase.GetMoviesUseCase
import com.trianglz_task_ahmedhussin.feature.movies.domain.usecase.SearchMoviesUseCase
import com.trianglz_task_ahmedhussin.feature.movies.domain.usecase.SearchQueryValidator
import com.trianglz_task_ahmedhussin.testing.MainDispatcherRule
import com.trianglz_task_ahmedhussin.testing.TestData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Search in the Movies ViewModel (R13), checked in virtual time: debounce, de-duplication, cancellation. */
@OptIn(ExperimentalCoroutinesApi::class)
class MoviesViewModelSearchTest {

    /** Standard dispatcher: the tests move virtual time to cross (or not) the debounce window. */
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** Records every query that reaches the repository; each query has its own results. */
    private class FakeMovieSearchRepository : MovieSearchRepository {
        val queries = mutableListOf<String>()

        override fun searchMovies(query: String): Flow<PagingData<Movie>> {
            queries += query
            return flowOf(PagingData.from(listOf(TestData.movie(id = query.hashCode(), title = "Result for $query"))))
        }
    }

    private val popular = object : MoviesRepository {
        override fun popularMovies(): Flow<PagingData<Movie>> = flowOf(PagingData.from(TestData.movies(count = 2)))
    }

    private val search = FakeMovieSearchRepository()

    /** Search runs only while someone collects the results, as the screen does. */
    private fun TestScope.viewModel() = MoviesViewModel(GetMoviesUseCase(popular), SearchMoviesUseCase(search)).also { viewModel ->
        backgroundScope.launch { viewModel.searchResults.collect {} }
    }

    private fun MoviesViewModel.type(text: String) = onAction(MoviesUiAction.QueryChanged(text))

    @Test
    fun `the search bar text updates at once but searching waits for the debounce`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.type("coco")
        advanceTimeBy(DEBOUNCE_MS - 1)
        runCurrent()

        assertEquals("coco", viewModel.state.value.query)
        assertFalse(viewModel.state.value.isSearching)
        assertEquals(emptyList<String>(), search.queries)

        advanceTimeBy(2)
        runCurrent()

        assertTrue(viewModel.state.value.isSearching)
        assertEquals(listOf("coco"), search.queries)
    }

    @Test
    fun `typing fast makes a single request for the final text`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        listOf("c", "co", "coc", "coco").forEach { text ->
            viewModel.type(text)
            advanceTimeBy(100)
        }
        advanceUntilIdle()

        assertEquals(listOf("coco"), search.queries)
    }

    @Test
    fun `the same query after clean-up does not search again`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.type("coco")
        advanceUntilIdle()
        viewModel.type("coco ")
        advanceUntilIdle()
        viewModel.type("  coco")
        advanceUntilIdle()

        assertEquals(listOf("coco"), search.queries)
    }

    @Test
    fun `clearing goes back to the list without waiting`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        viewModel.type("coco")
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isSearching)

        viewModel.onAction(MoviesUiAction.ClearQuery)
        runCurrent()

        assertEquals("", viewModel.state.value.query)
        assertFalse(viewModel.state.value.isSearching)
    }

    @Test
    fun `blank text counts as cleared and never searches`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.type("   ")
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isSearching)
        assertEquals(emptyList<String>(), search.queries)
    }

    @Test
    fun `a new query replaces the previous results`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.type("coco")
        advanceUntilIdle()
        viewModel.type("up")
        advanceUntilIdle()

        assertEquals(listOf("coco", "up"), search.queries)
        val shown = flowOf(viewModel.searchResults.first()).asSnapshot()
        assertEquals(listOf("Result for up"), shown.map { it.title })
    }

    @Test
    fun `the query is capped at the validator's maximum length`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.type("x".repeat(SearchQueryValidator.MAX_LENGTH + 20))

        assertEquals(SearchQueryValidator.MAX_LENGTH, viewModel.state.value.query.length)
    }

    @Test
    fun `the logged action never contains the search text`() {
        assertEquals("QueryChanged(length=6)", MoviesUiAction.QueryChanged("secret").toString())
    }

    private companion object {
        const val DEBOUNCE_MS = 350L
    }
}
