package com.trianglz_task_ahmedhussin.testing

import com.trianglz_task_ahmedhussin.feature.movies.domain.model.Movie

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
}
