package com.trianglz_task_ahmedhussin.core.di

import android.util.Log
import com.trianglz_task_ahmedhussin.BuildConfig
import com.trianglz_task_ahmedhussin.core.common.locale.LocaleProvider
import com.trianglz_task_ahmedhussin.core.network.TmdbAccessToken
import com.trianglz_task_ahmedhussin.core.network.TmdbConfig
import com.trianglz_task_ahmedhussin.core.network.TmdbHttpClientFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.Locale
import javax.inject.Singleton

/** Provides the TMDb networking stack. Feature modules create their API services from [Retrofit]. */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val HTTP_LOG_TAG = "TmdbHttp"

    @Provides
    @TmdbAccessToken
    fun provideTmdbAccessToken(): String = BuildConfig.TMDB_ACCESS_TOKEN

    /** `Locale.getDefault()` follows per-app language changes, so it is read per request. */
    @Provides
    fun provideLocaleProvider(): LocaleProvider = LocaleProvider { Locale.getDefault() }

    @Provides
    @Singleton
    fun provideJson(): Json = TmdbHttpClientFactory.json

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @TmdbAccessToken accessToken: String,
        localeProvider: LocaleProvider,
    ): OkHttpClient = TmdbHttpClientFactory.okHttpClient(
        accessToken = accessToken,
        localeProvider = localeProvider,
        httpLogger = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Logger { Log.d(HTTP_LOG_TAG, it) } else null,
    )

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        TmdbHttpClientFactory.retrofit(TmdbConfig.API_BASE_URL, client)
}
