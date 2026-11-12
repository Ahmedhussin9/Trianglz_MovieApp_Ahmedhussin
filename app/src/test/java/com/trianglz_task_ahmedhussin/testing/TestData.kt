package com.trianglz_task_ahmedhussin.testing

import com.trianglz_task_ahmedhussin.feature.details.domain.model.Genre
import com.trianglz_task_ahmedhussin.feature.details.domain.model.MovieDetails
import com.trianglz_task_ahmedhussin.feature.details.domain.model.ReleaseStatus
import com.trianglz_task_ahmedhussin.feature.movies.domain.model.Movie
import java.time.LocalDate

/** Builders for domain objects with sensible defaults; tests override only what matters. */
object TestData {

    fun movie(
        id: Int = 1,
        title: String = "Movie $id",
        posterUrl: String? = "https://image.tmdb.org/t/p/w342/poster$id.jpg",
        releaseYear: Int? = 2024,
        rating: Double = 7.5,
    ) = Movie(id = id, title = title, posterUrl = posterUrl, releaseYear = releaseYear, rating = rating)

    fun movies(count: Int, startId: Int = 1): List<Movie> = (startId until startId + count).map { movie(id = it) }

    fun genre(id: Int = 28, name: String = "Action") = Genre(id = id, name = name)

    fun movieDetails(
        id: Int = 1,
        title: String = "Movie $id",
        tagline: String? = "A tagline",
        overview: String? = "A synopsis of movie $id.",
        posterUrl: String? = "https://image.tmdb.org/t/p/w342/poster$id.jpg",
        backdropUrl: String? = "https://image.tmdb.org/t/p/w780/backdrop$id.jpg",
        releaseDate: LocalDate? = LocalDate.of(2024, 3, 12),
        runtimeMinutes: Int? = 120,
        rating: Double = 7.5,
        voteCount: Int = 1_000,
        genres: List<Genre> = listOf(genre()),
        originalLanguage: String? = "en",
        status: ReleaseStatus = ReleaseStatus.Released,
    ) = MovieDetails(
        id = id,
        title = title,
        tagline = tagline,
        overview = overview,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        releaseDate = releaseDate,
        runtimeMinutes = runtimeMinutes,
        rating = rating,
        voteCount = voteCount,
        genres = genres,
        originalLanguage = originalLanguage,
        status = status,
    )
}
