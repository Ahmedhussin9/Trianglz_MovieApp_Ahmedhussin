package com.trianglz_task_ahmedhussin.feature.details.presentation.model

import com.trianglz_task_ahmedhussin.feature.details.domain.model.Genre
import com.trianglz_task_ahmedhussin.feature.details.domain.model.MovieDetails
import com.trianglz_task_ahmedhussin.feature.details.domain.model.ReleaseStatus
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Movie details ready to display. Locale-dependent formatting (dates, numbers, language names)
 * happens here, so composables only place text.
 *
 * Two values stay raw because their wording lives in string resources, which only the UI reads:
 * [runtimeMinutes] ("1 h 21 min") and [status] (a translated label).
 *
 * @property releaseDateText for example "Oct 30, 1995" in English, formatted in the user's locale.
 * @property ratingFraction rating as 0–1, for the rating ring.
 * @property ratingText rating with one decimal ("8.0"), or `null` when nobody has voted yet.
 * @property voteCount raw count, which the UI needs to pick the plural form ("1 vote" / "5 votes").
 * @property voteCountText vote count with grouping ("18,924").
 * @property languageText original language in the user's language ("English"), or `null`.
 */
data class MovieDetailsUi(
    val id: Int,
    val title: String,
    val tagline: String?,
    val overview: String?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val releaseDateText: String?,
    val runtimeMinutes: Int?,
    val ratingFraction: Float,
    val ratingText: String?,
    val voteCount: Int,
    val voteCountText: String,
    val genres: List<Genre>,
    val languageText: String?,
    val status: ReleaseStatus,
)

fun MovieDetails.toUi(locale: Locale): MovieDetailsUi {
    val hasVotes = voteCount > 0 && rating > 0.0
    return MovieDetailsUi(
        id = id,
        title = title,
        tagline = tagline,
        overview = overview,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        releaseDateText = releaseDate?.format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(locale)
                // Without this java.time keeps Latin digits even in Arabic, unlike the numbers around it.
                .withDecimalStyle(DecimalStyle.of(locale)),
        ),
        runtimeMinutes = runtimeMinutes,
        ratingFraction = if (hasVotes) (rating / MAX_RATING).toFloat() else 0f,
        // A "0.0" ring would read as "terrible" rather than "unrated", as on the list cards.
        ratingText = if (hasVotes) String.format(locale, "%.1f", rating) else null,
        voteCount = voteCount,
        voteCountText = NumberFormat.getIntegerInstance(locale).format(voteCount),
        genres = genres,
        languageText = originalLanguage?.toLanguageName(locale),
        status = status,
    )
}

private const val MAX_RATING = 10.0

/**
 * "en" → "English" (or "الإنجليزية" in Arabic). Java returns the code itself for codes it
 * does not know, and a bare "xx" means nothing to users, so those become `null`.
 */
private fun String.toLanguageName(locale: Locale): String? {
    val name = Locale.forLanguageTag(this).getDisplayLanguage(locale)
    if (name.isBlank() || name.equals(this, ignoreCase = true)) return null
    return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}
