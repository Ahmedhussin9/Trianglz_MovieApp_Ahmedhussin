package com.trianglz_task_ahmedhussin.feature.movies.data.remote

import com.trianglz_task_ahmedhussin.feature.movies.domain.model.Movie

/**
 * A validated page of movies, ready to be cached or shown.
 *
 * @property movies only valid, de-duplicated movies, in TMDb's order.
 * @property isLastPage `true` when no further page can be requested.
 */
data class MoviesPage(
    val page: Int,
    val movies: List<Movie>,
    val isLastPage: Boolean,
)
