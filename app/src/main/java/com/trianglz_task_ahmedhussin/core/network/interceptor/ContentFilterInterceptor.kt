package com.trianglz_task_ahmedhussin.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds the content-filter parameters the task requires on **every** TMDb request (R24–R26).
 *
 * Applying them in one interceptor means no API method can forget them, and
 * `setQueryParameter` overwrites any conflicting value a caller passes.
 *
 * TMDb only honours the certification filters on `/discover/movie`; other endpoints
 * (search, details) ignore them but still receive them, as the task requires.
 */
class ContentFilterInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val url = original.url.newBuilder().apply {
            REQUIRED_QUERY_PARAMETERS.forEach { (name, value) -> setQueryParameter(name, value) }
        }.build()
        return chain.proceed(original.newBuilder().url(url).build())
    }

    companion object {
        val REQUIRED_QUERY_PARAMETERS: Map<String, String> = mapOf(
            "include_adult" to "false",
            "certification_country" to "US",
            "certification.lte" to "PG-13",
        )
    }
}
