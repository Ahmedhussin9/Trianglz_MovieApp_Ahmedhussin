package com.trianglz_task_ahmedhussin.testing

import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.trianglz_task_ahmedhussin.core.database.MovieDatabase
import com.trianglz_task_ahmedhussin.feature.movies.data.local.MovieEntity

/** A fresh in-memory database per test: real SQLite and Room, nothing written to disk. */
fun inMemoryDatabase(): MovieDatabase =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MovieDatabase::class.java).build()

/** Reads the cached list exactly as the UI would: through the DAO's PagingSource, in order. */
suspend fun MovieDatabase.cachedMovies(): List<MovieEntity> {
    val result = movieDao().pagingSource().load(
        PagingSource.LoadParams.Refresh(key = null, loadSize = 1_000, placeholdersEnabled = false),
    )
    return (result as PagingSource.LoadResult.Page).data
}

fun movieEntity(id: Int, position: Int) = MovieEntity(
    id = id,
    title = "Cached $id",
    posterUrl = null,
    releaseYear = 2020,
    rating = 6.0,
    position = position,
)
