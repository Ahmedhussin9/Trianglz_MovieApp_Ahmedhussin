package com.trianglz_task_ahmedhussin.feature.movies.data.remote

import com.trianglz_task_ahmedhussin.feature.movies.data.mapper.toDomainOrNull
import javax.inject.Inject

/**
 * Fetches movie pages from TMDb and returns them validated.
 *
 * Network and HTTP failures are thrown as-is; the caller (the RemoteMediator in T1.3)
 * converts them with `toDomainException()` so Paging can report a `DomainError`.
 *
 * No dispatcher switch is needed: Retrofit `suspend` calls run I/O and JSON parsing on
 * OkHttp's own threads, so they are already main-safe.
 */
class MoviesRemoteDataSource @Inject constructor(
    private val api: MoviesApi,
) {

    suspend fun discoverMovies(page: Int): MoviesPage {
        val response = api.discoverMovies(page = page, sortBy = SORT_BY_POPULARITY)
        val movies = response.results
            .mapNotNull { it.toDomainOrNull() }
            .distinctBy { it.id }
        return MoviesPage(
            page = page,
            movies = movies,
            isLastPage = page >= minOf(response.totalPages, MAX_PAGE),
        )
    }

    companion object {
        const val SORT_BY_POPULARITY = "popularity.desc"

        /**
         * TMDb reports more pages than it serves (`total_pages` was 1001 in testing), but it
         * rejects `page` > 500, so pagination stops at this cap instead of failing.
         */
        const val MAX_PAGE = 500
    }
}
