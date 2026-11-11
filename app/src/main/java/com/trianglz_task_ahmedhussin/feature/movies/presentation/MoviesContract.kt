package com.trianglz_task_ahmedhussin.feature.movies.presentation

/*
 * MVI contract of the Movies screen (R17):
 *   Model  = MoviesUiState  (immutable, exposed as StateFlow)
 *   Intent = MoviesUiAction (sent through a single onAction())
 *   Effect = MoviesEffect   (one-time events, delivered once through a Channel)
 *
 * The movie list itself is a separate Flow<PagingData> on the ViewModel: PagingData is a
 * stream with its own load states, not a value that fits inside an immutable state object.
 */

/** Screen state that is not part of the paged list. */
data class MoviesUiState(
    /** The user closed the "showing saved movies" notice for this session. */
    val isCachedNoticeDismissed: Boolean = false,
)

sealed interface MoviesUiAction {
    data class MovieClicked(val movieId: Int) : MoviesUiAction
    data object DismissCachedNotice : MoviesUiAction
    data object SettingsClicked : MoviesUiAction
}

sealed interface MoviesEffect {
    data class NavigateToDetails(val movieId: Int) : MoviesEffect
    data object NavigateToSettings : MoviesEffect
}
