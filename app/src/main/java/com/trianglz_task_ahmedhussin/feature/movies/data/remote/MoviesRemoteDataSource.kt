package com.trianglz_task_ahmedhussin.feature.movies.data.remote

import com.trianglz_task_ahmedhussin.feature.movies.data.mapper.toDomainOrNull
import com.trianglz_task_ahmedhussin.feature.movies.data.remote.dto.MoviesPageDto
import javax.inject.Inject

/**
 * Fetches movie pages from TMDb (popular list and title search) and returns them validated.
 *
 * Network and HTTP failures are thrown as-is; the callers (the RemoteMediator and the search
 * PagingSource) convert them with `toDomainException()` so Paging can report a `DomainError`.
 *
 * No dispatcher switch is needed: Retrofit `suspend` calls run I/O and JSON parsing on
 * OkHttp's own threads, so they are already main-safe.
 */
class MoviesRemoteDataSource @Inject constructor(
    private val api: MoviesApi,
) {

    suspend fun discoverMovies(page: Int): MoviesPage =
        api.discoverMovies(page = page, sortBy = SORT_BY_POPULARITY).toMoviesPage(page)

    /** [query] is already validated (`SearchQueryValidator`). */
    suspend fun searchMovies(query: String, page: Int): MoviesPage =
        api.searchMovies(query = query, page = page).toMoviesPage(page)

    /** Both endpoints share the page shape, the item validation and the page cap. */
    private fun MoviesPageDto.toMoviesPage(page: Int) = MoviesPage(
        page = page,
        movies = results.mapNotNull { it.toDomainOrNull() }.distinctBy { it.id },
        isLastPage = page >= minOf(totalPages, MAX_PAGE),
    )

    companion object {
        const val SORT_BY_POPULARITY = "popularity.desc"

        /** TMDb list and search endpoints always return 20 items per page. */
        const val PAGE_SIZE = 20

        /**
         * TMDb reports more pages than it serves (`total_pages` was 1001 in testing), but it
         * rejects `page` > 500, so pagination stops at this cap instead of failing.
         */
        const val MAX_PAGE = 500
    }
}
