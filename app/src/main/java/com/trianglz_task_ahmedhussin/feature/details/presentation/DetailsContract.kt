package com.trianglz_task_ahmedhussin.feature.details.presentation

import com.trianglz_task_ahmedhussin.core.domain.error.DomainError
import com.trianglz_task_ahmedhussin.feature.details.presentation.model.MovieDetailsUi

/*
 * MVI contract of the Details screen (R17):
 *   Model  = DetailsUiState  (immutable, exposed as StateFlow)
 *   Intent = DetailsUiAction (sent through a single onAction())
 *   Effect = DetailsEffect   (one-time events, delivered once through a Channel)
 */

/** Everything the Details screen renders, as one immutable value. */
data class DetailsUiState(
    val status: DetailsStatus = DetailsStatus.Loading,
    /** Cached details are on screen while the network refresh is still running. */
    val isRefreshing: Boolean = false,
    val isSynopsisExpanded: Boolean = false,
)

/** What the main area of the screen shows. Empty does not apply: a missing movie is `Error(NotFound)`. */
sealed interface DetailsStatus {
    data object Loading : DetailsStatus
    data class Content(val details: MovieDetailsUi) : DetailsStatus
    data class Error(val error: DomainError) : DetailsStatus
}

/** What the user can do on the Details screen. */
sealed interface DetailsUiAction {
    /** Loads again: from the full-screen error, or from the refresh-failed snackbar. */
    data object Retry : DetailsUiAction
    data object Back : DetailsUiAction
    data object ToggleSynopsis : DetailsUiAction
}

/** One-time events for the Details screen: navigation and snackbars. */
sealed interface DetailsEffect {
    data object NavigateBack : DetailsEffect

    /** The refresh failed while cached details stay on screen: shown as a snackbar, not an error screen. */
    data class ShowRefreshError(val error: DomainError) : DetailsEffect
}
