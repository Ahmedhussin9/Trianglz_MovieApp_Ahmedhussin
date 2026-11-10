package com.trianglz_task_ahmedhussin.feature.movies.data.mapper

import com.trianglz_task_ahmedhussin.testing.TestData
import org.junit.Assert.assertEquals
import org.junit.Test

class MovieEntityMapperTest {

    @Test
    fun `movie survives a round trip through the cache entity`() {
        val movie = TestData.movie(id = 42, posterUrl = null, releaseYear = null, rating = 0.0)

        val entity = movie.toEntity(position = 7)

        assertEquals(7, entity.position)
        assertEquals(movie, entity.toDomain())
    }
}
