package com.trianglz_task_ahmedhussin.feature.settings.domain.repository

import com.trianglz_task_ahmedhussin.feature.settings.domain.model.AppLanguage
import com.trianglz_task_ahmedhussin.feature.settings.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/** User preferences. Implemented in the data layer, which also applies them to the app. */
interface SettingsRepository {

    /** The saved theme choice; emits again whenever it changes. */
    val themeMode: Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)

    fun appLanguage(): AppLanguage

    /** Saves and applies the language; visible screens are recreated in the new language. */
    fun setAppLanguage(language: AppLanguage)
}
