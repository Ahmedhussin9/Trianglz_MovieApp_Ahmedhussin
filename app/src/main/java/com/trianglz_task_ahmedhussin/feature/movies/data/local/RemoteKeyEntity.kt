package com.trianglz_task_ahmedhussin.feature.movies.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Where to continue loading a cached list from the network.
 *
 * One row per list ([listId]); the popular list is the only one today. Persisting it means a
 * user who scrolled deep before closing the app keeps appending from the right page.
 *
 * @property nextPage next TMDb page to request, or `null` when the end was reached.
 */
@Entity(tableName = "remote_keys")
data class RemoteKeyEntity(
    @PrimaryKey @ColumnInfo(name = "list_id") val listId: String,
    @ColumnInfo(name = "next_page") val nextPage: Int?,
)
