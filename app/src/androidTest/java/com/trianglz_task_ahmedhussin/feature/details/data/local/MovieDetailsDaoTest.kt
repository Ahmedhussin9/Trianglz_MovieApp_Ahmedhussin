package com.trianglz_task_ahmedhussin.feature.details.data.local

import com.trianglz_task_ahmedhussin.core.database.MovieDatabase
import com.trianglz_task_ahmedhussin.testing.inMemoryDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/** Runs the real SQL and type converters against the device's SQLite engine. */
class MovieDetailsDaoTest {

    private lateinit var database: MovieDatabase
    private lateinit var dao: MovieDetailsDao

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        dao = database.movieDetailsDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun missingMovieReturnsNull() = runTest {
        assertNull(dao.get(1))
    }

    @Test
    fun storedDetailsReadBackWithDateAndGenresIntact() = runTest {
        val details = entity(id = 1)

        dao.upsert(details)

        assertEquals(details, dao.get(1))
    }

    @Test
    fun nullDateAndEmptyGenresSurviveTheRoundTrip() = runTest {
        val details = entity(id = 1).copy(releaseDate = null, genres = emptyList(), tagline = null)

        dao.upsert(details)

        assertEquals(details, dao.get(1))
    }

    @Test
    fun upsertReplacesTheCachedRow() = runTest {
        dao.upsert(entity(id = 1, title = "Old title"))

        dao.upsert(entity(id = 1, title = "New title"))

        assertEquals("New title", dao.get(1)?.title)
    }

    @Test
    fun rowsAreKeptPerMovie() = runTest {
        dao.upsert(entity(id = 1))
        dao.upsert(entity(id = 2))

        assertEquals(1, dao.get(1)?.id)
        assertEquals(2, dao.get(2)?.id)
    }

    private fun entity(id: Int, title: String = "Movie $id") = MovieDetailsEntity(
        id = id,
        title = title,
        tagline = "Tagline $id",
        overview = "Synopsis $id",
        posterUrl = "https://image.tmdb.org/t/p/w500/poster$id.jpg",
        backdropUrl = null,
        releaseDate = LocalDate.of(1995, 10, 30),
        runtimeMinutes = 81,
        rating = 7.97,
        voteCount = 18924,
        genres = listOf(CachedGenre(16, "Animation"), CachedGenre(35, "Comedy")),
        originalLanguage = "en",
        status = "Released",
    )
}
