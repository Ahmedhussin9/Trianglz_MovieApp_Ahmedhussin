package com.trianglz_task_ahmedhussin.feature.settings.data

import com.trianglz_task_ahmedhussin.feature.settings.domain.model.AppLanguage
import com.trianglz_task_ahmedhussin.feature.settings.domain.model.ThemeMode
import com.trianglz_task_ahmedhussin.feature.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

/**
 * Theme in DataStore, language in AndroidX per-app locales. Saving the theme does not apply it:
 * `MovieApplication` observes [themeMode] and applies every change, so the app start-up and the
 * settings screen go through the same path.
 */
class SettingsRepositoryImpl @Inject constructor(
    private val themeModeDataSource: ThemeModeDataSource,
    private val appLanguageDataSource: AppLanguageDataSource,
) : SettingsRepository {

    override val themeMode: Flow<ThemeMode> = themeModeDataSource.themeMode

    override suspend fun setThemeMode(mode: ThemeMode) {
        Timber.d("Theme mode set to %s", mode)
        themeModeDataSource.setThemeMode(mode)
    }

    override fun appLanguage(): AppLanguage = appLanguageDataSource.appLanguage()

    override fun setAppLanguage(language: AppLanguage) {
        Timber.d("App language set to %s", language)
        appLanguageDataSource.setAppLanguage(language)
    }
}
