package com.trianglz_task_ahmedhussin.feature.details.data.mapper

import com.trianglz_task_ahmedhussin.feature.details.data.local.CachedGenre
import com.trianglz_task_ahmedhussin.feature.details.data.local.MovieDetailsEntity
import com.trianglz_task_ahmedhussin.feature.details.domain.model.Genre
import com.trianglz_task_ahmedhussin.feature.details.domain.model.MovieDetails
import com.trianglz_task_ahmedhussin.feature.details.domain.model.ReleaseStatus

/** Details are validated before caching (`toDomainOrNull`), so these mappings are 1:1. */
fun MovieDetails.toEntity() = MovieDetailsEntity(
    id = id,
    title = title,
    tagline = tagline,
    overview = overview,
    posterUrl = posterUrl,
    backdropUrl = backdropUrl,
    releaseDate = releaseDate,
    runtimeMinutes = runtimeMinutes,
    rating = rating,
    voteCount = voteCount,
    genres = genres.map { CachedGenre(id = it.id, name = it.name) },
    originalLanguage = originalLanguage,
    status = status.name,
)

/** An unknown stored status (for example after renaming an enum entry) becomes `Unknown`. */
fun MovieDetailsEntity.toDomain() = MovieDetails(
    id = id,
    title = title,
    tagline = tagline,
    overview = overview,
    posterUrl = posterUrl,
    backdropUrl = backdropUrl,
    releaseDate = releaseDate,
    runtimeMinutes = runtimeMinutes,
    rating = rating,
    voteCount = voteCount,
    genres = genres.map { Genre(id = it.id, name = it.name) },
    originalLanguage = originalLanguage,
    status = ReleaseStatus.entries.firstOrNull { it.name == status } ?: ReleaseStatus.Unknown,
)
