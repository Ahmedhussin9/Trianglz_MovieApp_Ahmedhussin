package com.trianglz_task_ahmedhussin

import android.os.Bundle
import android.os.LocaleList
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.trianglz_task_ahmedhussin.core.designsystem.theme.MovieAppTheme
import com.trianglz_task_ahmedhussin.core.navigation.AppNavigation
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single activity hosting the Compose UI. Drawing edge-to-edge is required from targetSdk 35;
 * each screen's Scaffold/TopAppBar applies the system-bar insets.
 *
 * An [AppCompatActivity] (still a ComponentActivity) because the in-app language and theme use
 * AppCompat's per-app locales and night mode, which apply to AppCompat activities.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        syncDefaultLocaleWithResources()
        setContent {
            MovieAppTheme {
                AppNavigation()
            }
        }
    }

    /**
     * Makes `Locale.getDefault()` the locale this activity's resources use.
     *
     * Numbers and dates are formatted with `Locale.getDefault()` (ViewModels, the TMDb `language`
     * parameter), while translated strings with numbers are formatted by Android with the
     * resources' locale. With a per-app language on Android 13+ (an Arabic app on an English
     * phone), the platform does not reliably update the default, so the two could disagree and
     * mix digit styles. A language change recreates this activity, so syncing here keeps one
     * source of truth.
     */
    private fun syncDefaultLocaleWithResources() {
        val locales = resources.configuration.locales
        if (locales != LocaleList.getDefault()) {
            LocaleList.setDefault(locales)
        }
    }
}
