package com.trianglz_task_ahmedhussin.core.network

import com.trianglz_task_ahmedhussin.core.common.locale.LocaleProvider
import com.trianglz_task_ahmedhussin.core.network.interceptor.AuthInterceptor
import com.trianglz_task_ahmedhussin.core.network.interceptor.ContentFilterInterceptor
import com.trianglz_task_ahmedhussin.core.network.interceptor.LanguageInterceptor
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds the TMDb networking stack.
 *
 * Kept outside the Hilt module so tests build exactly the client the app uses
 * and point it at a MockWebServer.
 */
object TmdbHttpClientFactory {

    private const val CONNECT_TIMEOUT_SECONDS = 15L
    private const val READ_TIMEOUT_SECONDS = 20L

    /**
     * Lenient JSON parsing: unknown fields are ignored so TMDb can add fields without
     * breaking the app, and `null` for a non-null field with a default falls back to the
     * default instead of throwing. Real validation happens in the data-layer mappers.
     */
    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    /**
     * @param httpLogger receives HTTP logs when non-null (debug builds only). The
     * `Authorization` header is always redacted so the token never reaches Logcat.
     */
    fun okHttpClient(
        accessToken: String,
        localeProvider: LocaleProvider,
        httpLogger: HttpLoggingInterceptor.Logger? = null,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(AuthInterceptor(accessToken))
        .addInterceptor(ContentFilterInterceptor())
        .addInterceptor(LanguageInterceptor(localeProvider))
        .apply {
            if (httpLogger != null) {
                addInterceptor(
                    HttpLoggingInterceptor(httpLogger).apply {
                        level = HttpLoggingInterceptor.Level.BODY
                        redactHeader(AuthInterceptor.HEADER_AUTHORIZATION)
                    },
                )
            }
        }
        .build()

    fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
