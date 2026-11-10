package com.trianglz_task_ahmedhussin.feature.movies.presentation

import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import com.trianglz_task_ahmedhussin.core.domain.error.DomainError
import com.trianglz_task_ahmedhussin.core.domain.error.DomainException

/**
 * What the whole list area should show. Derived from Paging's load states by [toListStatus],
 * so the screen is a plain `when` over four cases instead of nested load-state checks (R21).
 */
sealed interface ListStatus {
    data object Loading : ListStatus
    data object Empty : ListStatus
    data class Error(val error: DomainError) : ListStatus

    /**
     * @property isRefreshing a background refresh is running while (cached) items are shown.
     * @property refreshError the last background refresh failed; items are from the cache (R9).
     */
    data class Content(val isRefreshing: Boolean, val refreshError: DomainError?) : ListStatus
}

/** What the bottom of the list should show while paging (R4). */
sealed interface FooterStatus {
    data object None : FooterStatus
    data object Loading : FooterStatus
    data class Error(val error: DomainError) : FooterStatus
}

/**
 * Maps Paging's load states to a [ListStatus].
 *
 * With a RemoteMediator two things load independently: the network refresh (`mediator`) and
 * the database read (`source`). Items on screen always win: if any are shown, the status is
 * Content, and network trouble only becomes a non-blocking notice. Without items, a running
 * load means Loading (never a premature Empty), a failure means Error, and only a finished,
 * successful load with no rows means Empty.
 */
fun CombinedLoadStates.toListStatus(itemCount: Int): ListStatus {
    val networkRefresh = mediator?.refresh
    if (itemCount > 0) {
        return ListStatus.Content(
            isRefreshing = networkRefresh is LoadState.Loading,
            refreshError = (networkRefresh as? LoadState.Error)?.domainError(),
        )
    }
    return when {
        refresh is LoadState.Loading || source.refresh is LoadState.Loading || networkRefresh is LoadState.Loading ->
            ListStatus.Loading
        networkRefresh is LoadState.Error -> ListStatus.Error(networkRefresh.domainError())
        refresh is LoadState.Error -> ListStatus.Error((refresh as LoadState.Error).domainError())
        else -> ListStatus.Empty
    }
}

fun CombinedLoadStates.toFooterStatus(): FooterStatus = when (val state = append) {
    is LoadState.Loading -> FooterStatus.Loading
    is LoadState.Error -> FooterStatus.Error(state.domainError())
    is LoadState.NotLoading -> FooterStatus.None
}

/** The data layer always wraps failures in a [DomainException] (`toDomainException`). */
private fun LoadState.Error.domainError(): DomainError = (error as? DomainException)?.error ?: DomainError.Unknown
