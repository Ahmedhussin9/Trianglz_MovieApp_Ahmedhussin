package com.trianglz_task_ahmedhussin.feature.movies.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

/** Paging keys of the cached popular list, one row per movie (see `MoviesRemoteMediator`). */
@Dao
interface RemoteKeyDao {

    @Upsert
    suspend fun upsert(key: RemoteKeyEntity)

    @Query("SELECT * FROM remote_keys WHERE list_id = :listId")
    suspend fun get(listId: String): RemoteKeyEntity?
}
