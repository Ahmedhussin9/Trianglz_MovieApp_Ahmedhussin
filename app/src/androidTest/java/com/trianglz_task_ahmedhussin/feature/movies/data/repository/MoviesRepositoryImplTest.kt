package com.trianglz_task_ahmedhussin.feature.movies.data.repository

import androidx.paging.LoadState
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import androidx.paging.testing.asSnapshot
import com.trianglz_task_ahmedhussin.core.database.MovieDatabase
import com.trianglz_task_ahmedhussin.core.domain.error.DomainError
import com.trianglz_task_ahmedhussin.core.domain.error.DomainException
import com.trianglz_task_ahmedhussin.feature.movies.data.remote.MoviesRemoteDataSource
import com.trianglz_task_ahmedhussin.feature.movies.domain.model.Movie
import com.trianglz_task_ahmedhussin.testing.FakeMoviesApi
import com.trianglz_task_ahmedhussin.testing.inMemoryDatabase
import com.trianglz_task_ahmedhussin.testing.movieEntity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.net.UnknownHostException

/** End-to-end caching flow through the real Pager, mediator and Room (R7–R10). */
class MoviesRepositoryImplTest {

    private lateinit var database: MovieDatabase
    private val api = FakeMoviesApi()
    private lateinit var repository: MoviesRepositoryImpl

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        repository = MoviesRepositoryImpl(MoviesRemoteDataSource(api), database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun firstLaunchOnlineShowsMoviesFromTheNetwork() = runTest {
        api.respond(page = 1, movieIds = listOf(1, 2, 3), totalPages = 1)

        val movies = repository.popularMovies().asSnapshot()

        assertEquals(listOf(1, 2, 3), movies.map { it.id })
    }

    /**
     * Uses Paging's own [PagingDataPresenter] (what `LazyPagingItems` is built on) instead of
     * `asSnapshot()`: the latter throws an internal stub when the RemoteMediator refresh fails,
     * while the presenter shows exactly what the UI would get, data and load states.
     */
    @Test
    fun offlineLaunchShowsTheCachedMoviesAndReportsTheRefreshError() = runTest {
        database.movieDao().insertAll(listOf(movieEntity(id = 10, position = 0), movieEntity(id = 11, position = 1)))
        api.fail(page = 1, error = UnknownHostException("offline"))
        val presenter = object : PagingDataPresenter<Movie>() {
            override suspend fun presentPagingDataEvent(event: PagingDataEvent<Movie>) = Unit
        }

        backgroundScope.launch { repository.popularMovies().collectLatest { presenter.collectFrom(it) } }
        val loadStates = presenter.loadStateFlow.filterNotNull().first {
            it.mediator?.refresh is LoadState.Error && it.source.refresh is LoadState.NotLoading
        }

        assertEquals(listOf(10, 11), presenter.snapshot().items.map { it.id })
        assertEquals("Cached 10", presenter.snapshot().items.first().title)
        val error = (loadStates.mediator?.refresh as LoadState.Error).error as DomainException
        assertEquals(DomainError.NoInternet, error.error)
    }

    @Test
    fun onlineLaunchWithACacheEndsWithTheFreshMovies() = runTest {
        database.movieDao().insertAll(listOf(movieEntity(id = 10, position = 0)))
        api.respond(page = 1, movieIds = listOf(1, 2), totalPages = 1)

        val movies = repository.popularMovies().asSnapshot()

        assertEquals(listOf(1, 2), movies.map { it.id })
        assertEquals("Movie 1", movies.first().title)
    }
}
