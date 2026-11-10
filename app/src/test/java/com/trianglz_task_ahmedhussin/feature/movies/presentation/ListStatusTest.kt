package com.trianglz_task_ahmedhussin.feature.movies.presentation

import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.LoadStates
import com.trianglz_task_ahmedhussin.core.domain.error.DomainError
import com.trianglz_task_ahmedhussin.core.domain.error.DomainException
import org.junit.Assert.assertEquals
import org.junit.Test

class ListStatusTest {

    private val idle = LoadState.NotLoading(endOfPaginationReached = false)
    private val loading = LoadState.Loading
    private fun failed(error: DomainError) = LoadState.Error(DomainException(error))

    /** Builds load states the way a Pager with a RemoteMediator reports them. */
    private fun states(
        source: LoadState = idle,
        mediator: LoadState = idle,
        append: LoadState = idle,
    ) = CombinedLoadStates(
        refresh = if (mediator !is LoadState.NotLoading) mediator else source,
        prepend = idle,
        append = append,
        source = LoadStates(refresh = source, prepend = idle, append = idle),
        mediator = LoadStates(refresh = mediator, prepend = idle, append = append),
    )

    // With items on screen: always Content; network state is only a notice.

    @Test
    fun `cached items during a background refresh are content that is refreshing`() {
        assertEquals(
            ListStatus.Content(isRefreshing = true, refreshError = null),
            states(mediator = loading).toListStatus(itemCount = 20),
        )
    }

    @Test
    fun `cached items after a failed refresh are content with the refresh error`() {
        assertEquals(
            ListStatus.Content(isRefreshing = false, refreshError = DomainError.NoInternet),
            states(mediator = failed(DomainError.NoInternet)).toListStatus(itemCount = 20),
        )
    }

    @Test
    fun `items with everything idle are plain content`() {
        assertEquals(ListStatus.Content(isRefreshing = false, refreshError = null), states().toListStatus(itemCount = 5))
    }

    // Without items.

    @Test
    fun `no items while the database is loading is Loading`() {
        assertEquals(ListStatus.Loading, states(source = loading).toListStatus(itemCount = 0))
    }

    @Test
    fun `empty cache while the network refresh runs is Loading, not a premature Empty`() {
        assertEquals(ListStatus.Loading, states(mediator = loading).toListStatus(itemCount = 0))
    }

    @Test
    fun `empty cache and a failed refresh is a full screen Error`() {
        assertEquals(
            ListStatus.Error(DomainError.Unauthorized),
            states(mediator = failed(DomainError.Unauthorized)).toListStatus(itemCount = 0),
        )
    }

    @Test
    fun `finished successful load with no rows is Empty`() {
        assertEquals(ListStatus.Empty, states().toListStatus(itemCount = 0))
    }

    @Test
    fun `an error that is not a DomainException is reported as Unknown`() {
        assertEquals(
            ListStatus.Error(DomainError.Unknown),
            states(mediator = LoadState.Error(IllegalStateException())).toListStatus(itemCount = 0),
        )
    }

    // Footer (bottom of the list, R4).

    @Test
    fun `footer follows the append state`() {
        assertEquals(FooterStatus.Loading, states(append = loading).toFooterStatus())
        assertEquals(FooterStatus.Error(DomainError.Timeout), states(append = failed(DomainError.Timeout)).toFooterStatus())
        assertEquals(FooterStatus.None, states().toFooterStatus())
        assertEquals(FooterStatus.None, states(append = LoadState.NotLoading(endOfPaginationReached = true)).toFooterStatus())
    }
}
