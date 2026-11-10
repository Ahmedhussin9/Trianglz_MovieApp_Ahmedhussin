package com.trianglz_task_ahmedhussin.feature.movies.domain.usecase

import androidx.paging.PagingData
import com.trianglz_task_ahmedhussin.feature.movies.domain.model.Movie
import com.trianglz_task_ahmedhussin.feature.movies.domain.repository.MoviesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Returns the paginated list of popular movies for the home screen.
 *
 * Currently a pass-through, but it is the single entry point the ViewModel depends on, so
 * list rules (for example filtering or sorting) have one obvious home without touching
 * the presentation or data layers.
 */
class GetMoviesUseCase @Inject constructor(
    private val repository: MoviesRepository,
) {
    operator fun invoke(): Flow<PagingData<Movie>> = repository.popularMovies()
}
