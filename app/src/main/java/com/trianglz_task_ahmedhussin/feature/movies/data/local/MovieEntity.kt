package com.trianglz_task_ahmedhussin.feature.movies.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A cached movie of the popular list (R7). Stores already-validated data (see `MovieMapper`).
 *
 * @property position place in TMDb's popularity order across all loaded pages; the list is
 * read `ORDER BY position`, so the cache shows movies in the same order as the API.
 */
@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey val id: Int,
    val title: String,
    @ColumnInfo(name = "poster_url") val posterUrl: String?,
    @ColumnInfo(name = "release_year") val releaseYear: Int?,
    val rating: Double,
    val position: Int,
)
