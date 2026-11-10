package com.trianglz_task_ahmedhussin.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.trianglz_task_ahmedhussin.feature.details.presentation.DetailsScreen
import com.trianglz_task_ahmedhussin.feature.movies.presentation.MoviesRoute

/**
 * The app's navigation host.
 *
 * - The back stack is a plain, saveable list of [androidx.navigation3.runtime.NavKey]s:
 *   navigating is `add`, going back is `removeLastOrNull`.
 * - `rememberSaveableStateHolderNavEntryDecorator` keeps each screen's `rememberSaveable`
 *   state (for example scroll position) while it is on the back stack.
 * - `rememberViewModelStoreNavEntryDecorator` scopes ViewModels to their entry, so every
 *   `DetailsRoute(id)` gets its own ViewModel and it is cleared when the entry is popped.
 */
@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(MoviesRoute)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = modifier,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<MoviesRoute> {
                MoviesRoute(onMovieClick = { movieId -> backStack.add(DetailsRoute(movieId)) })
            }
            entry<DetailsRoute> { route ->
                DetailsScreen(
                    movieId = route.movieId,
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
