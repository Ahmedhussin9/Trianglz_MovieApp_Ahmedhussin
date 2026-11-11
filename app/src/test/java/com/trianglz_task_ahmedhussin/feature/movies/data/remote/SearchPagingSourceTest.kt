package com.trianglz_task_ahmedhussin.feature.movies.data.remote

import androidx.paging.PagingConfig
import androidx.paging.PagingSource.LoadResult
import androidx.paging.testing.TestPager
import androidx.paging.testing.asSnapshot
import com.trianglz_task_ahmedhussin.core.domain.error.DomainError
import com.trianglz_task_ahmedhussin.core.domain.error.DomainException
import com.trianglz_task_ahmedhussin.feature.movies.data.remote.dto.MovieDto
import com.trianglz_task_ahmedhussin.feature.movies.data.remote.dto.MoviesPageDto
import com.trianglz_task_ahmedhussin.feature.movies.data.repository.MovieSearchRepositoryImpl
import com.trianglz_task_ahmedhussin.feature.movies.domain.model.Movie
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SearchPagingSourceTest {

    /** Serves scripted search pages and records every request. */
    private class FakeMoviesApi(private val totalPages: Int, private val pages: Map<Int, List<Int>>) : MoviesApi {
        val requests = mutableListOf<Pair<String, Int>>()
        var failure: Throwable? = null

        override suspend fun discoverMovies(page: Int, sortBy: String): MoviesPageDto = error("not used by search")

        override suspend fun searchMovies(query: String, page: Int): MoviesPageDto {
            requests += query to page
            failure?.let { throw it }
            val ids = pages[page].orEmpty()
            return MoviesPageDto(page = page, results = ids.map { MovieDto(id = it, title = "Movie $it") }, totalPages = totalPages)
        }
    }

    private val config = PagingConfig(pageSize = MoviesRemoteDataSource.PAGE_SIZE, enablePlaceholders = false)

    private fun pager(api: MoviesApi, query: String = "coco") =
        TestPager(config, SearchPagingSource(MoviesRemoteDataSource(api), query))

    private fun LoadResult<Int, Movie>.page() = this as LoadResult.Page<Int, Movie>

    @Test
    fun `refresh loads the first page for the query`() = runTest {
        val api = FakeMoviesApi(totalPages = 3, pages = mapOf(1 to listOf(1, 2)))

        val page = pager(api).refresh().page()

        assertEquals(listOf(1, 2), page.data.map { it.id })
        assertNull(page.prevKey)
        assertEquals(2, page.nextKey)
        assertEquals(listOf("coco" to 1), api.requests)
    }

    @Test
    fun `append loads the next page`() = runTest {
        val api = FakeMoviesApi(totalPages = 3, pages = mapOf(1 to listOf(1, 2), 2 to listOf(3, 4)))
        val pager = pager(api)
        pager.refresh()

        val page = pager.append()!!.page()

        assertEquals(listOf(3, 4), page.data.map { it.id })
        assertEquals(3, page.nextKey)
    }

    @Test
    fun `the last page has no next key`() = runTest {
        val api = FakeMoviesApi(totalPages = 1, pages = mapOf(1 to listOf(1)))

        assertNull(pager(api).refresh().page().nextKey)
    }

    @Test
    fun `no matches is an empty, final page`() = runTest {
        val api = FakeMoviesApi(totalPages = 0, pages = emptyMap())

        val page = pager(api).refresh().page()

        assertTrue(page.data.isEmpty())
        assertNull(page.nextKey)
    }

    @Test
    fun `a movie already shown on an earlier page is skipped`() = runTest {
        val api = FakeMoviesApi(totalPages = 2, pages = mapOf(1 to listOf(1, 2), 2 to listOf(2, 3)))
        val pager = pager(api)
        pager.refresh()

        val page = pager.append()!!.page()

        assertEquals(listOf(3), page.data.map { it.id })
    }

    @Test
    fun `network failures become a DomainException`() = runTest {
        val api = FakeMoviesApi(totalPages = 1, pages = emptyMap()).apply { failure = IOException("offline") }

        val result = pager(api).refresh() as LoadResult.Error

        assertEquals(DomainError.NoInternet, (result.throwable as DomainException).error)
    }

    @Test
    fun `a refresh always starts again from the first page`() = runTest {
        val api = FakeMoviesApi(totalPages = 3, pages = mapOf(1 to listOf(1), 2 to listOf(2)))
        val pager = pager(api)
        pager.refresh()
        pager.append()

        assertNull(SearchPagingSource(MoviesRemoteDataSource(api), "coco").getRefreshKey(pager.getPagingState(0)))
    }

    @Test
    fun `the repository pages through every result`() = runTest {
        val api = FakeMoviesApi(totalPages = 2, pages = mapOf(1 to (1..20).toList(), 2 to (21..25).toList()))
        val repository = MovieSearchRepositoryImpl(MoviesRemoteDataSource(api))

        val shown = repository.searchMovies("coco").asSnapshot { appendScrollWhile { true } }

        assertEquals((1..25).toList(), shown.map { it.id })
    }
}
