package com.trianglz_task_ahmedhussin.feature.movies.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** The cached popular list (R7), read by Paging and written by `MoviesRemoteMediator`. */
@Dao
interface MovieDao {

    /**
     * The cached list in popularity order. Room invalidates this source whenever the table
     * changes, so a background refresh updates the UI automatically (R10).
     */
    @Query("SELECT * FROM movies ORDER BY position ASC")
    fun pagingSource(): PagingSource<Int, MovieEntity>

    /**
     * TMDb's ranking shifts between requests, so a movie can appear on two pages. `IGNORE`
     * keeps the first occurrence (and its position) instead of moving it down the list.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(movies: List<MovieEntity>)

    @Query("SELECT MAX(position) FROM movies")
    suspend fun maxPosition(): Int?

    @Query("DELETE FROM movies")
    suspend fun clearAll()
}
