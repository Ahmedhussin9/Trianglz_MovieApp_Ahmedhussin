package com.trianglz_task_ahmedhussin.feature.movies.presentation

/*
 * MVI contract of the Movies screen (R17):
 *   Model  = MoviesUiState  (immutable, exposed as StateFlow)
 *   Intent = MoviesUiAction (sent through a single onAction())
 *   Effect = MoviesEffect   (one-time events, delivered once through a Channel)
 *
 * The popular list and the search results are separate Flow<PagingData> on the ViewModel:
 * PagingData is a stream with its own load states, not a value that fits inside an immutable
 * state object.
 */

/** Screen state that is not part of the paged lists. */
data class MoviesUiState(
    /** Exactly what is in the search bar, updated on every keystroke. */
    val query: String = "",
    /**
     * Search results are shown instead of the popular list. It follows the debounced, validated
     * query, so the list stays on screen while the user is still typing.
     */
    val isSearching: Boolean = false,
    /** The user closed the "showing saved movies" notice for this session. */
    val isCachedNoticeDismissed: Boolean = false,
)

/** What the user can do on the Movies screen. */
sealed interface MoviesUiAction {
    data class MovieClicked(val movieId: Int) : MoviesUiAction
    data object DismissCachedNotice : MoviesUiAction
    data object SettingsClicked : MoviesUiAction

    data class QueryChanged(val text: String) : MoviesUiAction {
        /** Actions are logged; the search text is user input, so only its length is printed. */
        override fun toString() = "QueryChanged(length=${text.length})"
    }

    data object ClearQuery : MoviesUiAction
}

/** One-time navigation events from the Movies screen. */
sealed interface MoviesEffect {
    data class NavigateToDetails(val movieId: Int) : MoviesEffect
    data object NavigateToSettings : MoviesEffect
}
