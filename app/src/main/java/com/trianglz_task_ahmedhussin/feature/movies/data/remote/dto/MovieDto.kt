package com.trianglz_task_ahmedhussin.feature.movies.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One movie as returned by TMDb list endpoints (`/discover/movie`, `/search/movie`).
 *
 * Every field is nullable with a default: TMDb data is user-contributed and fields are
 * sometimes missing or `null`. Parsing therefore never fails on a single bad item; the
 * mapper decides what is usable (R22). Only fields the app uses are declared.
 */
@Serializable
data class MovieDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
)
