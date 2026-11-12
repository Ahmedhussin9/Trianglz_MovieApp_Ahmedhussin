package com.trianglz_task_ahmedhussin.feature.movies.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

/** Where each cached list continues paging from: one row per list, holding its next TMDb page (see `MoviesRemoteMediator`). */
@Dao
interface RemoteKeyDao {

    @Upsert
    suspend fun upsert(key: RemoteKeyEntity)

    @Query("SELECT * FROM remote_keys WHERE list_id = :listId")
    suspend fun get(listId: String): RemoteKeyEntity?
}
