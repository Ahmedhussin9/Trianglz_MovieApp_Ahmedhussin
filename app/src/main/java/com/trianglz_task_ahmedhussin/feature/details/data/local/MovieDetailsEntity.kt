package com.trianglz_task_ahmedhussin.feature.details.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import kotlinx.serialization.Serializable
import java.time.LocalDate

/**
 * Cached details of one movie (R7, R38, requirements Q5). Stores already-validated data
 * (see `MovieDetailsMapper`), so reading it back needs no validation.
 *
 * @property genres stored as a JSON column: they are only ever read together with their movie,
 * so a separate table and join would add schema without adding any query.
 * @property status the `ReleaseStatus` name, stored as text so renaming an enum entry can
 * never make an old row unreadable (the mapper falls back to `Unknown`).
 */
@Entity(tableName = "movie_details")
@TypeConverters(MovieDetailsConverters::class)
data class MovieDetailsEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val tagline: String?,
    val overview: String?,
    @ColumnInfo(name = "poster_url") val posterUrl: String?,
    @ColumnInfo(name = "backdrop_url") val backdropUrl: String?,
    @ColumnInfo(name = "release_date") val releaseDate: LocalDate?,
    @ColumnInfo(name = "runtime_minutes") val runtimeMinutes: Int?,
    val rating: Double,
    @ColumnInfo(name = "vote_count") val voteCount: Int,
    val genres: List<CachedGenre>,
    @ColumnInfo(name = "original_language") val originalLanguage: String?,
    val status: String,
)

/** A genre as stored inside [MovieDetailsEntity]'s JSON column. */
@Serializable
data class CachedGenre(val id: Int, val name: String)
