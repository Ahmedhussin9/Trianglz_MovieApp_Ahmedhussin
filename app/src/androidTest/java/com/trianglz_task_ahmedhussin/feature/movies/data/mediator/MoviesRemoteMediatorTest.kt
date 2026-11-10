package com.trianglz_task_ahmedhussin.feature.movies.data.mediator

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator.InitializeAction
import androidx.paging.RemoteMediator.MediatorResult
import com.trianglz_task_ahmedhussin.core.database.MovieDatabase
import com.trianglz_task_ahmedhussin.core.domain.error.DomainError
import com.trianglz_task_ahmedhussin.core.domain.error.DomainException
import com.trianglz_task_ahmedhussin.feature.movies.data.local.MovieEntity
import com.trianglz_task_ahmedhussin.feature.movies.data.local.RemoteKeyEntity
import com.trianglz_task_ahmedhussin.feature.movies.data.remote.MoviesRemoteDataSource
import com.trianglz_task_ahmedhussin.testing.FakeMoviesApi
import com.trianglz_task_ahmedhussin.testing.cachedMovies
import com.trianglz_task_ahmedhussin.testing.inMemoryDatabase
import com.trianglz_task_ahmedhussin.testing.movieEntity
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.UnknownHostException

@OptIn(ExperimentalPagingApi::class)
class MoviesRemoteMediatorTest {

    private lateinit var database: MovieDatabase
    private val api = FakeMoviesApi()
    private lateinit var mediator: MoviesRemoteMediator

    private val emptyState = PagingState<Int, MovieEntity>(
        pages = emptyList(),
        anchorPosition = null,
        config = PagingConfig(pageSize = 20),
        leadingPlaceholderCount = 0,
    )

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        mediator = MoviesRemoteMediator(MoviesRemoteDataSource(api), database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun startupRequestsAnImmediateBackgroundRefresh() = runTest {
        assertEquals(InitializeAction.LAUNCH_INITIAL_REFRESH, mediator.initialize())
    }

    @Test
    fun refreshCachesPageOneInApiOrderAndStoresTheNextPage() = runTest {
        api.respond(page = 1, movieIds = listOf(30, 10, 20), totalPages = 5)

        val result = mediator.load(LoadType.REFRESH, emptyState)

        assertTrue(result is MediatorResult.Success)
        assertFalse((result as MediatorResult.Success).endOfPaginationReached)
        assertEquals(listOf(30, 10, 20), database.cachedMovies().map { it.id })
        assertEquals(2, database.remoteKeyDao().get(MoviesRemoteMediator.LIST_ID)?.nextPage)
    }

    @Test
    fun refreshReplacesTheOldCache() = runTest {
        database.movieDao().insertAll(listOf(movieEntity(id = 99, position = 0)))
        api.respond(page = 1, movieIds = listOf(1, 2), totalPages = 5)

        mediator.load(LoadType.REFRESH, emptyState)

        assertEquals(listOf(1, 2), database.cachedMovies().map { it.id })
    }

    @Test
    fun refreshOfASinglePageListReachesTheEnd() = runTest {
        api.respond(page = 1, movieIds = listOf(1), totalPages = 1)

        val result = mediator.load(LoadType.REFRESH, emptyState) as MediatorResult.Success

        assertTrue(result.endOfPaginationReached)
        assertNull(database.remoteKeyDao().get(MoviesRemoteMediator.LIST_ID)?.nextPage)
    }

    @Test
    fun failedRefreshKeepsTheCachedListAndReportsTheDomainError() = runTest {
        database.movieDao().insertAll(listOf(movieEntity(id = 5, position = 0), movieEntity(id = 6, position = 1)))
        api.fail(page = 1, error = UnknownHostException("offline"))

        val result = mediator.load(LoadType.REFRESH, emptyState)

        val error = (result as MediatorResult.Error).throwable as DomainException
        assertEquals(DomainError.NoInternet, error.error)
        assertEquals(listOf(5, 6), database.cachedMovies().map { it.id })
    }

    @Test
    fun unauthorizedRefreshReportsUnauthorized() = runTest {
        api.fail(page = 1, error = HttpException(Response.error<Any>(401, "{}".toResponseBody())))

        val result = mediator.load(LoadType.REFRESH, emptyState) as MediatorResult.Error

        assertEquals(DomainError.Unauthorized, (result.throwable as DomainException).error)
    }

    @Test
    fun appendLoadsTheStoredNextPageAfterTheCachedMovies() = runTest {
        api.respond(page = 1, movieIds = listOf(1, 2), totalPages = 3)
        api.respond(page = 2, movieIds = listOf(3, 4), totalPages = 3)
        mediator.load(LoadType.REFRESH, emptyState)

        val result = mediator.load(LoadType.APPEND, emptyState) as MediatorResult.Success

        assertFalse(result.endOfPaginationReached)
        assertEquals(listOf(1, 2, 3, 4), database.cachedMovies().map { it.id })
        assertEquals(listOf(0, 1, 2, 3), database.cachedMovies().map { it.position })
        assertEquals(3, database.remoteKeyDao().get(MoviesRemoteMediator.LIST_ID)?.nextPage)
    }

    @Test
    fun movieRepeatedOnTheNextPageKeepsItsOriginalPlace() = runTest {
        api.respond(page = 1, movieIds = listOf(1, 2), totalPages = 3)
        api.respond(page = 2, movieIds = listOf(2, 3), totalPages = 3)
        mediator.load(LoadType.REFRESH, emptyState)

        mediator.load(LoadType.APPEND, emptyState)

        assertEquals(listOf(1, 2, 3), database.cachedMovies().map { it.id })
    }

    @Test
    fun appendAfterTheLastPageMakesNoNetworkCall() = runTest {
        database.remoteKeyDao().upsert(RemoteKeyEntity(MoviesRemoteMediator.LIST_ID, nextPage = null))

        val result = mediator.load(LoadType.APPEND, emptyState) as MediatorResult.Success

        assertTrue(result.endOfPaginationReached)
        assertTrue(api.requestedPages.isEmpty())
    }

    @Test
    fun appendWithoutAnyStoredKeyMakesNoNetworkCall() = runTest {
        val result = mediator.load(LoadType.APPEND, emptyState) as MediatorResult.Success

        assertTrue(result.endOfPaginationReached)
        assertTrue(api.requestedPages.isEmpty())
    }

    @Test
    fun failedAppendKeepsLoadedPagesAndKeyForRetry() = runTest {
        api.respond(page = 1, movieIds = listOf(1, 2), totalPages = 3)
        mediator.load(LoadType.REFRESH, emptyState)
        api.fail(page = 2, error = UnknownHostException("offline"))

        val result = mediator.load(LoadType.APPEND, emptyState)

        assertTrue(result is MediatorResult.Error)
        assertEquals(listOf(1, 2), database.cachedMovies().map { it.id })
        assertEquals(2, database.remoteKeyDao().get(MoviesRemoteMediator.LIST_ID)?.nextPage)
    }

    @Test
    fun prependNeverLoads() = runTest {
        val result = mediator.load(LoadType.PREPEND, emptyState) as MediatorResult.Success

        assertTrue(result.endOfPaginationReached)
        assertTrue(api.requestedPages.isEmpty())
    }
}
