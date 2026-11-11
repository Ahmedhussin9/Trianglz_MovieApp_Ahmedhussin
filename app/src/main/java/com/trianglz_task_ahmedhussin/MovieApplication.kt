package com.trianglz_task_ahmedhussin

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application entry point. [HiltAndroidApp] generates the application-level
 * dependency container that every other Hilt component is a child of.
 *
 * It also configures the app-wide Coil image loader (posters and backdrops).
 */
@HiltAndroidApp
class MovieApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        // Logs go to Logcat only in debug builds. Release builds plant no tree, so every
        // Timber call is a no-op and nothing (URLs, errors) leaks into production logs.
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    /**
     * Images fade in over 300 ms instead of popping in (animation A2). Coil's default memory
     * and disk caches are kept, so posters seen once are still shown when offline.
     * Image requests use Coil's own HTTP client: image.tmdb.org must not get the API token.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .crossfade(IMAGE_CROSSFADE_MILLIS)
            .build()

    private companion object {
        const val IMAGE_CROSSFADE_MILLIS = 300
    }
}
