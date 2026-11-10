package com.trianglz_task_ahmedhussin.feature.movies.di

import com.trianglz_task_ahmedhussin.feature.movies.data.remote.MoviesApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** Dependencies of the movies feature. The repository binding is added with the cache (T1.3). */
@Module
@InstallIn(SingletonComponent::class)
object MoviesModule {

    @Provides
    @Singleton
    fun provideMoviesApi(retrofit: Retrofit): MoviesApi = retrofit.create(MoviesApi::class.java)
}
