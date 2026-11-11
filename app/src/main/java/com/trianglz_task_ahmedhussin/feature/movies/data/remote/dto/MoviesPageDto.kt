package com.trianglz_task_ahmedhussin.feature.movies.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A page of movies from a TMDb list endpoint. */
@Serializable
data class MoviesPageDto(
    @SerialName("page") val page: Int = 1,
    @SerialName("results") val results: List<MovieDto> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 0,
    @SerialName("total_results") val totalResults: Int = 0,
)
