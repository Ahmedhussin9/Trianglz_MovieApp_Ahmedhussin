package com.trianglz_task_ahmedhussin.feature.movies.di

import com.trianglz_task_ahmedhussin.feature.movies.data.remote.MoviesApi
import com.trianglz_task_ahmedhussin.feature.movies.data.repository.MovieSearchRepositoryImpl
import com.trianglz_task_ahmedhussin.feature.movies.data.repository.MoviesRepositoryImpl
import com.trianglz_task_ahmedhussin.feature.movies.domain.repository.MovieSearchRepository
import com.trianglz_task_ahmedhussin.feature.movies.domain.repository.MoviesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** Dependencies of the movies feature. */
@Module
@InstallIn(SingletonComponent::class)
object MoviesModule {

    @Provides
    @Singleton
    fun provideMoviesApi(retrofit: Retrofit): MoviesApi = retrofit.create(MoviesApi::class.java)

    /** Presentation and domain depend on the interface; only this line knows the implementation. */
    @Provides
    fun provideMoviesRepository(impl: MoviesRepositoryImpl): MoviesRepository = impl

    @Provides
    fun provideMovieSearchRepository(impl: MovieSearchRepositoryImpl): MovieSearchRepository = impl
}
