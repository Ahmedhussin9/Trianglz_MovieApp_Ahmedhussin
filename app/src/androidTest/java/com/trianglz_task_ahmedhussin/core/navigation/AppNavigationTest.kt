package com.trianglz_task_ahmedhussin.core.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.test.espresso.Espresso
import com.trianglz_task_ahmedhussin.MainActivity
import com.trianglz_task_ahmedhussin.R
import com.trianglz_task_ahmedhussin.core.domain.error.DomainError
import com.trianglz_task_ahmedhussin.core.domain.result.AppResult
import com.trianglz_task_ahmedhussin.feature.details.di.DetailsModule
import com.trianglz_task_ahmedhussin.feature.details.domain.model.Genre
import com.trianglz_task_ahmedhussin.feature.details.domain.model.MovieDetails
import com.trianglz_task_ahmedhussin.feature.details.domain.model.ReleaseStatus
import com.trianglz_task_ahmedhussin.feature.details.domain.repository.MovieDetailsRepository
import com.trianglz_task_ahmedhussin.feature.movies.di.MoviesModule
import com.trianglz_task_ahmedhussin.feature.movies.domain.model.Movie
import com.trianglz_task_ahmedhussin.feature.movies.domain.repository.MoviesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * End to end through the real app: `MainActivity`, Hilt, Navigation 3, both ViewModels and both
 * screens, with only the two repositories replaced by in-memory fakes (no network, no database),
 * so the test is deterministic and needs no TMDb token (R5, R30).
 *
 * `@UninstallModules` plus a nested module swaps the fakes in for this class only; the rest of
 * the instrumented tests (for example `AppGraphTest`) keep the production bindings.
 */
@HiltAndroidTest
@UninstallModules(MoviesModule::class, DetailsModule::class)
class AppNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Module
    @InstallIn(SingletonComponent::class)
    object FakeRepositoriesModule {

        @Provides
        fun provideMoviesRepository(): MoviesRepository = object : MoviesRepository {
            override fun popularMovies(): Flow<PagingData<Movie>> = flowOf(
                // Explicit load states: without them the list would stay in its initial Loading state.
                PagingData.from(MOVIES, sourceLoadStates = LoadStates(NOT_LOADING, END_REACHED, END_REACHED)),
            )
        }

        @Provides
        fun provideMovieDetailsRepository(): MovieDetailsRepository = object : MovieDetailsRepository {
            override fun movieDetails(id: Int): Flow<AppResult<MovieDetails>> = flowOf(
                MOVIES.firstOrNull { it.id == id }?.let { AppResult.Success(it.toDetails()) }
                    ?: AppResult.Failure(DomainError.NotFound),
            )
        }
    }

    @Test
    fun listShowsTheMovies() {
        waitForText(FIRST.title)

        composeRule.onNodeWithText(SECOND.title).assertIsDisplayed()
    }

    @Test
    fun tappingAMovieOpensItsDetailsAndTheBackButtonReturnsToTheList() {
        waitForText(FIRST.title)

        composeRule.onNodeWithText(FIRST.title).performClick()
        waitForText(taglineOf(FIRST))

        composeRule.onNodeWithContentDescription(string(R.string.action_back)).performClick()
        waitForText(SECOND.title)
        composeRule.onNodeWithText(taglineOf(FIRST)).assertDoesNotExist()
    }

    @Test
    fun systemBackFromDetailsReturnsToTheList() {
        waitForText(FIRST.title)
        composeRule.onNodeWithText(FIRST.title).performClick()
        waitForText(taglineOf(FIRST))

        Espresso.pressBack()

        waitForText(SECOND.title)
        composeRule.onNodeWithText(taglineOf(FIRST)).assertDoesNotExist()
    }

    @Test
    fun eachMovieOpensItsOwnDetails() {
        waitForText(FIRST.title)
        composeRule.onNodeWithText(FIRST.title).performClick()
        waitForText(taglineOf(FIRST))
        Espresso.pressBack()
        waitForText(SECOND.title)

        composeRule.onNodeWithText(SECOND.title).performClick()

        waitForText(taglineOf(SECOND))
        composeRule.onNodeWithText(taglineOf(FIRST)).assertDoesNotExist()
    }

    /** Loads finish asynchronously (Paging, ViewModel coroutines), so wait for the text first. */
    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = WAIT_MS) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun string(@StringRes id: Int): String = composeRule.activity.getString(id)

    private companion object {
        const val WAIT_MS = 5_000L
        val NOT_LOADING = LoadState.NotLoading(endOfPaginationReached = false)
        val END_REACHED = LoadState.NotLoading(endOfPaginationReached = true)

        val FIRST = Movie(id = 1, title = "First Fake Movie", posterUrl = null, releaseYear = 2024, rating = 7.5)
        val SECOND = Movie(id = 2, title = "Second Fake Movie", posterUrl = null, releaseYear = 2023, rating = 6.0)
        val MOVIES = listOf(FIRST, SECOND)

        fun taglineOf(movie: Movie) = "Tagline of ${movie.title}"

        fun Movie.toDetails() = MovieDetails(
            id = id,
            title = title,
            tagline = taglineOf(this),
            overview = "Synopsis of $title.",
            posterUrl = null,
            backdropUrl = null,
            releaseDate = LocalDate.of(releaseYear ?: 2024, 1, 1),
            runtimeMinutes = 100,
            rating = rating,
            voteCount = 100,
            genres = listOf(Genre(id = 18, name = "Drama")),
            originalLanguage = "en",
            status = ReleaseStatus.Released,
        )
    }
}
