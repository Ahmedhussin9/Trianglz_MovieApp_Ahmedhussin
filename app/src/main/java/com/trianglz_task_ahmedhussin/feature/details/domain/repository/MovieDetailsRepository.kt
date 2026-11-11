package com.trianglz_task_ahmedhussin.feature.details.domain.repository

import com.trianglz_task_ahmedhussin.core.domain.result.AppResult
import com.trianglz_task_ahmedhussin.feature.details.domain.model.MovieDetails
import kotlinx.coroutines.flow.Flow

/**
 * Source of movie details. Implemented in the data layer, which reads the local cache and
 * refreshes it from the network (R7, requirements Q5).
 */
interface MovieDetailsRepository {

    /**
     * Details of the movie with [id], cache first.
     *
     * Emits the cached details first when there are any, then the outcome of the network
     * refresh, and then completes:
     *
     * | Cache | Network | Emissions                                    |
     * |-------|---------|----------------------------------------------|
     * | no    | ok      | `Success(fresh)`                             |
     * | no    | fails   | `Failure(error)`                             |
     * | yes   | ok      | `Success(cached)`, `Success(fresh)`          |
     * | yes   | fails   | `Success(cached)`, `Failure(error)`          |
     *
     * A `Failure` after a `Success` means the refresh failed while cached data is available,
     * so callers keep showing the cached details. Completion marks the end of the refresh,
     * and collecting again starts a new one (Retry).
     */
    fun movieDetails(id: Int): Flow<AppResult<MovieDetails>>
}
