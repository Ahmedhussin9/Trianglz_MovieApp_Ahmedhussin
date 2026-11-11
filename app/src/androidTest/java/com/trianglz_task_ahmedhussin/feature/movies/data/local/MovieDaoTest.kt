package com.trianglz_task_ahmedhussin.feature.movies.data.local

import com.trianglz_task_ahmedhussin.core.database.MovieDatabase
import com.trianglz_task_ahmedhussin.testing.cachedMovies
import com.trianglz_task_ahmedhussin.testing.inMemoryDatabase
import com.trianglz_task_ahmedhussin.testing.movieEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Runs the real SQL against the device's SQLite engine. */
class MovieDaoTest {

    private lateinit var database: MovieDatabase
    private lateinit var movieDao: MovieDao
    private lateinit var remoteKeyDao: RemoteKeyDao

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        movieDao = database.movieDao()
        remoteKeyDao = database.remoteKeyDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun pagingSourceReturnsMoviesOrderedByPositionNotInsertionOrder() = runTest {
        movieDao.insertAll(listOf(movieEntity(id = 3, position = 2), movieEntity(id = 1, position = 0), movieEntity(id = 2, position = 1)))

        assertEquals(listOf(1, 2, 3), database.cachedMovies().map { it.id })
    }

    @Test
    fun duplicateIdKeepsTheFirstRowAndItsPosition() = runTest {
        movieDao.insertAll(listOf(movieEntity(id = 7, position = 0)))

        movieDao.insertAll(listOf(movieEntity(id = 7, position = 25).copy(title = "Later duplicate")))

        val cached = database.cachedMovies().single()
        assertEquals(0, cached.position)
        assertEquals("Cached 7", cached.title)
    }

    @Test
    fun maxPositionIsNullWhenEmptyAndHighestOtherwise() = runTest {
        assertNull(movieDao.maxPosition())

        movieDao.insertAll(listOf(movieEntity(id = 1, position = 0), movieEntity(id = 2, position = 19)))

        assertEquals(19, movieDao.maxPosition())
    }

    @Test
    fun clearAllEmptiesTheCache() = runTest {
        movieDao.insertAll(listOf(movieEntity(id = 1, position = 0)))

        movieDao.clearAll()

        assertTrue(database.cachedMovies().isEmpty())
    }

    @Test
    fun remoteKeyUpsertInsertsThenReplaces() = runTest {
        assertNull(remoteKeyDao.get("popular"))

        remoteKeyDao.upsert(RemoteKeyEntity("popular", nextPage = 2))
        remoteKeyDao.upsert(RemoteKeyEntity("popular", nextPage = 3))

        assertEquals(3, remoteKeyDao.get("popular")?.nextPage)
    }
}
