package com.trianglz_task_ahmedhussin

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Instrumentation runner that starts the app process with [HiltTestApplication] instead of
 * `MovieApplication`, so `@HiltAndroidTest` classes can build a test component and replace
 * bindings (for example a fake repository via `@TestInstallIn`).
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(classLoader: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(classLoader, HiltTestApplication::class.java.name, context)
}
