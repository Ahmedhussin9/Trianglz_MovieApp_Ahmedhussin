package com.trianglz_task_ahmedhussin.feature.movies.domain.model

/**
 * A movie as shown in the list and search results (R2).
 *
 * Values are already validated by the data layer: [title] is never blank, [rating] is
 * within 0–10, and fields TMDb may omit are nullable instead of placeholders.
 *
 * @property posterUrl full image URL, or `null` when TMDb has no poster.
 * @property releaseYear year of the release date, or `null` when it is unknown or invalid.
 * @property rating TMDb average vote, 0–10.
 */
data class Movie(
    val id: Int,
    val title: String,
    val posterUrl: String?,
    val releaseYear: Int?,
    val rating: Double,
)
