package com.trianglz_task_ahmedhussin.feature.settings.presentation

import app.cash.turbine.test
import com.trianglz_task_ahmedhussin.feature.settings.domain.model.AppLanguage
import com.trianglz_task_ahmedhussin.feature.settings.domain.model.ThemeMode
import com.trianglz_task_ahmedhussin.feature.settings.domain.repository.SettingsRepository
import com.trianglz_task_ahmedhussin.feature.settings.domain.usecase.GetAppLanguageUseCase
import com.trianglz_task_ahmedhussin.feature.settings.domain.usecase.ObserveThemeModeUseCase
import com.trianglz_task_ahmedhussin.feature.settings.domain.usecase.SetAppLanguageUseCase
import com.trianglz_task_ahmedhussin.feature.settings.domain.usecase.SetThemeModeUseCase
import com.trianglz_task_ahmedhussin.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private class FakeSettingsRepository(
        themeMode: ThemeMode = ThemeMode.System,
        var language: AppLanguage = AppLanguage.System,
    ) : SettingsRepository {
        override val themeMode = MutableStateFlow(themeMode)
        val appliedLanguages = mutableListOf<AppLanguage>()

        override suspend fun setThemeMode(mode: ThemeMode) {
            themeMode.value = mode
        }

        override fun appLanguage(): AppLanguage = language

        override fun setAppLanguage(language: AppLanguage) {
            appliedLanguages += language
            this.language = language
        }
    }

    private fun viewModel(repository: SettingsRepository) = SettingsViewModel(
        observeThemeMode = ObserveThemeModeUseCase(repository),
        getAppLanguage = GetAppLanguageUseCase(repository),
        setThemeMode = SetThemeModeUseCase(repository),
        setAppLanguage = SetAppLanguageUseCase(repository),
    )

    @Test
    fun `state shows the saved theme and the current language`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel(FakeSettingsRepository(themeMode = ThemeMode.Dark, language = AppLanguage.Arabic))

        viewModel.state.test {
            assertEquals(SettingsUiState(language = AppLanguage.Arabic, themeMode = ThemeMode.Dark), awaitItem())
        }
    }

    @Test
    fun `selecting a theme saves it and the state follows`() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeSettingsRepository()
        val viewModel = viewModel(repository)

        viewModel.state.test {
            assertEquals(ThemeMode.System, awaitItem().themeMode)

            viewModel.onAction(SettingsUiAction.ThemeSelected(ThemeMode.Light))

            assertEquals(ThemeMode.Light, awaitItem().themeMode)
            assertEquals(ThemeMode.Light, repository.themeMode.value)
        }
    }

    @Test
    fun `selecting a language applies it and the state follows`() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeSettingsRepository()
        val viewModel = viewModel(repository)

        viewModel.state.test {
            assertEquals(AppLanguage.System, awaitItem().language)

            viewModel.onAction(SettingsUiAction.LanguageSelected(AppLanguage.Arabic))

            assertEquals(AppLanguage.Arabic, awaitItem().language)
            assertEquals(listOf(AppLanguage.Arabic), repository.appliedLanguages)
        }
    }

    @Test
    fun `selecting the current language does not apply it again`() {
        val repository = FakeSettingsRepository(language = AppLanguage.English)
        val viewModel = viewModel(repository)

        viewModel.onAction(SettingsUiAction.LanguageSelected(AppLanguage.English))

        assertEquals(emptyList<AppLanguage>(), repository.appliedLanguages)
    }

    @Test
    fun `Back sends a single navigation effect`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel(FakeSettingsRepository())

        viewModel.effects.test {
            viewModel.onAction(SettingsUiAction.Back)

            assertEquals(SettingsEffect.NavigateBack, awaitItem())
            expectNoEvents()
        }
    }
}
