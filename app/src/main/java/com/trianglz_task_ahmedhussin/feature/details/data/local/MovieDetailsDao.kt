package com.trianglz_task_ahmedhussin.feature.details.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

/** Cached details, one row per movie (R7, requirements Q5). */
@Dao
interface MovieDetailsDao {

    @Query("SELECT * FROM movie_details WHERE id = :movieId")
    suspend fun get(movieId: Int): MovieDetailsEntity?

    /** A refresh replaces the whole cached row with the latest details. */
    @Upsert
    suspend fun upsert(details: MovieDetailsEntity)
}
