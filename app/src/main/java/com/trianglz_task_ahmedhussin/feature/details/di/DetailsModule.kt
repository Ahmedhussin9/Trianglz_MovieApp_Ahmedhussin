package com.trianglz_task_ahmedhussin.feature.details.di

import com.trianglz_task_ahmedhussin.feature.details.data.remote.MovieDetailsApi
import com.trianglz_task_ahmedhussin.feature.details.data.repository.MovieDetailsRepositoryImpl
import com.trianglz_task_ahmedhussin.feature.details.domain.repository.MovieDetailsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** Dependencies of the details feature. */
@Module
@InstallIn(SingletonComponent::class)
object DetailsModule {

    @Provides
    @Singleton
    fun provideMovieDetailsApi(retrofit: Retrofit): MovieDetailsApi = retrofit.create(MovieDetailsApi::class.java)

    /** Presentation and domain depend on the interface; only this line knows the implementation. */
    @Provides
    fun provideMovieDetailsRepository(impl: MovieDetailsRepositoryImpl): MovieDetailsRepository = impl
}
