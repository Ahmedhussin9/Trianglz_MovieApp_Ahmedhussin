package com.trianglz_task_ahmedhussin.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.trianglz_task_ahmedhussin.feature.movies.data.local.MovieDao
import com.trianglz_task_ahmedhussin.feature.movies.data.local.MovieEntity
import com.trianglz_task_ahmedhussin.feature.movies.data.local.RemoteKeyDao
import com.trianglz_task_ahmedhussin.feature.movies.data.local.RemoteKeyEntity

/**
 * The app's single Room database: an offline cache of TMDb data (R7).
 *
 * It lives in `core/` because Room needs one class listing every feature's entities; each
 * feature still owns its own entities and DAOs. The schema is exported to `app/schemas/`.
 */
@Database(
    entities = [MovieEntity::class, RemoteKeyEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class MovieDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
    abstract fun remoteKeyDao(): RemoteKeyDao

    companion object {
        const val NAME = "movies.db"
    }
}
