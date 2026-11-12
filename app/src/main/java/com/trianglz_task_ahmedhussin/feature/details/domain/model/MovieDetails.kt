package com.trianglz_task_ahmedhussin.feature.details.domain.model

import java.time.LocalDate

/**
 * Everything the details screen shows about one movie (R6).
 *
 * Values are already validated by the data layer: [title] is never blank, [rating] is
 * within 0–10, and fields TMDb may omit or send empty are `null` instead of placeholders.
 *
 * @property overview the synopsis, or `null` when TMDb has none.
 * @property releaseDate release date, or `null` when it is unknown or invalid. Kept as a date
 * (not preformatted text) so the presentation layer formats it in the user's locale.
 * @property runtimeMinutes length in minutes, or `null` when TMDb reports 0 or nothing.
 * @property rating TMDb average vote, 0–10.
 * @property originalLanguage ISO 639-1 code such as "en"; presentation turns it into a
 * localized language name.
 */
data class MovieDetails(
    val id: Int,
    val title: String,
    val tagline: String?,
    val overview: String?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val releaseDate: LocalDate?,
    val runtimeMinutes: Int?,
    val rating: Double,
    val voteCount: Int,
    val genres: List<Genre>,
    val originalLanguage: String?,
    val status: ReleaseStatus,
)

/** A TMDb genre. The [id] gives each genre chip a stable key; [name] is localized by TMDb. */
data class Genre(val id: Int, val name: String)

/**
 * Production status of a movie. TMDb sends it as English text in every language, so it is
 * kept as an enum and translated in the presentation layer.
 */
enum class ReleaseStatus {
    Rumored,
    Planned,
    InProduction,
    PostProduction,
    Released,
    Canceled,

    /** TMDb sent no status, or one this app does not know yet. */
    Unknown,
}
