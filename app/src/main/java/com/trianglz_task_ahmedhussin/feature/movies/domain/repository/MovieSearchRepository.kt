package com.trianglz_task_ahmedhussin.feature.movies.domain.repository

import androidx.paging.PagingData
import com.trianglz_task_ahmedhussin.feature.movies.domain.model.Movie
import kotlinx.coroutines.flow.Flow

/**
 * Source of search results (R12). Kept apart from [MoviesRepository]: search is network-only
 * paging while the popular list is cached, and callers of one never need the other.
 */
interface MovieSearchRepository {

    /**
     * Movies whose title matches [query], page by page. [query] is already validated
     * (see `SearchQueryValidator`). Load failures are reported through Paging's
     * `LoadState.Error` carrying a `DomainException`, as for the popular list.
     */
    fun searchMovies(query: String): Flow<PagingData<Movie>>
}
