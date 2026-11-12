package com.trianglz_task_ahmedhussin.feature.movies.data.mapper

import com.trianglz_task_ahmedhussin.core.network.TmdbImageUrls
import com.trianglz_task_ahmedhussin.core.network.toReleaseDate
import com.trianglz_task_ahmedhussin.core.network.toValidRating
import com.trianglz_task_ahmedhussin.core.network.trimToNull
import com.trianglz_task_ahmedhussin.feature.movies.data.remote.dto.MovieDto
import com.trianglz_task_ahmedhussin.feature.movies.domain.model.Movie

/**
 * Validates and converts a TMDb movie to the domain model (R22).
 *
 * @return `null` when the item is unusable (missing or invalid id, blank title), so the
 * caller can drop it instead of showing a broken card. Optional data that is missing or
 * invalid becomes `null` (poster, year) or is clamped (rating) rather than rejected.
 */
fun MovieDto.toDomainOrNull(): Movie? {
    val validId = id?.takeIf { it > 0 } ?: return null
    val validTitle = title.trimToNull() ?: return null
    return Movie(
        id = validId,
        title = validTitle,
        posterUrl = TmdbImageUrls.poster(posterPath),
        releaseYear = releaseDate.toReleaseDate()?.year,
        rating = voteAverage.toValidRating(),
    )
}
