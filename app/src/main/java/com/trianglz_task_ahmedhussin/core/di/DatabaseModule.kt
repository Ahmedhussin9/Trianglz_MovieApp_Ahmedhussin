package com.trianglz_task_ahmedhussin.core.di

import android.content.Context
import androidx.room.Room
import com.trianglz_task_ahmedhussin.core.database.MovieDatabase
import com.trianglz_task_ahmedhussin.feature.details.data.local.MovieDetailsDao
import com.trianglz_task_ahmedhussin.feature.movies.data.local.MovieDao
import com.trianglz_task_ahmedhussin.feature.movies.data.local.RemoteKeyDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides the single Room database and every feature's DAOs from it. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * The database only holds a cache that can always be re-downloaded, so a schema change
     * may simply drop and recreate the tables instead of requiring hand-written migrations.
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MovieDatabase =
        Room.databaseBuilder(context, MovieDatabase::class.java, MovieDatabase.NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideMovieDao(database: MovieDatabase): MovieDao = database.movieDao()

    @Provides
    fun provideRemoteKeyDao(database: MovieDatabase): RemoteKeyDao = database.remoteKeyDao()

    @Provides
    fun provideMovieDetailsDao(database: MovieDatabase): MovieDetailsDao = database.movieDetailsDao()
}
