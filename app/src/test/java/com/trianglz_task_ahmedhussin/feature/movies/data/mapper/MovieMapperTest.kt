package com.trianglz_task_ahmedhussin.feature.movies.data.mapper

import com.trianglz_task_ahmedhussin.feature.movies.data.remote.dto.MovieDto
import com.trianglz_task_ahmedhussin.feature.movies.domain.model.Movie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MovieMapperTest {

    private val valid = MovieDto(
        id = 862,
        title = "Toy Story",
        posterPath = "/poster.jpg",
        releaseDate = "1995-10-30",
        voteAverage = 7.97,
    )

    @Test
    fun `valid dto maps every field`() {
        assertEquals(
            Movie(
                id = 862,
                title = "Toy Story",
                posterUrl = "https://image.tmdb.org/t/p/w342/poster.jpg",
                releaseYear = 1995,
                rating = 7.97,
            ),
            valid.toDomainOrNull(),
        )
    }

    // Required fields: the item is dropped.

    @Test
    fun `missing, zero or negative id is rejected`() {
        assertNull(valid.copy(id = null).toDomainOrNull())
        assertNull(valid.copy(id = 0).toDomainOrNull())
        assertNull(valid.copy(id = -5).toDomainOrNull())
    }

    @Test
    fun `missing or blank title is rejected`() {
        assertNull(valid.copy(title = null).toDomainOrNull())
        assertNull(valid.copy(title = "").toDomainOrNull())
        assertNull(valid.copy(title = "   ").toDomainOrNull())
    }

    @Test
    fun `title is trimmed`() {
        assertEquals("Up", valid.copy(title = "  Up \n").toDomainOrNull()?.title)
    }

    // Optional fields: the item is kept with a safe value.

    @Test
    fun `missing poster becomes null so the UI shows a placeholder`() {
        assertNull(valid.copy(posterPath = null).toDomainOrNull()!!.posterUrl)
        assertNull(valid.copy(posterPath = " ").toDomainOrNull()!!.posterUrl)
    }

    @Test
    fun `release year is read from an ISO date`() {
        assertEquals(2024, "2024-02-29".toReleaseYear())
        assertEquals(2024, " 2024-12-31 ".toReleaseYear())
    }

    @Test
    fun `missing, blank or malformed release date gives no year`() {
        listOf(null, "", "   ", "2024", "2024-13-01", "2024-00-10", "2024-01-32", "30/10/1995", "abcd-ef-gh")
            .forEach { assertNull("'$it'", it.toReleaseYear()) }
    }

    @Test
    fun `implausibly early release year is ignored`() {
        assertNull("0001-01-01".toReleaseYear())
        assertEquals(1895, "1895-12-28".toReleaseYear())
    }

    @Test
    fun `rating is clamped to 0-10 and missing or NaN becomes 0`() {
        assertEquals(10.0, 11.5.toValidRating(), 0.0)
        assertEquals(0.0, (-1.0).toValidRating(), 0.0)
        assertEquals(0.0, (null as Double?).toValidRating(), 0.0)
        assertEquals(0.0, Double.NaN.toValidRating(), 0.0)
        assertEquals(6.4, 6.4.toValidRating(), 0.0)
    }
}
