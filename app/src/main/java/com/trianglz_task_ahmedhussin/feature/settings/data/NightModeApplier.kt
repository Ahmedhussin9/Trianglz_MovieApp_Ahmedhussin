package com.trianglz_task_ahmedhussin.feature.settings.data

import androidx.appcompat.app.AppCompatDelegate
import com.trianglz_task_ahmedhussin.feature.settings.domain.model.ThemeMode

/**
 * Applies a [ThemeMode] app-wide through AppCompat's night mode. It changes the configuration's
 * `uiMode`, so `isSystemInDarkTheme()` (MovieAppTheme), `values-night` resources and
 * `enableEdgeToEdge()`'s status-bar icon colours all follow the in-app choice, not just Compose.
 */
object NightModeApplier {

    /** Must run on the main thread: AppCompat recreates visible activities when the mode changes. */
    fun apply(mode: ThemeMode) {
        AppCompatDelegate.setDefaultNightMode(mode.toNightMode())
    }
}

/** [ThemeMode.System] follows the device; the others force light or dark. */
internal fun ThemeMode.toNightMode(): Int = when (this) {
    ThemeMode.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    ThemeMode.Light -> AppCompatDelegate.MODE_NIGHT_NO
    ThemeMode.Dark -> AppCompatDelegate.MODE_NIGHT_YES
}
