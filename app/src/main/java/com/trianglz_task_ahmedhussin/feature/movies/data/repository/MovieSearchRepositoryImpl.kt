package com.trianglz_task_ahmedhussin.feature.movies.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.trianglz_task_ahmedhussin.feature.movies.data.remote.MoviesRemoteDataSource
import com.trianglz_task_ahmedhussin.feature.movies.data.remote.SearchPagingSource
import com.trianglz_task_ahmedhussin.feature.movies.domain.model.Movie
import com.trianglz_task_ahmedhussin.feature.movies.domain.repository.MovieSearchRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Network-only, paged title search (R12) on top of [SearchPagingSource]. */
class MovieSearchRepositoryImpl @Inject constructor(
    private val dataSource: MoviesRemoteDataSource,
) : MovieSearchRepository {

    override fun searchMovies(query: String): Flow<PagingData<Movie>> = Pager(
        config = PagingConfig(pageSize = MoviesRemoteDataSource.PAGE_SIZE, enablePlaceholders = false),
        pagingSourceFactory = { SearchPagingSource(dataSource, query) },
    ).flow
}
