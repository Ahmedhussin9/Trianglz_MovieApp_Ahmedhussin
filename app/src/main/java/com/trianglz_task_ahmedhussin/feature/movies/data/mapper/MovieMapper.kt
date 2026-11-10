package com.trianglz_task_ahmedhussin.feature.movies.data.mapper

import com.trianglz_task_ahmedhussin.core.network.TmdbImageUrls
import com.trianglz_task_ahmedhussin.feature.movies.data.remote.dto.MovieDto
import com.trianglz_task_ahmedhussin.feature.movies.domain.model.Movie

private const val MIN_RATING = 0.0
private const val MAX_RATING = 10.0

/** The first films date from the late 1880s; anything earlier is bad data. */
private const val EARLIEST_RELEASE_YEAR = 1880

/** TMDb dates are ISO `yyyy-MM-dd`. Checked with a regex because `java.time` needs API 26. */
private val ISO_DATE = Regex("""^(\d{4})-(0[1-9]|1[0-2])-(0[1-9]|[12]\d|3[01])$""")

/**
 * Validates and converts a TMDb movie to the domain model (R22).
 *
 * @return `null` when the item is unusable (missing or invalid id, blank title), so the
 * caller can drop it instead of showing a broken card. Optional data that is missing or
 * invalid becomes `null` (poster, year) or is clamped (rating) rather than rejected.
 */
fun MovieDto.toDomainOrNull(): Movie? {
    val validId = id?.takeIf { it > 0 } ?: return null
    val validTitle = title?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return Movie(
        id = validId,
        title = validTitle,
        posterUrl = TmdbImageUrls.poster(posterPath),
        releaseYear = releaseDate.toReleaseYear(),
        rating = voteAverage.toValidRating(),
    )
}

internal fun String?.toReleaseYear(): Int? {
    val match = this?.trim()?.let(ISO_DATE::matchEntire) ?: return null
    return match.groupValues[1].toInt().takeIf { it >= EARLIEST_RELEASE_YEAR }
}

internal fun Double?.toValidRating(): Double = when {
    this == null || isNaN() -> MIN_RATING
    else -> coerceIn(MIN_RATING, MAX_RATING)
}
