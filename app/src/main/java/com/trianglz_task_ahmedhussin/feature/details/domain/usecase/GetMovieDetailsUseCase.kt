package com.trianglz_task_ahmedhussin.feature.details.domain.usecase

import com.trianglz_task_ahmedhussin.core.domain.error.DomainError
import com.trianglz_task_ahmedhussin.core.domain.result.AppResult
import com.trianglz_task_ahmedhussin.feature.details.domain.model.MovieDetails
import com.trianglz_task_ahmedhussin.feature.details.domain.repository.MovieDetailsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Returns the details of one movie, cache first (see [MovieDetailsRepository.movieDetails]).
 *
 * TMDb IDs are positive, so a non-positive ID (for example from a malformed deep link or a
 * corrupted saved back stack) can never match a movie. It fails as [DomainError.NotFound]
 * right away instead of costing a database read and a network call.
 */
class GetMovieDetailsUseCase @Inject constructor(
    private val repository: MovieDetailsRepository,
) {
    operator fun invoke(movieId: Int): Flow<AppResult<MovieDetails>> =
        if (movieId > 0) {
            repository.movieDetails(movieId)
        } else {
            flowOf(AppResult.Failure(DomainError.NotFound))
        }
}
