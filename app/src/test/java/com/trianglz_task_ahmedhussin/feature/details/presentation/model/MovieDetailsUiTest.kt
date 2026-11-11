package com.trianglz_task_ahmedhussin.feature.details.presentation.model

import com.trianglz_task_ahmedhussin.testing.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class MovieDetailsUiTest {

    private val details = TestData.movieDetails(
        releaseDate = LocalDate.of(1995, 10, 30),
        rating = 7.97,
        voteCount = 18924,
        originalLanguage = "en",
    )

    @Test
    fun `values are formatted for the user's locale`() {
        val ui = details.toUi(Locale.US)

        assertEquals("Oct 30, 1995", ui.releaseDateText)
        assertEquals("8.0", ui.ratingText)
        assertEquals(0.797f, ui.ratingFraction, 0.0001f)
        assertEquals("18,924", ui.voteCountText)
        assertEquals("English", ui.languageText)
    }

    @Test
    fun `numbers follow the locale's separators`() {
        val ui = details.toUi(Locale.GERMANY)

        assertEquals("8,0", ui.ratingText)
        assertEquals("18.924", ui.voteCountText)
        assertEquals("Englisch", ui.languageText)
    }

    @Test
    fun `language name is shown in Arabic for Arabic users`() {
        assertEquals("الإنجليزية", details.toUi(Locale.forLanguageTag("ar")).languageText)
    }

    @Test
    fun `a movie nobody voted for has no rating text and an empty ring`() {
        val ui = details.copy(rating = 0.0, voteCount = 0).toUi(Locale.US)

        assertNull(ui.ratingText)
        assertEquals(0f, ui.ratingFraction, 0f)
    }

    @Test
    fun `missing or unknown values give null text`() {
        val ui = details.copy(releaseDate = null, originalLanguage = "xx").toUi(Locale.US)

        assertNull(ui.releaseDateText)
        assertNull(ui.languageText)
        assertNull(details.copy(originalLanguage = null).toUi(Locale.US).languageText)
    }

    @Test
    fun `raw values the UI words itself are passed through`() {
        val ui = details.toUi(Locale.US)

        assertEquals(details.runtimeMinutes, ui.runtimeMinutes)
        assertEquals(details.status, ui.status)
        assertEquals(details.genres, ui.genres)
    }
}
