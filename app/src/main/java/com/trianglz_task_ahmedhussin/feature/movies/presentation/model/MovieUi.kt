package com.trianglz_task_ahmedhussin.feature.movies.presentation.model

import com.trianglz_task_ahmedhussin.feature.movies.domain.model.Movie
import java.util.Locale

/**
 * A movie ready to display: all formatting is done here, so composables only place text.
 *
 * @property yearText release year, or `null` when unknown (the card then hides it).
 * @property ratingText rating with one decimal in the user's number format, or `null` when
 * the movie has no votes yet (a "0.0" badge would read as "terrible" rather than "unrated").
 */
data class MovieUi(
    val id: Int,
    val title: String,
    val posterUrl: String?,
    val yearText: String?,
    val ratingText: String?,
)

fun Movie.toUi(locale: Locale = Locale.getDefault()) = MovieUi(
    id = id,
    title = title,
    posterUrl = posterUrl,
    yearText = releaseYear?.toString(),
    ratingText = rating.takeIf { it > 0.0 }?.let { String.format(locale, "%.1f", it) },
)
