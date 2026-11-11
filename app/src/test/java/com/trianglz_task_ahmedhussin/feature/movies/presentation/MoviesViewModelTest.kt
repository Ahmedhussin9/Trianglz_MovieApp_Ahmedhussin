package com.trianglz_task_ahmedhussin.feature.movies.presentation

import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import app.cash.turbine.test
import com.trianglz_task_ahmedhussin.feature.movies.domain.model.Movie
import com.trianglz_task_ahmedhussin.feature.movies.domain.repository.MoviesRepository
import com.trianglz_task_ahmedhussin.feature.movies.domain.usecase.GetMoviesUseCase
import com.trianglz_task_ahmedhussin.testing.MainDispatcherRule
import com.trianglz_task_ahmedhussin.testing.TestData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MoviesViewModelTest {

    /** Unconfined: work launched in `viewModelScope` (cachedIn, effects) runs immediately. */
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val movies = listOf(
        TestData.movie(id = 1, title = "Coco", releaseYear = 2017, rating = 8.2),
        TestData.movie(id = 2, title = "Up", releaseYear = null, rating = 0.0),
    )

    private val repository = object : MoviesRepository {
        override fun popularMovies(): Flow<PagingData<Movie>> = flowOf(PagingData.from(movies))
    }

    private fun viewModel() = MoviesViewModel(GetMoviesUseCase(repository))

    /**
     * `asSnapshot()` never completes on the `cachedIn` flow itself (it keeps waiting on the
     * shared upstream), so the first emitted PagingData is snapshotted on its own.
     */
    @Test
    fun `exposes the repository's movies as display models in order`() = runTest(mainDispatcherRule.testDispatcher) {
        val shown = flowOf(viewModel().movies.first()).asSnapshot()

        assertEquals(listOf(1, 2), shown.map { it.id })
        assertEquals("Coco", shown[0].title)
        assertEquals("2017", shown[0].yearText)
        assertEquals(null, shown[1].ratingText)
    }

    @Test
    fun `clicking a movie emits a single navigation effect with its id`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onAction(MoviesUiAction.MovieClicked(movieId = 2))

            assertEquals(MoviesEffect.NavigateToDetails(movieId = 2), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `the settings button emits a single navigation effect`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onAction(MoviesUiAction.SettingsClicked)

            assertEquals(MoviesEffect.NavigateToSettings, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `dismissing the cached notice updates the state`() {
        val viewModel = viewModel()
        assertFalse(viewModel.state.value.isCachedNoticeDismissed)

        viewModel.onAction(MoviesUiAction.DismissCachedNotice)

        assertTrue(viewModel.state.value.isCachedNoticeDismissed)
    }
}
