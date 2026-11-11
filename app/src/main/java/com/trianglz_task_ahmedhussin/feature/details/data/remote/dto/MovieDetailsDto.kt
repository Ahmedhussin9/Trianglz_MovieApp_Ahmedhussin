package com.trianglz_task_ahmedhussin.feature.details.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A movie as returned by `/movie/{movie_id}`.
 *
 * Every field is nullable with a default, as in `MovieDto`: parsing never fails on missing
 * data, and the mapper decides what is usable (R22). Only fields the app shows are declared.
 */
@Serializable
data class MovieDetailsDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("tagline") val tagline: String? = null,
    @SerialName("overview") val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("runtime") val runtime: Int? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    @SerialName("genres") val genres: List<GenreDto>? = null,
    @SerialName("original_language") val originalLanguage: String? = null,
    @SerialName("status") val status: String? = null,
)

/** A genre as returned inside `/movie/{movie_id}`; validated by the mapper. */
@Serializable
data class GenreDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("name") val name: String? = null,
)
