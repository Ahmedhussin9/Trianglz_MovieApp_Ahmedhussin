package com.trianglz_task_ahmedhussin.feature.movies.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.trianglz_task_ahmedhussin.core.database.MovieDatabase
import com.trianglz_task_ahmedhussin.feature.movies.data.mapper.toDomain
import com.trianglz_task_ahmedhussin.feature.movies.data.mediator.MoviesRemoteMediator
import com.trianglz_task_ahmedhussin.feature.movies.data.remote.MoviesRemoteDataSource
import com.trianglz_task_ahmedhussin.feature.movies.domain.model.Movie
import com.trianglz_task_ahmedhussin.feature.movies.domain.repository.MoviesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Offline-first popular movies: Room is the single source of truth for the UI, and
 * [MoviesRemoteMediator] fills it from TMDb (R3, R7–R10, R38).
 */
class MoviesRepositoryImpl @Inject constructor(
    private val dataSource: MoviesRemoteDataSource,
    private val database: MovieDatabase,
) : MoviesRepository {

    @OptIn(ExperimentalPagingApi::class)
    override fun popularMovies(): Flow<PagingData<Movie>> = Pager(
        config = PagingConfig(pageSize = MoviesRemoteDataSource.PAGE_SIZE, enablePlaceholders = false),
        remoteMediator = MoviesRemoteMediator(dataSource, database),
        pagingSourceFactory = { database.movieDao().pagingSource() },
    ).flow.map { pagingData -> pagingData.map { it.toDomain() } }
}
