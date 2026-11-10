package com.trianglz_task_ahmedhussin.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Authenticates every request with the TMDb v4 Read Access Token.
 *
 * A header is used instead of the v3 `api_key` query parameter so the secret never
 * appears in URLs, logs or cache keys. A blank token is still sent: TMDb answers 401,
 * which the error layer maps to `DomainError.Unauthorized` with a clear message.
 */
class AuthInterceptor(private val accessToken: String) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header(HEADER_AUTHORIZATION, "Bearer $accessToken")
            .header(HEADER_ACCEPT, "application/json")
            .build()
        return chain.proceed(request)
    }

    companion object {
        const val HEADER_AUTHORIZATION = "Authorization"
        private const val HEADER_ACCEPT = "Accept"
    }
}
