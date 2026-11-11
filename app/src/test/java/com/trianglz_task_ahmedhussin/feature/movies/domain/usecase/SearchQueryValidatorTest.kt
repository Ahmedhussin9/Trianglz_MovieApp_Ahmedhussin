package com.trianglz_task_ahmedhussin.feature.movies.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchQueryValidatorTest {

    @Test
    fun `blank text is idle`() {
        listOf("", " ", "   ", "\t", "\n ").forEach {
            assertEquals("'$it'", SearchQuery.Idle, SearchQueryValidator.validate(it))
        }
    }

    @Test
    fun `text is trimmed`() {
        assertEquals(SearchQuery.Valid("Coco"), SearchQueryValidator.validate("  Coco \n"))
    }

    @Test
    fun `runs of whitespace become one space`() {
        assertEquals(SearchQuery.Valid("spider man"), SearchQueryValidator.validate("spider   man"))
        assertEquals(SearchQuery.Valid("the dark knight"), SearchQueryValidator.validate("the\tdark \n knight"))
    }

    @Test
    fun `a single character is a valid search`() {
        assertEquals(SearchQuery.Valid("a"), SearchQueryValidator.validate("a"))
    }

    @Test
    fun `Arabic text is kept as typed`() {
        assertEquals(SearchQuery.Valid("فارس الظلام"), SearchQueryValidator.validate(" فارس  الظلام "))
    }

    @Test
    fun `long text is capped at the maximum length`() {
        val result = SearchQueryValidator.validate("x".repeat(SearchQueryValidator.MAX_LENGTH + 50))

        assertEquals(SearchQuery.Valid("x".repeat(SearchQueryValidator.MAX_LENGTH)), result)
    }

    @Test
    fun `a cap that ends on a space does not leave a trailing space`() {
        val raw = "a".repeat(SearchQueryValidator.MAX_LENGTH - 1) + " bcd"

        assertEquals(SearchQuery.Valid("a".repeat(SearchQueryValidator.MAX_LENGTH - 1)), SearchQueryValidator.validate(raw))
    }
}
