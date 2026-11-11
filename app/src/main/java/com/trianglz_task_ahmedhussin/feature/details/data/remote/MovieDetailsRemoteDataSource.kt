package com.trianglz_task_ahmedhussin.feature.details.data.remote

import com.trianglz_task_ahmedhussin.core.domain.error.DomainError
import com.trianglz_task_ahmedhussin.core.domain.result.AppResult
import com.trianglz_task_ahmedhussin.core.network.error.safeApiCall
import com.trianglz_task_ahmedhussin.feature.details.data.mapper.toDomainOrNull
import com.trianglz_task_ahmedhussin.feature.details.domain.model.MovieDetails
import timber.log.Timber
import javax.inject.Inject

/**
 * Fetches one movie's details from TMDb and returns them validated.
 *
 * Unlike the list (where Paging needs exceptions), failures come back as [AppResult.Failure]:
 * network and HTTP errors through [safeApiCall], and a response without a usable id or title
 * as [DomainError.Unknown], the error for malformed responses.
 */
class MovieDetailsRemoteDataSource @Inject constructor(
    private val api: MovieDetailsApi,
) {

    suspend fun movieDetails(movieId: Int): AppResult<MovieDetails> =
        when (val response = safeApiCall { api.movieDetails(movieId) }) {
            is AppResult.Failure -> response
            is AppResult.Success -> response.data.toDomainOrNull()?.let { AppResult.Success(it) }
                ?: AppResult.Failure(DomainError.Unknown).also {
                    Timber.w("Details of movie %d are unusable (missing id or title)", movieId)
                }
        }
}
