package com.trianglz_task_ahmedhussin.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class TmdbValuesTest {

    @Test
    fun `release date is parsed from an ISO date`() {
        assertEquals(LocalDate.of(2024, 2, 29), "2024-02-29".toReleaseDate())
        assertEquals(LocalDate.of(2024, 12, 31), " 2024-12-31 ".toReleaseDate())
    }

    @Test
    fun `missing, blank, malformed or impossible dates give null`() {
        listOf(null, "", "   ", "2024", "2024-13-01", "2024-00-10", "2024-01-32", "2023-02-29", "2024-04-31", "30/10/1995", "abcd-ef-gh")
            .forEach { assertNull("'$it'", it.toReleaseDate()) }
    }

    @Test
    fun `implausibly early release date is ignored`() {
        assertNull("0001-01-01".toReleaseDate())
        assertEquals(LocalDate.of(1895, 12, 28), "1895-12-28".toReleaseDate())
    }

    @Test
    fun `rating is clamped to 0-10 and missing or NaN becomes 0`() {
        assertEquals(10.0, 11.5.toValidRating(), 0.0)
        assertEquals(0.0, (-1.0).toValidRating(), 0.0)
        assertEquals(0.0, (null as Double?).toValidRating(), 0.0)
        assertEquals(0.0, Double.NaN.toValidRating(), 0.0)
        assertEquals(6.4, 6.4.toValidRating(), 0.0)
    }

    @Test
    fun `text is trimmed and blank becomes null`() {
        assertEquals("Hello", "  Hello \n".trimToNull())
        assertNull("".trimToNull())
        assertNull("   ".trimToNull())
        assertNull((null as String?).trimToNull())
    }
}
