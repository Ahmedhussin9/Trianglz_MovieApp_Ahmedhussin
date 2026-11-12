package com.trianglz_task_ahmedhussin.feature.movies.data.remote

import com.trianglz_task_ahmedhussin.feature.movies.data.remote.dto.MoviesPageDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * TMDb movie list endpoints.
 *
 * Every request also gets `include_adult=false`, `certification_country=US`,
 * `certification.lte=PG-13`, `language` and the Bearer token from the shared
 * interceptors (`core/network`), so they are not repeated here.
 */
interface MoviesApi {

    /**
     * `/discover/movie` is used instead of `/movie/popular` because it is the endpoint where
     * TMDb applies the required certification filter (R24–R26).
     */
    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("page") page: Int,
        @Query("sort_by") sortBy: String,
    ): MoviesPageDto

    /**
     * Title search (R12). It returns the same page shape as `/discover/movie`. The three filter
     * parameters are sent here too (R24–R26), but TMDb applies only `include_adult` on this
     * endpoint and ignores the certification ones (requirements Q2).
     */
    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("page") page: Int,
    ): MoviesPageDto
}
