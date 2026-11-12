package com.trianglz_task_ahmedhussin.feature.movies.domain.repository

import androidx.paging.PagingData
import com.trianglz_task_ahmedhussin.feature.movies.domain.model.Movie
import kotlinx.coroutines.flow.Flow

/**
 * Source of movies for the list screen. Implemented in the data layer, which decides
 * where the data comes from (cache and/or network).
 *
 * `PagingData` comes from `androidx.paging:paging-common`, a pure Kotlin library with no
 * Android dependencies, so the domain layer stays platform-free.
 */
interface MoviesRepository {

    /**
     * Popular movies, page by page (R1, R3). Emits cached pages first and updates them
     * when fresher data arrives (R8–R10). Load failures are reported through Paging's
     * `LoadState.Error` carrying a `DomainException`.
     */
    fun popularMovies(): Flow<PagingData<Movie>>
}
