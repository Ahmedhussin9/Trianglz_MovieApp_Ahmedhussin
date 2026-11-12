package com.trianglz_task_ahmedhussin.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trianglz_task_ahmedhussin.feature.settings.domain.usecase.GetAppLanguageUseCase
import com.trianglz_task_ahmedhussin.feature.settings.domain.usecase.ObserveThemeModeUseCase
import com.trianglz_task_ahmedhussin.feature.settings.domain.usecase.SetAppLanguageUseCase
import com.trianglz_task_ahmedhussin.feature.settings.domain.usecase.SetThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * State holder of the Settings screen (MVI, see `SettingsContract.kt`).
 *
 * A language or theme change recreates the activity; this ViewModel survives it (configuration
 * change), so the screen comes back showing the new selection.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeThemeMode: ObserveThemeModeUseCase,
    getAppLanguage: GetAppLanguageUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val setAppLanguage: SetAppLanguageUseCase,
) : ViewModel() {

    /** AppCompat offers no stream for the language, so the ViewModel keeps the current choice. */
    private val language = MutableStateFlow(getAppLanguage())

    val state: StateFlow<SettingsUiState> = combine(language, observeThemeMode()) { language, themeMode ->
        SettingsUiState(language = language, themeMode = themeMode)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = SettingsUiState(language = language.value),
    )

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)

    /** One-time events; each is delivered to exactly one collector (no replay on rotation). */
    val effects: Flow<SettingsEffect> = _effects.receiveAsFlow()

    fun onAction(action: SettingsUiAction) {
        Timber.d("Action: %s", action)
        when (action) {
            is SettingsUiAction.LanguageSelected -> if (action.language != language.value) {
                language.value = action.language
                setAppLanguage(action.language)
            }
            is SettingsUiAction.ThemeSelected -> viewModelScope.launch { setThemeMode(action.mode) }
            SettingsUiAction.Back -> viewModelScope.launch { _effects.send(SettingsEffect.NavigateBack) }
        }
    }

    private companion object {
        /** Keeps the theme subscription alive across the short recreate a theme change causes. */
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
