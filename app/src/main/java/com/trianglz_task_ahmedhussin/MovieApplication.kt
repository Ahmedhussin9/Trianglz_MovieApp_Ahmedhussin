package com.trianglz_task_ahmedhussin

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.crossfade
import com.trianglz_task_ahmedhussin.feature.settings.data.NightModeApplier
import com.trianglz_task_ahmedhussin.feature.settings.domain.usecase.ObserveThemeModeUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

/**
 * Application entry point. [HiltAndroidApp] generates the application-level
 * dependency container that every other Hilt component is a child of.
 *
 * It also configures the app-wide Coil image loader (posters and backdrops) and applies the
 * saved theme choice.
 */
@HiltAndroidApp
class MovieApplication : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var observeThemeMode: ObserveThemeModeUseCase

    /** Lives as long as the process; used only to follow theme changes on the main thread. */
    private val applicationScope = MainScope()

    override fun onCreate() {
        super.onCreate()
        // Logs go to Logcat only in debug builds. Release builds plant no tree, so every
        // Timber call is a no-op and nothing (URLs, errors) leaks into production logs.
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        applyThemeMode()
    }

    /**
     * The saved theme is read once, blocking, before any activity exists: a tiny preferences
     * file, and it means the first screen already opens in the chosen theme instead of flashing
     * the system one and then switching. Later changes (from Settings) are applied as they come.
     */
    private fun applyThemeMode() {
        NightModeApplier.apply(runBlocking { observeThemeMode().first() })
        observeThemeMode().onEach(NightModeApplier::apply).launchIn(applicationScope)
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
