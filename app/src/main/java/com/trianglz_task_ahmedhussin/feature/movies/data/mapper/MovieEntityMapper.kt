package com.trianglz_task_ahmedhussin.feature.movies.data.mapper

import com.trianglz_task_ahmedhussin.feature.movies.data.local.MovieEntity
import com.trianglz_task_ahmedhussin.feature.movies.domain.model.Movie

/** Movies are validated before caching (`toDomainOrNull`), so these mappings are 1:1. */
fun Movie.toEntity(position: Int) = MovieEntity(
    id = id,
    title = title,
    posterUrl = posterUrl,
    releaseYear = releaseYear,
    rating = rating,
    position = position,
)

fun MovieEntity.toDomain() = Movie(
    id = id,
    title = title,
    posterUrl = posterUrl,
    releaseYear = releaseYear,
    rating = rating,
)
