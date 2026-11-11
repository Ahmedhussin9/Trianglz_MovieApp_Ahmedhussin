package com.trianglz_task_ahmedhussin.feature.movies.presentation

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.trianglz_task_ahmedhussin.R
import com.trianglz_task_ahmedhussin.core.designsystem.theme.MovieAppTheme
import com.trianglz_task_ahmedhussin.feature.movies.presentation.model.MovieUi
import com.trianglz_task_ahmedhussin.testing.FailOnceThenSucceedPagingSource
import com.trianglz_task_ahmedhussin.testing.loadStates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The search part of the stateless [MoviesScreen] (R11–R13, R30): the search bar's actions, and
 * which list and state the screen shows while searching.
 */
class SearchScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val actions = mutableListOf<MoviesUiAction>()

    private val popularMovie = MovieUi(id = 1, title = "Popular Movie", posterUrl = null, yearText = "2024", ratingText = "7.0")
    private val searchHit = MovieUi(id = 2, title = "Search Hit", posterUrl = null, yearText = "2023", ratingText = "6.5")

    @Test
    fun typingInTheSearchBarSendsQueryChanged() {
        setScreen(MoviesUiState())

        composeRule.onNode(hasSetTextAction()).performTextInput("coco")

        assertEquals(listOf(MoviesUiAction.QueryChanged("coco")), actions)
    }

    @Test
    fun clearButtonIsHiddenWhenTheSearchBarIsEmpty() {
        setScreen(MoviesUiState(query = ""))

        composeRule.onNodeWithContentDescription(string(R.string.action_clear_search)).assertDoesNotExist()
    }

    @Test
    fun clearButtonShowsWithTextAndSendsClearQuery() {
        setScreen(MoviesUiState(query = "coco"))

        composeRule.onNodeWithContentDescription(string(R.string.action_clear_search)).performClick()

        assertEquals(listOf(MoviesUiAction.ClearQuery), actions)
    }

    @Test
    fun notSearchingShowsThePopularList() {
        setScreen(MoviesUiState(query = "co", isSearching = false))

        composeRule.onNodeWithText(popularMovie.title).assertIsDisplayed()
        composeRule.onNodeWithText(searchHit.title).assertDoesNotExist()
    }

    @Test
    fun searchingShowsTheResultsInsteadOfThePopularList() {
        setScreen(MoviesUiState(query = "hit", isSearching = true))

        composeRule.onNodeWithText(searchHit.title).assertIsDisplayed()
        composeRule.onNodeWithText(popularMovie.title).assertDoesNotExist()
    }

    @Test
    fun noMatchesShowsTheQueryInTheEmptyState() {
        setScreen(
            MoviesUiState(query = " qwxz ", isSearching = true),
            searchResults = flowOf(PagingData.from(emptyList(), sourceLoadStates = loadStates())),
        )

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.search_empty_title, "qwxz")).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.search_empty_message)).assertIsDisplayed()
    }

    @Test
    fun searchErrorShowsRetryWhichLoadsTheResults() {
        val source = FailOnceThenSucceedPagingSource(listOf(searchHit))
        setScreen(
            MoviesUiState(query = "hit", isSearching = true),
            searchResults = Pager(PagingConfig(pageSize = 20)) { source }.flow,
        )
        composeRule.onNodeWithText(string(R.string.error_no_internet)).assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.action_retry)).performClick()

        composeRule.onNodeWithText(searchHit.title).assertIsDisplayed()
        assertEquals(2, source.loadCount)
    }

    @Test
    fun openingASearchResultSendsMovieClicked() {
        setScreen(MoviesUiState(query = "hit", isSearching = true))

        composeRule.onNodeWithText(searchHit.title).performClick()

        assertTrue(actions.contains(MoviesUiAction.MovieClicked(searchHit.id)))
    }

    private fun setScreen(
        state: MoviesUiState,
        searchResults: Flow<PagingData<MovieUi>> = flowOf(PagingData.from(listOf(searchHit), sourceLoadStates = loadStates())),
    ) {
        val movies = flowOf(PagingData.from(listOf(popularMovie), sourceLoadStates = loadStates()))
        composeRule.setContent {
            MovieAppTheme {
                MoviesScreen(
                    state = state,
                    movies = movies.collectAsLazyPagingItems(),
                    searchResults = searchResults.collectAsLazyPagingItems(),
                    onAction = { actions += it },
                )
            }
        }
    }

    private fun string(@StringRes id: Int): String = composeRule.activity.getString(id)
}
