package com.trianglz_task_ahmedhussin

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application entry point. [HiltAndroidApp] generates the application-level
 * dependency container that every other Hilt component is a child of.
 */
@HiltAndroidApp
class MovieApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Logs go to Logcat only in debug builds. Release builds plant no tree, so every
        // Timber call is a no-op and nothing (URLs, errors) leaks into production logs.
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
