package com.trianglz_task_ahmedhussin.testing

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.trianglz_task_ahmedhussin.core.domain.error.DomainError
import com.trianglz_task_ahmedhussin.core.domain.error.DomainException

/**
 * Load states for `PagingData.from` in screen tests. Always pass them: without them
 * `LazyPagingItems.loadState` keeps its initial refresh = Loading, and an empty list would show the
 * skeleton instead of Empty (found in T1.6).
 */
fun loadStates(
    refresh: LoadState = LoadState.NotLoading(endOfPaginationReached = false),
    append: LoadState = LoadState.NotLoading(endOfPaginationReached = true),
) = LoadStates(refresh = refresh, prepend = LoadState.NotLoading(endOfPaginationReached = true), append = append)

/** The data layer always wraps failures in a [DomainException], so screens expect one. */
fun errorState(error: DomainError) = LoadState.Error(DomainException(error))

/**
 * Fails its first load with [DomainError.NoInternet], then returns [items]. Used to prove that a
 * screen's Retry really re-runs the load (it calls `LazyPagingItems.retry()`, not an action).
 */
class FailOnceThenSucceedPagingSource<T : Any>(private val items: List<T>) : PagingSource<Int, T>() {
    var loadCount = 0
        private set

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        loadCount++
        return if (loadCount == 1) {
            LoadResult.Error(DomainException(DomainError.NoInternet))
        } else {
            LoadResult.Page(data = items, prevKey = null, nextKey = null)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, T>): Int? = null
}
