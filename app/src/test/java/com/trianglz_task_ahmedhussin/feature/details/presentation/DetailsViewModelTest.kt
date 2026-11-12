package com.trianglz_task_ahmedhussin.feature.details.presentation

import app.cash.turbine.test
import com.trianglz_task_ahmedhussin.core.common.locale.LocaleProvider
import com.trianglz_task_ahmedhussin.core.domain.error.DomainError
import com.trianglz_task_ahmedhussin.core.domain.result.AppResult
import com.trianglz_task_ahmedhussin.feature.details.domain.model.MovieDetails
import com.trianglz_task_ahmedhussin.feature.details.domain.repository.MovieDetailsRepository
import com.trianglz_task_ahmedhussin.feature.details.domain.usecase.GetMovieDetailsUseCase
import com.trianglz_task_ahmedhussin.feature.details.presentation.model.toUi
import com.trianglz_task_ahmedhussin.testing.MainDispatcherRule
import com.trianglz_task_ahmedhussin.testing.TestData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class DetailsViewModelTest {

    /** Standard (not unconfined): tests step through virtual time to see in-between states. */
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** Each call to `movieDetails` returns the next scripted flow. */
    private class FakeMovieDetailsRepository(vararg responses: Flow<AppResult<MovieDetails>>) : MovieDetailsRepository {
        private val queue = ArrayDeque(responses.toList())
        var calls = 0
            private set

        override fun movieDetails(id: Int): Flow<AppResult<MovieDetails>> {
            calls++
            return queue.removeFirst()
        }
    }

    private val locale = Locale.US
    private val cached = TestData.movieDetails(title = "Cached title")
    private val fresh = TestData.movieDetails(title = "Fresh title")

    private fun viewModel(repository: MovieDetailsRepository, movieId: Int = 1) =
        DetailsViewModel(movieId, GetMovieDetailsUseCase(repository), LocaleProvider { locale })

    /** Emits [results] in order, each after a 1 s "network" delay. */
    private fun responses(vararg results: AppResult<MovieDetails>): Flow<AppResult<MovieDetails>> = flow {
        results.forEach {
            delay(NETWORK_DELAY_MS)
            emit(it)
        }
    }

    /** The cached value arrives at once, the network result one second later. */
    private fun cachedThen(networkResult: AppResult<MovieDetails>): Flow<AppResult<MovieDetails>> = flow {
        emit(AppResult.Success(cached))
        delay(NETWORK_DELAY_MS)
        emit(networkResult)
    }

    private fun content(details: MovieDetails) = DetailsStatus.Content(details.toUi(locale))

    @Test
    fun `starts in Loading`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel(FakeMovieDetailsRepository(responses(AppResult.Success(fresh))))
        runCurrent()

        assertEquals(DetailsUiState(status = DetailsStatus.Loading), viewModel.state.value)
    }

    @Test
    fun `no cache and network ok shows the fresh details`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel(FakeMovieDetailsRepository(responses(AppResult.Success(fresh))))
        advanceUntilIdle()

        assertEquals(DetailsUiState(status = content(fresh), isRefreshing = false), viewModel.state.value)
    }

    @Test
    fun `cached details show at once and are replaced by fresh ones`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel(FakeMovieDetailsRepository(cachedThen(AppResult.Success(fresh))))

        runCurrent()
        assertEquals(DetailsUiState(status = content(cached), isRefreshing = true), viewModel.state.value)

        advanceUntilIdle()
        assertEquals(DetailsUiState(status = content(fresh), isRefreshing = false), viewModel.state.value)
    }

    @Test
    fun `failed refresh keeps cached details and sends a snackbar effect`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel(FakeMovieDetailsRepository(cachedThen(AppResult.Failure(DomainError.NoInternet))))

        viewModel.effects.test {
            advanceUntilIdle()

            assertEquals(DetailsEffect.ShowRefreshError(DomainError.NoInternet), awaitItem())
            expectNoEvents()
        }
        assertEquals(DetailsUiState(status = content(cached), isRefreshing = false), viewModel.state.value)
    }

    @Test
    fun `failure without cache shows the error, and Retry loads again`() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeMovieDetailsRepository(
            responses(AppResult.Failure(DomainError.Server)),
            responses(AppResult.Success(fresh)),
        )
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        assertEquals(DetailsStatus.Error(DomainError.Server), viewModel.state.value.status)

        viewModel.onAction(DetailsUiAction.Retry)
        runCurrent()
        assertEquals(DetailsStatus.Loading, viewModel.state.value.status)

        advanceUntilIdle()
        assertEquals(DetailsUiState(status = content(fresh), isRefreshing = false), viewModel.state.value)
        assertEquals(2, repository.calls)
    }

    @Test
    fun `Retry with details on screen keeps them visible while refreshing`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel(
            FakeMovieDetailsRepository(
                cachedThen(AppResult.Failure(DomainError.NoInternet)),
                responses(AppResult.Success(fresh)),
            ),
        )
        advanceUntilIdle()

        viewModel.onAction(DetailsUiAction.Retry)
        runCurrent()
        assertEquals(DetailsUiState(status = content(cached), isRefreshing = true), viewModel.state.value)

        advanceUntilIdle()
        assertEquals(DetailsUiState(status = content(fresh), isRefreshing = false), viewModel.state.value)
    }

    @Test
    fun `Retry while a load is running is ignored`() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeMovieDetailsRepository(responses(AppResult.Success(fresh)))
        val viewModel = viewModel(repository)
        runCurrent()

        viewModel.onAction(DetailsUiAction.Retry)
        viewModel.onAction(DetailsUiAction.Retry)
        advanceUntilIdle()

        assertEquals(1, repository.calls)
        assertEquals(content(fresh), viewModel.state.value.status)
    }

    @Test
    fun `an invalid movie id shows NotFound without asking the repository`() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeMovieDetailsRepository()
        val viewModel = viewModel(repository, movieId = 0)
        advanceUntilIdle()

        assertEquals(DetailsStatus.Error(DomainError.NotFound), viewModel.state.value.status)
        assertEquals(0, repository.calls)
    }

    @Test
    fun `ToggleSynopsis expands and collapses the synopsis`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel(FakeMovieDetailsRepository(responses(AppResult.Success(fresh))))
        assertFalse(viewModel.state.value.isSynopsisExpanded)

        viewModel.onAction(DetailsUiAction.ToggleSynopsis)
        assertTrue(viewModel.state.value.isSynopsisExpanded)

        viewModel.onAction(DetailsUiAction.ToggleSynopsis)
        assertFalse(viewModel.state.value.isSynopsisExpanded)
    }

    @Test
    fun `Back sends a single navigation effect`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel(FakeMovieDetailsRepository(responses(AppResult.Success(fresh))))

        viewModel.effects.test {
            viewModel.onAction(DetailsUiAction.Back)

            assertEquals(DetailsEffect.NavigateBack, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `details are formatted in the locale from the provider`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel(FakeMovieDetailsRepository(responses(AppResult.Success(fresh))))
        advanceUntilIdle()

        val details = (viewModel.state.value.status as DetailsStatus.Content).details
        assertEquals("Mar 12, 2024", details.releaseDateText)
    }

    private companion object {
        const val NETWORK_DELAY_MS = 1_000L
    }
}
