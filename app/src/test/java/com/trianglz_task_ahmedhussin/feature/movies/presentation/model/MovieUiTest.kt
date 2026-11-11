package com.trianglz_task_ahmedhussin.feature.movies.presentation.model

import com.trianglz_task_ahmedhussin.testing.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class MovieUiTest {

    @Test
    fun `maps every field ready for display`() {
        val ui = TestData.movie(id = 7, title = "Coco", posterUrl = "https://x/p.jpg", releaseYear = 2017, rating = 8.207)
            .toUi(Locale.US)

        assertEquals(MovieUi(id = 7, title = "Coco", posterUrl = "https://x/p.jpg", yearText = "2017", ratingText = "8.2"), ui)
    }

    @Test
    fun `rating is rounded to one decimal`() {
        assertEquals("8.0", TestData.movie(rating = 7.97).toUi(Locale.US).ratingText)
        assertEquals("6.4", TestData.movie(rating = 6.44).toUi(Locale.US).ratingText)
    }

    @Test
    fun `rating follows the user's number format`() {
        assertEquals("7,5", TestData.movie(rating = 7.5).toUi(Locale.GERMANY).ratingText)
    }

    @Test
    fun `unrated movie has no rating badge instead of a misleading zero`() {
        assertNull(TestData.movie(rating = 0.0).toUi(Locale.US).ratingText)
    }

    @Test
    fun `unknown year and poster stay absent`() {
        val ui = TestData.movie(releaseYear = null, posterUrl = null).toUi(Locale.US)

        assertNull(ui.yearText)
        assertNull(ui.posterUrl)
    }
}
