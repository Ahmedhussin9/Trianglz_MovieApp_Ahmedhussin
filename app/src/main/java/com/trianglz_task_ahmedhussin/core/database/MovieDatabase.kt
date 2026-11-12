package com.trianglz_task_ahmedhussin.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.trianglz_task_ahmedhussin.feature.details.data.local.MovieDetailsDao
import com.trianglz_task_ahmedhussin.feature.details.data.local.MovieDetailsEntity
import com.trianglz_task_ahmedhussin.feature.movies.data.local.MovieDao
import com.trianglz_task_ahmedhussin.feature.movies.data.local.MovieEntity
import com.trianglz_task_ahmedhussin.feature.movies.data.local.RemoteKeyDao
import com.trianglz_task_ahmedhussin.feature.movies.data.local.RemoteKeyEntity

/**
 * The app's single Room database: an offline cache of TMDb data (R7).
 *
 * It lives in `core/` because Room needs one class listing every feature's entities; each
 * feature still owns its own entities and DAOs. The schema is exported to `app/schemas/`.
 *
 * Version history: 1 = movie list cache (T1.3); 2 = adds `movie_details` (T2.2). Upgrades
 * drop and recreate the tables (see `DatabaseModule`), which only costs a re-download.
 */
@Database(
    entities = [MovieEntity::class, RemoteKeyEntity::class, MovieDetailsEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class MovieDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
    abstract fun remoteKeyDao(): RemoteKeyDao
    abstract fun movieDetailsDao(): MovieDetailsDao

    companion object {
        const val NAME = "movies.db"
    }
}
