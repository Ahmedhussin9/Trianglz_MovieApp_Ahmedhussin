package com.trianglz_task_ahmedhussin.feature.movies.domain.usecase

/** What the search bar's text means for the screen (R12, R22). */
sealed interface SearchQuery {

    /** Nothing to search for: the popular list is shown instead of results. */
    data object Idle : SearchQuery

    /** A cleaned-up query worth sending to TMDb. */
    data class Valid(val text: String) : SearchQuery
}

/**
 * Turns raw search-bar text into a [SearchQuery] (input validation, R22).
 *
 * - Leading and trailing spaces are removed and runs of whitespace become one space, so
 *   "spider man", " spider man" and "spider   man" are the same search and do not each cost a
 *   request.
 * - Blank text means [SearchQuery.Idle].
 * - The query is capped at [MAX_LENGTH] characters: no movie title needs more, and it keeps a
 *   pasted paragraph from becoming a huge URL.
 */
object SearchQueryValidator {

    const val MAX_LENGTH = 100

    private val WHITESPACE_RUN = Regex("""\s+""")

    fun validate(raw: String): SearchQuery {
        val normalized = raw.trim().replace(WHITESPACE_RUN, " ")
        if (normalized.isEmpty()) return SearchQuery.Idle
        return SearchQuery.Valid(normalized.take(MAX_LENGTH).trimEnd())
    }
}
