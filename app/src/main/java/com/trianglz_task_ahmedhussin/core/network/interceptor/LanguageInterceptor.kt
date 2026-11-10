package com.trianglz_task_ahmedhussin.core.network.interceptor

import com.trianglz_task_ahmedhussin.core.common.locale.LocaleProvider
import okhttp3.Interceptor
import okhttp3.Response
import java.util.Locale

/**
 * Requests TMDb content (titles, synopses, genres) in the app's current language (Q14).
 *
 * The app supports English and Arabic; any other locale falls back to English.
 */
class LanguageInterceptor(private val localeProvider: LocaleProvider) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val url = original.url.newBuilder()
            .setQueryParameter(QUERY_LANGUAGE, localeProvider.current().toTmdbLanguage())
            .build()
        return chain.proceed(original.newBuilder().url(url).build())
    }

    companion object {
        const val QUERY_LANGUAGE = "language"
        const val ENGLISH = "en-US"
        const val ARABIC = "ar-SA"

        /** Maps a device/app locale to a TMDb `language` value (ISO 639-1 + ISO 3166-1). */
        fun Locale.toTmdbLanguage(): String = when (language) {
            "ar" -> ARABIC
            else -> ENGLISH
        }
    }
}
