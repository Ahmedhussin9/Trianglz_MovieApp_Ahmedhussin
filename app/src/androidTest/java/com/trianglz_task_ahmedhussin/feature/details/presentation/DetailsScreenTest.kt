package com.trianglz_task_ahmedhussin.feature.details.presentation

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.trianglz_task_ahmedhussin.R
import com.trianglz_task_ahmedhussin.core.designsystem.theme.MovieAppTheme
import com.trianglz_task_ahmedhussin.core.domain.error.DomainError
import com.trianglz_task_ahmedhussin.feature.details.domain.model.Genre
import com.trianglz_task_ahmedhussin.feature.details.domain.model.ReleaseStatus
import com.trianglz_task_ahmedhussin.feature.details.presentation.model.MovieDetailsUi
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Renders the stateless [DetailsScreen] for each state and checks what it shows and reports. */
class DetailsScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val actions = mutableListOf<DetailsUiAction>()

    private val details = MovieDetailsUi(
        id = 862,
        title = "Toy Story",
        tagline = "To infinity and beyond!",
        overview = "Andy's toys come to life. ".repeat(30).trim(),
        posterUrl = null,
        backdropUrl = null,
        releaseDateText = "Oct 30, 1995",
        runtimeMinutes = 81,
        ratingFraction = 0.8f,
        ratingText = "8.0",
        voteCount = 18924,
        voteCountText = "18,924",
        genres = listOf(Genre(16, "Animation"), Genre(35, "Comedy")),
        languageText = "English",
        status = ReleaseStatus.Released,
    )

    @Test
    fun loadingShowsThePlaceholder() {
        setScreen(DetailsUiState(status = DetailsStatus.Loading))

        composeRule.onNodeWithContentDescription(string(R.string.loading)).assertIsDisplayed()
    }

    @Test
    fun contentShowsTheMovieDetails() {
        setScreen(DetailsUiState(status = DetailsStatus.Content(details)))

        composeRule.onNodeWithText("To infinity and beyond!").assertIsDisplayed()
        composeRule.onNodeWithText("Oct 30, 1995 · 1 h 21 min · English").assertIsDisplayed()
        composeRule.onNodeWithText("Animation").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(
                R.string.details_rating_description,
                "8.0",
                composeRule.activity.resources.getQuantityString(R.plurals.details_votes, 18924, "18,924"),
            ),
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun longSynopsisOffersReadMoreWhichSendsToggleSynopsis() {
        setScreen(DetailsUiState(status = DetailsStatus.Content(details)))

        composeRule.onNodeWithText(string(R.string.action_read_more)).performScrollTo().performClick()

        assertEquals(listOf(DetailsUiAction.ToggleSynopsis), actions)
    }

    @Test
    fun expandedSynopsisOffersShowLess() {
        setScreen(DetailsUiState(status = DetailsStatus.Content(details), isSynopsisExpanded = true))

        composeRule.onNodeWithText(string(R.string.action_show_less)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun missingSynopsisShowsAMessage() {
        setScreen(DetailsUiState(status = DetailsStatus.Content(details.copy(overview = null))))

        composeRule.onNodeWithText(string(R.string.details_no_synopsis)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun errorShowsTheMessageAndRetrySendsRetry() {
        setScreen(DetailsUiState(status = DetailsStatus.Error(DomainError.NotFound)))

        composeRule.onNodeWithText(string(R.string.error_not_found)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.action_retry)).performClick()

        assertEquals(listOf(DetailsUiAction.Retry), actions)
    }

    @Test
    fun backButtonSendsBack() {
        setScreen(DetailsUiState(status = DetailsStatus.Content(details)))

        composeRule.onNodeWithContentDescription(string(R.string.action_back)).performClick()

        assertEquals(listOf(DetailsUiAction.Back), actions)
    }

    private fun setScreen(state: DetailsUiState) {
        composeRule.setContent {
            MovieAppTheme {
                DetailsScreen(state = state, snackbarHostState = SnackbarHostState(), onAction = { actions += it })
            }
        }
    }

    private fun string(@StringRes id: Int): String = composeRule.activity.getString(id)
}
