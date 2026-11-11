package com.trianglz_task_ahmedhussin.feature.movies.data.remote

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.trianglz_task_ahmedhussin.core.network.error.toDomainException
import com.trianglz_task_ahmedhussin.feature.movies.domain.model.Movie
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException

/**
 * Pages of TMDb search results for one [query] (R12), straight from the network: search results
 * are not cached (only the popular list is, R7).
 *
 * TMDb's ranking can shift between requests, so a movie may come back on two pages. The grid
 * keys items by movie id and a repeated key would crash it, so movies already returned by this
 * source are skipped. A new query or a refresh creates a new source, which starts clean.
 */
class SearchPagingSource(
    private val dataSource: MoviesRemoteDataSource,
    private val query: String,
) : PagingSource<Int, Movie>() {

    private val shownIds = mutableSetOf<Int>()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> {
        val page = params.key ?: FIRST_PAGE
        return try {
            val result = dataSource.searchMovies(query = query, page = page)
            val newMovies = synchronized(shownIds) { result.movies.filter { shownIds.add(it.id) } }
            // The query itself is user input, so only the page and counts are logged.
            Timber.d("Search page %d loaded: %d movies (%d repeated)", page, newMovies.size, result.movies.size - newMovies.size)
            LoadResult.Page(
                data = newMovies,
                prevKey = null,
                nextKey = if (result.isLastPage) null else page + 1,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e.toDomainException())
        }
    }

    /**
     * Always reload from the first page. Results only grow by appending (no prepend), and a
     * refresh starts a new source with no memory of shown movies, so starting mid-list could
     * repeat or skip movies.
     */
    override fun getRefreshKey(state: PagingState<Int, Movie>): Int? = null

    private companion object {
        const val FIRST_PAGE = 1
    }
}
