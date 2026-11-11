package com.trianglz_task_ahmedhussin.feature.movies.data.mediator

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.trianglz_task_ahmedhussin.core.database.MovieDatabase
import com.trianglz_task_ahmedhussin.core.network.error.toDomainException
import com.trianglz_task_ahmedhussin.feature.movies.data.local.MovieEntity
import com.trianglz_task_ahmedhussin.feature.movies.data.local.RemoteKeyEntity
import com.trianglz_task_ahmedhussin.feature.movies.data.mapper.toEntity
import com.trianglz_task_ahmedhussin.feature.movies.data.remote.MoviesRemoteDataSource
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException

/**
 * Keeps the Room cache of popular movies in sync with TMDb (R7–R10).
 *
 * Paging always reads the list from Room; this mediator only writes to Room:
 * - **App start:** [initialize] asks for an immediate refresh. Room's PagingSource shows the
 *   cached list straight away while page 1 is fetched in the background (R8, R9).
 * - **Refresh succeeded:** the old list is replaced in one transaction; Room invalidates the
 *   PagingSource and the UI shows the fresh data (R10).
 * - **Refresh failed:** nothing is deleted, so the cached list stays visible; the error is
 *   reported as `LoadState.Error(DomainException)` for the UI to show non-blockingly.
 * - **Append:** loads the page stored in `remote_keys` and adds it after the cached movies.
 */
@OptIn(ExperimentalPagingApi::class)
class MoviesRemoteMediator(
    private val dataSource: MoviesRemoteDataSource,
    private val database: MovieDatabase,
) : RemoteMediator<Int, MovieEntity>() {

    private val movieDao = database.movieDao()
    private val remoteKeyDao = database.remoteKeyDao()

    override suspend fun initialize(): InitializeAction = InitializeAction.LAUNCH_INITIAL_REFRESH

    override suspend fun load(loadType: LoadType, state: PagingState<Int, MovieEntity>): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> FIRST_PAGE
            // The list only grows downwards; there is never anything before page 1.
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> remoteKeyDao.get(LIST_ID)?.nextPage
                ?: return MediatorResult.Success(endOfPaginationReached = true)
        }

        return try {
            Timber.d("%s: loading page %d", loadType, page)
            val result = dataSource.discoverMovies(page)
            database.withTransaction {
                if (loadType == LoadType.REFRESH) movieDao.clearAll()
                val firstPosition = (movieDao.maxPosition() ?: -1) + 1
                movieDao.insertAll(result.movies.mapIndexed { index, movie -> movie.toEntity(firstPosition + index) })
                remoteKeyDao.upsert(RemoteKeyEntity(LIST_ID, nextPage = if (result.isLastPage) null else page + 1))
            }
            Timber.d("%s: cached %d movies from page %d (last page: %b)", loadType, result.movies.size, page, result.isLastPage)
            MediatorResult.Success(endOfPaginationReached = result.isLastPage)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            MediatorResult.Error(e.toDomainException())
        }
    }

    companion object {
        const val LIST_ID = "popular"
        private const val FIRST_PAGE = 1
    }
}
