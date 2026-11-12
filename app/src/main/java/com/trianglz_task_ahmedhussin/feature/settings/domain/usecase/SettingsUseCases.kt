package com.trianglz_task_ahmedhussin.feature.settings.domain.usecase

import com.trianglz_task_ahmedhussin.feature.settings.domain.model.AppLanguage
import com.trianglz_task_ahmedhussin.feature.settings.domain.model.ThemeMode
import com.trianglz_task_ahmedhussin.feature.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/*
 * Settings use cases. They are pass-throughs today, kept so the ViewModel and the app start-up
 * depend on single-purpose entry points rather than on the whole repository.
 */

/** The saved theme, emitted again whenever it changes. */
class ObserveThemeModeUseCase @Inject constructor(private val repository: SettingsRepository) {
    operator fun invoke(): Flow<ThemeMode> = repository.themeMode
}

/** Saves a new theme choice; the app applies it as soon as it is stored. */
class SetThemeModeUseCase @Inject constructor(private val repository: SettingsRepository) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}

/** The in-app language currently in use. */
class GetAppLanguageUseCase @Inject constructor(private val repository: SettingsRepository) {
    operator fun invoke(): AppLanguage = repository.appLanguage()
}

/** Saves and applies a new language; visible screens are recreated in it. */
class SetAppLanguageUseCase @Inject constructor(private val repository: SettingsRepository) {
    operator fun invoke(language: AppLanguage) = repository.setAppLanguage(language)
}
