package com.trianglz_task_ahmedhussin.feature.details.data.mapper

import com.trianglz_task_ahmedhussin.feature.details.data.remote.dto.GenreDto
import com.trianglz_task_ahmedhussin.feature.details.data.remote.dto.MovieDetailsDto
import com.trianglz_task_ahmedhussin.feature.details.domain.model.Genre
import com.trianglz_task_ahmedhussin.feature.details.domain.model.ReleaseStatus
import com.trianglz_task_ahmedhussin.testing.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class MovieDetailsMapperTest {

    private val valid = MovieDetailsDto(
        id = 862,
        title = "Toy Story",
        tagline = "To infinity and beyond!",
        overview = "Andy's toys come to life.",
        posterPath = "/poster.jpg",
        backdropPath = "/backdrop.jpg",
        releaseDate = "1995-10-30",
        runtime = 81,
        voteAverage = 7.97,
        voteCount = 18924,
        genres = listOf(GenreDto(16, "Animation"), GenreDto(35, "Comedy")),
        originalLanguage = "en",
        status = "Released",
    )

    private fun validDetails() = valid.toDomainOrNull()!!

    @Test
    fun `valid dto maps every field`() {
        assertEquals(
            TestData.movieDetails(
                id = 862,
                title = "Toy Story",
                tagline = "To infinity and beyond!",
                overview = "Andy's toys come to life.",
                posterUrl = "https://image.tmdb.org/t/p/w500/poster.jpg",
                backdropUrl = "https://image.tmdb.org/t/p/w1280/backdrop.jpg",
                releaseDate = LocalDate.of(1995, 10, 30),
                runtimeMinutes = 81,
                rating = 7.97,
                voteCount = 18924,
                genres = listOf(Genre(16, "Animation"), Genre(35, "Comedy")),
                originalLanguage = "en",
                status = ReleaseStatus.Released,
            ),
            validDetails(),
        )
    }

    // Required fields: the movie is rejected.

    @Test
    fun `missing, zero or negative id is rejected`() {
        listOf(null, 0, -5).forEach { assertNull("id=$it", valid.copy(id = it).toDomainOrNull()) }
    }

    @Test
    fun `missing or blank title is rejected`() {
        listOf(null, "", "  ").forEach { assertNull("title='$it'", valid.copy(title = it).toDomainOrNull()) }
    }

    // Optional fields: the movie is kept with a safe value.

    @Test
    fun `text fields are trimmed and empty text becomes null`() {
        val details = valid.copy(title = " Up ", tagline = "", overview = "   ", originalLanguage = " ").toDomainOrNull()!!

        assertEquals("Up", details.title)
        assertNull(details.tagline)
        assertNull(details.overview)
        assertNull(details.originalLanguage)
    }

    @Test
    fun `missing images become null so the UI shows placeholders`() {
        val details = valid.copy(posterPath = null, backdropPath = " ").toDomainOrNull()!!

        assertNull(details.posterUrl)
        assertNull(details.backdropUrl)
    }

    @Test
    fun `invalid release date becomes null`() {
        assertNull(valid.copy(releaseDate = "").toDomainOrNull()!!.releaseDate)
        assertNull(valid.copy(releaseDate = "2023-02-29").toDomainOrNull()!!.releaseDate)
    }

    @Test
    fun `zero or missing runtime becomes null instead of showing 0 min`() {
        assertNull(valid.copy(runtime = 0).toDomainOrNull()!!.runtimeMinutes)
        assertNull(valid.copy(runtime = null).toDomainOrNull()!!.runtimeMinutes)
    }

    @Test
    fun `rating is clamped and vote count is never negative`() {
        val details = valid.copy(voteAverage = 11.0, voteCount = -3).toDomainOrNull()!!

        assertEquals(10.0, details.rating, 0.0)
        assertEquals(0, details.voteCount)
        assertEquals(0, valid.copy(voteCount = null).toDomainOrNull()!!.voteCount)
    }

    @Test
    fun `invalid and duplicate genres are dropped, valid ones keep their order`() {
        val genres = listOf(
            GenreDto(16, "Animation"),
            GenreDto(null, "No id"),
            GenreDto(12, " "),
            GenreDto(16, "Animation again"),
            GenreDto(35, " Comedy "),
        )

        assertEquals(listOf(Genre(16, "Animation"), Genre(35, "Comedy")), valid.copy(genres = genres).toDomainOrNull()!!.genres)
        assertEquals(emptyList<Genre>(), valid.copy(genres = null).toDomainOrNull()!!.genres)
    }

    @Test
    fun `language code is normalized to lowercase`() {
        assertEquals("en", valid.copy(originalLanguage = "EN").toDomainOrNull()!!.originalLanguage)
    }

    @Test
    fun `every documented status is recognized and anything else is Unknown`() {
        mapOf(
            "Rumored" to ReleaseStatus.Rumored,
            "Planned" to ReleaseStatus.Planned,
            "In Production" to ReleaseStatus.InProduction,
            "Post Production" to ReleaseStatus.PostProduction,
            "Released" to ReleaseStatus.Released,
            "Canceled" to ReleaseStatus.Canceled,
            "Something new" to ReleaseStatus.Unknown,
            "" to ReleaseStatus.Unknown,
        ).forEach { (text, expected) -> assertEquals(text, expected, text.toReleaseStatus()) }
        assertEquals(ReleaseStatus.Unknown, (null as String?).toReleaseStatus())
    }

    @Test
    fun `entity round trip keeps every field`() {
        val details = validDetails()

        assertEquals(details, details.toEntity().toDomain())
    }

    @Test
    fun `unknown cached status name falls back to Unknown`() {
        val entity = validDetails().toEntity().copy(status = "Renamed")

        assertEquals(ReleaseStatus.Unknown, entity.toDomain().status)
    }
}
