package com.trianglz_task_ahmedhussin.core.network

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/*
 * Validation of raw TMDb values shared by every feature's mappers (R22). TMDb data is
 * user-contributed, so these never throw: unusable values become `null` or are clamped.
 */

private const val MIN_RATING = 0.0
private const val MAX_RATING = 10.0

/** The first films date from the late 1880s; anything earlier is bad data. */
private const val EARLIEST_RELEASE_YEAR = 1880

/** A TMDb vote average limited to 0–10. Missing or NaN becomes 0. */
fun Double?.toValidRating(): Double = when {
    this == null || isNaN() -> MIN_RATING
    else -> coerceIn(MIN_RATING, MAX_RATING)
}

/**
 * Parses a TMDb `yyyy-MM-dd` date. `ISO_LOCAL_DATE` resolves strictly, so impossible dates
 * such as `2023-02-29` are rejected rather than rolled over to the next valid day.
 *
 * @return the date, or `null` when it is missing, malformed, impossible or implausibly early.
 */
fun String?.toReleaseDate(): LocalDate? {
    val text = trimToNull() ?: return null
    val date = try {
        LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (_: DateTimeParseException) {
        return null
    }
    return date.takeIf { it.year >= EARLIEST_RELEASE_YEAR }
}

/** Trimmed text, or `null` when TMDb sent nothing or only whitespace (it often sends `""`). */
fun String?.trimToNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
