package com.trianglz_task_ahmedhussin.testing

import com.trianglz_task_ahmedhussin.feature.movies.data.remote.MoviesApi
import com.trianglz_task_ahmedhussin.feature.movies.data.remote.dto.MovieDto
import com.trianglz_task_ahmedhussin.feature.movies.data.remote.dto.MoviesPageDto

/**
 * In-memory [MoviesApi]: each page is either a response or a failure to throw.
 * Records requested pages so tests can assert that no unnecessary call was made.
 */
class FakeMoviesApi : MoviesApi {

    private val responses = mutableMapOf<Int, MoviesPageDto>()
    private val failures = mutableMapOf<Int, Exception>()
    val requestedPages = mutableListOf<Int>()

    fun respond(page: Int, movieIds: List<Int>, totalPages: Int) {
        failures.remove(page)
        responses[page] = MoviesPageDto(
            page = page,
            results = movieIds.map { MovieDto(id = it, title = "Movie $it", releaseDate = "2024-01-01", voteAverage = 7.0) },
            totalPages = totalPages,
            totalResults = totalPages * 20,
        )
    }

    fun fail(page: Int, error: Exception) {
        responses.remove(page)
        failures[page] = error
    }

    override suspend fun discoverMovies(page: Int, sortBy: String): MoviesPageDto {
        requestedPages += page
        failures[page]?.let { throw it }
        return responses[page] ?: error("No fake response for page $page")
    }
}
