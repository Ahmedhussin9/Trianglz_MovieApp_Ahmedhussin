package com.trianglz_task_ahmedhussin.feature.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.trianglz_task_ahmedhussin.feature.settings.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

/**
 * Stores the theme choice in Preferences DataStore. The enum name is stored as text, and a
 * missing or unknown value (for example after renaming an entry) falls back to [ThemeMode.System].
 */
class ThemeModeDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    val themeMode: Flow<ThemeMode> = dataStore.data
        .catch { e ->
            // An unreadable file must not break the app: use the default theme until the next write.
            if (e !is IOException) throw e
            Timber.w(e, "Reading settings failed; using defaults")
            emit(emptyPreferences())
        }
        .map { preferences -> preferences[THEME_MODE].toThemeMode() }
        .distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.name }
    }

    private fun String?.toThemeMode(): ThemeMode = ThemeMode.entries.firstOrNull { it.name == this } ?: ThemeMode.System

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
