package com.trianglz_task_ahmedhussin.feature.details.data.mapper

import com.trianglz_task_ahmedhussin.core.network.TmdbImageUrls
import com.trianglz_task_ahmedhussin.core.network.toReleaseDate
import com.trianglz_task_ahmedhussin.core.network.toValidRating
import com.trianglz_task_ahmedhussin.core.network.trimToNull
import com.trianglz_task_ahmedhussin.feature.details.data.remote.dto.GenreDto
import com.trianglz_task_ahmedhussin.feature.details.data.remote.dto.MovieDetailsDto
import com.trianglz_task_ahmedhussin.feature.details.domain.model.Genre
import com.trianglz_task_ahmedhussin.feature.details.domain.model.MovieDetails
import com.trianglz_task_ahmedhussin.feature.details.domain.model.ReleaseStatus

/**
 * Validates and converts TMDb movie details to the domain model (R22).
 *
 * @return `null` when the movie is unusable (missing or invalid id, blank title). Optional
 * data that is missing, empty or invalid becomes `null`, is clamped (rating, vote count) or
 * is dropped (individual genres) instead of rejecting the whole movie.
 */
fun MovieDetailsDto.toDomainOrNull(): MovieDetails? {
    val validId = id?.takeIf { it > 0 } ?: return null
    val validTitle = title.trimToNull() ?: return null
    return MovieDetails(
        id = validId,
        title = validTitle,
        tagline = tagline.trimToNull(),
        overview = overview.trimToNull(),
        posterUrl = TmdbImageUrls.poster(posterPath, TmdbImageUrls.PosterSize.LARGE),
        backdropUrl = TmdbImageUrls.backdrop(backdropPath, TmdbImageUrls.BackdropSize.LARGE),
        releaseDate = releaseDate.toReleaseDate(),
        runtimeMinutes = runtime?.takeIf { it > 0 },
        rating = voteAverage.toValidRating(),
        voteCount = voteCount?.coerceAtLeast(0) ?: 0,
        genres = genres.orEmpty().mapNotNull { it.toDomainOrNull() }.distinctBy { it.id },
        originalLanguage = originalLanguage.trimToNull()?.lowercase(),
        status = status.toReleaseStatus(),
    )
}

private fun GenreDto.toDomainOrNull(): Genre? {
    val validId = id?.takeIf { it > 0 } ?: return null
    val validName = name.trimToNull() ?: return null
    return Genre(id = validId, name = validName)
}

/** TMDb's documented status values; anything else becomes [ReleaseStatus.Unknown]. */
internal fun String?.toReleaseStatus(): ReleaseStatus = when (trimToNull()) {
    "Rumored" -> ReleaseStatus.Rumored
    "Planned" -> ReleaseStatus.Planned
    "In Production" -> ReleaseStatus.InProduction
    "Post Production" -> ReleaseStatus.PostProduction
    "Released" -> ReleaseStatus.Released
    "Canceled" -> ReleaseStatus.Canceled
    else -> ReleaseStatus.Unknown
}
