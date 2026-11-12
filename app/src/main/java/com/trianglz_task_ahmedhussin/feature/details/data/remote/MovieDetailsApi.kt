package com.trianglz_task_ahmedhussin.feature.details.data.remote

import com.trianglz_task_ahmedhussin.feature.details.data.remote.dto.MovieDetailsDto
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * TMDb movie details endpoint. The Bearer token and `language` come from the shared
 * interceptors (`core/network`). The content-filter parameters are sent too but have no
 * effect here: the list only links to movies that already passed the filter.
 */
interface MovieDetailsApi {

    @GET("movie/{movie_id}")
    suspend fun movieDetails(@Path("movie_id") movieId: Int): MovieDetailsDto
}
