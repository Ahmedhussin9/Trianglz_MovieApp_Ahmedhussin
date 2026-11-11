package com.trianglz_task_ahmedhussin.feature.details.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** Room column conversions for [MovieDetailsEntity]; registered on the entity only. */
class MovieDetailsConverters {

    /** ISO `yyyy-MM-dd` text: readable in the database inspector and sorts correctly. */
    @TypeConverter
    fun fromDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toDate(text: String?): LocalDate? = text?.let {
        try {
            LocalDate.parse(it)
        } catch (e: DateTimeParseException) {
            Timber.w(e, "Unreadable cached release date '%s'", it)
            null
        }
    }

    @TypeConverter
    fun fromGenres(genres: List<CachedGenre>): String = json.encodeToString(genres)

    /** A damaged row loses only its genres; the next refresh rewrites it. */
    @TypeConverter
    fun toGenres(text: String): List<CachedGenre> = try {
        json.decodeFromString(text)
    } catch (e: IllegalArgumentException) { // includes SerializationException
        Timber.w(e, "Unreadable cached genres")
        emptyList()
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
