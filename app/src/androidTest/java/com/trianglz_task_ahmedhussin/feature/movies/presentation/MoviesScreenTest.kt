package com.trianglz_task_ahmedhussin.feature.movies.presentation

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.trianglz_task_ahmedhussin.R
import com.trianglz_task_ahmedhussin.core.designsystem.theme.MovieAppTheme
import com.trianglz_task_ahmedhussin.core.domain.error.DomainError
import com.trianglz_task_ahmedhussin.feature.movies.presentation.model.MovieUi
import com.trianglz_task_ahmedhussin.testing.FailOnceThenSucceedPagingSource
import com.trianglz_task_ahmedhussin.testing.errorState
import com.trianglz_task_ahmedhussin.testing.loadStates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Renders the stateless [MoviesScreen] with fixed paging data and load states, so each of the
 * four states (and the footer and cached-data notice) is checked without a network or database.
 */
class MoviesScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val actions = mutableListOf<MoviesUiAction>()

    private val movies = listOf(
        MovieUi(id = 1, title = "First Movie", posterUrl = null, yearText = "2024", ratingText = "7.5"),
        MovieUi(id = 2, title = "Second Movie", posterUrl = null, yearText = null, ratingText = null),
    )

    @Test
    fun loadingStateShowsPlaceholderGrid() {
        setScreen(PagingData.from(emptyList(), sourceLoadStates = loadStates(refresh = LoadState.Loading)))

        composeRule.onNodeWithContentDescription(string(R.string.loading)).assertIsDisplayed()
    }

    @Test
    fun contentStateShowsMovieCards() {
        setScreen(PagingData.from(movies, sourceLoadStates = loadStates()))

        composeRule.onNodeWithText("First Movie").assertIsDisplayed()
        composeRule.onNodeWithText("Second Movie").assertIsDisplayed()
        // The badge replaces its text with a full sentence for screen readers.
        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.rating_content_description, "7.5"))
            .assertIsDisplayed()
    }

    @Test
    fun emptyStateShowsEmptyMessage() {
        setScreen(PagingData.from(emptyList(), sourceLoadStates = loadStates()))

        composeRule.onNodeWithText(string(R.string.movies_empty_title)).assertIsDisplayed()
    }

    @Test
    fun errorStateWithoutCacheShowsErrorMessageAndRetry() {
        setScreen(
            PagingData.from(emptyList(), sourceLoadStates = loadStates(refresh = errorState(DomainError.NoInternet))),
        )

        composeRule.onNodeWithText(string(R.string.error_no_internet)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.action_retry)).assertIsDisplayed()
    }

    @Test
    fun clickingCardSendsMovieClicked() {
        setScreen(PagingData.from(movies, sourceLoadStates = loadStates()))

        composeRule.onNodeWithText("Second Movie").performClick()

        assertEquals(listOf(MoviesUiAction.MovieClicked(2)), actions)
    }

    @Test
    fun appendErrorShowsFooterWithRetry() {
        setScreen(
            PagingData.from(movies, sourceLoadStates = loadStates(append = errorState(DomainError.Server))),
        )

        composeRule.onNodeWithText(string(R.string.error_server)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.action_retry)).assertIsDisplayed()
    }

    @Test
    fun failedRefreshWithCacheShowsNoticeAndKeepsMovies() {
        setScreen(cachedMoviesWithFailedRefresh())

        composeRule.onNodeWithText(string(R.string.movies_cached_notice)).assertIsDisplayed()
        composeRule.onNodeWithText("First Movie").assertIsDisplayed()
    }

    @Test
    fun dismissingNoticeSendsDismissCachedNotice() {
        setScreen(cachedMoviesWithFailedRefresh())

        composeRule.onNodeWithContentDescription(string(R.string.action_dismiss)).performClick()

        assertEquals(listOf(MoviesUiAction.DismissCachedNotice), actions)
    }

    @Test
    fun dismissedNoticeStaysHidden() {
        setScreen(cachedMoviesWithFailedRefresh(), state = MoviesUiState(isCachedNoticeDismissed = true))

        composeRule.onNodeWithText(string(R.string.movies_cached_notice)).assertDoesNotExist()
        composeRule.onNodeWithText("First Movie").assertIsDisplayed()
    }

    /** Retry is wired to `LazyPagingItems.retry()`, so it must re-run the failed load for real. */
    @Test
    fun retryAfterErrorLoadsMovies() {
        val pagingSource = FailOnceThenSucceedPagingSource(movies)
        setScreen(Pager(PagingConfig(pageSize = 20)) { pagingSource }.flow)
        composeRule.onNodeWithText(string(R.string.error_no_internet)).assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.action_retry)).performClick()

        composeRule.onNodeWithText("First Movie").assertIsDisplayed()
        assertEquals(2, pagingSource.loadCount)
    }

    private fun setScreen(pagingData: PagingData<MovieUi>, state: MoviesUiState = MoviesUiState()) =
        setScreen(flowOf(pagingData), state)

    private fun setScreen(pagingFlow: Flow<PagingData<MovieUi>>, state: MoviesUiState = MoviesUiState()) {
        composeRule.setContent {
            MovieAppTheme {
                MoviesScreen(
                    state = state,
                    movies = pagingFlow.collectAsLazyPagingItems(),
                    // Not searching in these tests, so the results list is never shown.
                    searchResults = flowOf(PagingData.empty<MovieUi>()).collectAsLazyPagingItems(),
                    onAction = { actions += it },
                )
            }
        }
    }

    /** Room still has movies, but the RemoteMediator's network refresh failed (R9). */
    private fun cachedMoviesWithFailedRefresh() = PagingData.from(
        data = movies,
        sourceLoadStates = loadStates(),
        mediatorLoadStates = loadStates(refresh = errorState(DomainError.NoInternet)),
    )

    private fun string(@StringRes id: Int): String = composeRule.activity.getString(id)
}
