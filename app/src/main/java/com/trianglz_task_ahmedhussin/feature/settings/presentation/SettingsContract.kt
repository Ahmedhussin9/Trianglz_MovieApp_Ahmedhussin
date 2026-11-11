package com.trianglz_task_ahmedhussin.feature.settings.presentation

import com.trianglz_task_ahmedhussin.feature.settings.domain.model.AppLanguage
import com.trianglz_task_ahmedhussin.feature.settings.domain.model.ThemeMode

/*
 * MVI contract of the Settings screen:
 *   Model  = SettingsUiState  (immutable, exposed as StateFlow)
 *   Intent = SettingsUiAction (sent through a single onAction())
 *   Effect = SettingsEffect   (one-time events, delivered once through a Channel)
 */

data class SettingsUiState(
    val language: AppLanguage = AppLanguage.System,
    val themeMode: ThemeMode = ThemeMode.System,
)

sealed interface SettingsUiAction {
    data class LanguageSelected(val language: AppLanguage) : SettingsUiAction
    data class ThemeSelected(val mode: ThemeMode) : SettingsUiAction
    data object Back : SettingsUiAction
}

sealed interface SettingsEffect {
    data object NavigateBack : SettingsEffect
}
