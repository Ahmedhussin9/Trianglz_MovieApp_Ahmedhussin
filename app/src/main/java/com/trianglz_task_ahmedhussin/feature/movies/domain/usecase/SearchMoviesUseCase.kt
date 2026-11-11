package com.trianglz_task_ahmedhussin.feature.movies.domain.usecase

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import com.trianglz_task_ahmedhussin.feature.movies.domain.model.Movie
import com.trianglz_task_ahmedhussin.feature.movies.domain.repository.MovieSearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Searches movies by title (R12). The raw text is validated here (see [SearchQueryValidator]),
 * so no caller can send a blank or oversized query to TMDb.
 *
 * An idle query returns an empty, finished result without touching the repository.
 */
class SearchMoviesUseCase @Inject constructor(
    private val repository: MovieSearchRepository,
) {
    operator fun invoke(rawQuery: String): Flow<PagingData<Movie>> =
        when (val query = SearchQueryValidator.validate(rawQuery)) {
            SearchQuery.Idle -> flowOf(PagingData.empty(NOTHING_TO_LOAD))
            is SearchQuery.Valid -> repository.searchMovies(query.text)
        }

    private companion object {
        /** Finished with nothing to load, so the UI never shows a spinner for an idle query. */
        val NOTHING_TO_LOAD = LoadStates(
            refresh = LoadState.NotLoading(endOfPaginationReached = true),
            prepend = LoadState.NotLoading(endOfPaginationReached = true),
            append = LoadState.NotLoading(endOfPaginationReached = true),
        )
    }
}
