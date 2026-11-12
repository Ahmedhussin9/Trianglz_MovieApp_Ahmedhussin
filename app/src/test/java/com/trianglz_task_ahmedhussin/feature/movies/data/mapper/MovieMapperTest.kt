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
    fun `invalid date gives no year and out-of-range rating is clamped`() {
        val movie = valid.copy(releaseDate = "2023-02-29", voteAverage = 12.0).toDomainOrNull()!!

        assertNull(movie.releaseYear)
        assertEquals(10.0, movie.rating, 0.0)
    }
}
