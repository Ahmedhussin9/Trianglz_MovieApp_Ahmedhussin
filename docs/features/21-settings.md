# 21: Settings, language and theme (T2.6, extras E1 + E2)

## 1. What was built
A Settings screen, opened from a gear button in the Movies top bar. It has two single-choice lists:
- **Language:** System default · English · العربية. Switching is instant: the app is recreated in the new language, Arabic is fully right-to-left, and TMDb content follows the new language too (Q14). The movie list is fetched again after a switch, and details refresh every time they open.
- **Theme:** System default · Light · Dark. It applies app-wide immediately, including the window and the status-bar icon colours.

Both choices survive restarts. Android 13+ also lists the app under Settings → App languages, kept in sync with the in-app choice.

These were extras E1 and E2, planned for after Phase 4. They were pulled forward at the user's request, so every later task can be checked in both languages and both themes.

While checking Arabic on the device, two places turned out to print Latin digits next to Arabic-Indic ones, and both were fixed:
- the list's release year (`2008` beside `٨٫٥`);
- the details release date (java.time keeps Latin digits unless it is given the locale's `DecimalStyle`).

## 2. Files created or changed

| Layer | File | Change |
|-------|------|--------|
| Domain | `feature/settings/domain/model/Settings.kt` | New: `ThemeMode`, `AppLanguage` |
| Domain | `feature/settings/domain/repository/SettingsRepository.kt` | New |
| Domain | `feature/settings/domain/usecase/SettingsUseCases.kt` | New: observe/set theme, get/set language |
| Data | `feature/settings/data/ThemeModeDataSource.kt` | New: Preferences DataStore |
| Data | `feature/settings/data/AppLanguageDataSource.kt` | New: AndroidX per-app locales, tag mappings |
| Data | `feature/settings/data/NightModeApplier.kt` | New: `ThemeMode` → AppCompat night mode |
| Data | `feature/settings/data/SettingsRepositoryImpl.kt` | New |
| DI | `feature/settings/di/SettingsModule.kt` | New: the DataStore singleton (on `@IoDispatcher`) and the repository binding |
| Presentation | `feature/settings/presentation/SettingsContract.kt`, `SettingsViewModel.kt`, `SettingsScreen.kt` | New |
| Presentation | `feature/movies/presentation/MoviesContract.kt`, `MoviesViewModel.kt`, `MoviesScreen.kt` | Gear button (`SettingsClicked` → `NavigateToSettings`), and a list refresh after a language change |
| Presentation | `feature/movies/presentation/model/MovieUi.kt` | The year is formatted with the locale's digits |
| Presentation | `feature/details/presentation/model/MovieDetailsUi.kt` | The release date uses the locale's `DecimalStyle` |
| App | `MainActivity.kt` | Now an `AppCompatActivity` |
| App | `MovieApplication.kt` | Applies the saved theme before the first screen, then follows changes |
| Navigation | `core/navigation/Routes.kt`, `AppNavigation.kt` | `SettingsRoute` and its entry; a double tap cannot open two Settings screens |
| Build | `gradle/libs.versions.toml`, `app/build.gradle.kts` | `appcompat` 1.8.0 and `datastore-preferences` 1.3.0 (current stable releases) |
| Manifest/res | `AndroidManifest.xml`, `res/xml/locales_config.xml` | `localeConfig`, and AppCompat's locale storage service for Android 12 and lower |
| Resources | `res/values/themes.xml` (`values-night/themes.xml` removed), strings in `values` and `values-ar`, `drawable/ic_settings.xml` | One AppCompat DayNight theme; settings strings; gear icon |
| Tests | `test/.../feature/settings/**`, `MoviesViewModelTest`, `MovieUiTest`, `MovieDetailsUiTest` | New and extended unit tests |
| Docs | `docs/PLAN.md` | T2.6 added and ticked; E1 and E2 marked done; package tree updated |

## 3. Data flow walkthrough

**Theme:**

```
SettingsScreen ─ThemeSelected(Dark)─▶ SettingsViewModel ─▶ SetThemeModeUseCase ─▶ SettingsRepositoryImpl
   ─▶ ThemeModeDataSource.setThemeMode (DataStore "theme_mode" = "Dark")
MovieApplication: observeThemeMode() ─▶ NightModeApplier.apply(Dark) ─▶ AppCompatDelegate.setDefaultNightMode(YES)
   ─▶ uiMode changes ─▶ isSystemInDarkTheme() / values-night / enableEdgeToEdge icons all follow
```

Start-up reads the saved value once before any activity exists, so the first frame already uses it:

```kotlin
private fun applyThemeMode() {
    NightModeApplier.apply(runBlocking { observeThemeMode().first() })
    observeThemeMode().onEach(NightModeApplier::apply).launchIn(applicationScope)
}
```

**Language:**

```
SettingsScreen ─LanguageSelected(Arabic)─▶ SettingsViewModel ─▶ SetAppLanguageUseCase ─▶ AppLanguageDataSource
   ─▶ AppCompatDelegate.setApplicationLocales("ar") ─▶ activity recreated in Arabic; Locale.getDefault() = ar
   ─▶ LanguageInterceptor sends language=ar-SA on the next request
Back on Movies: MoviesRoute sees the language differ from the one the list was loaded in ─▶ movies.refresh()
```

```kotlin
val language = LocalConfiguration.current.locales[0].language
var listLanguage by rememberSaveable { mutableStateOf(language) }
LaunchedEffect(language) {
    if (language != listLanguage) {
        listLanguage = language
        movies.refresh()
    }
}
```

## 4. Decisions made, and alternatives considered
- **AndroidX per-app locales for the language, not a DataStore value.** Android stores it (with AppCompat's service on Android 12 and lower), the system App languages screen stays in sync on Android 13+, and AppCompat handles the activity recreation and `Locale.getDefault()`. Saving a language in DataStore and wrapping contexts by hand was the alternative, with more code and more ways to drift.
- **AppCompat night mode for the theme, rather than only passing `darkTheme` into `MovieAppTheme`.** Night mode changes the configuration, so Compose (`isSystemInDarkTheme()`), XML `values-night` (the window background) and `enableEdgeToEdge()`'s status-bar icon colours stay consistent. With a Compose-only flag, a forced Light theme on a dark device would have light status-bar icons on a light screen.
- **DataStore for the theme**, as CLAUDE.md specifies for preferences. The stored value is the enum name, and unknown values fall back to System.
- **One blocking read at start-up.** It reads a tiny preferences file once, before any activity exists, which avoids the app opening in the system theme and then flipping. A splash screen that waits for the value was the non-blocking alternative, but it adds a dependency and a screen for the same result.
- **The list refresh is keyed on the language the list was loaded in, saved with `rememberSaveable`.** The Movies screen is not visible while Settings is open, so the check runs when the user comes back, and it survives the activity recreation the switch causes. Clearing the database on a switch was considered, but it would empty the screen and lose the offline fallback while the new language loads.
- **Language names are written in their own language** ("English", "العربية") and marked untranslatable, so each stays recognizable whatever language the UI is in.
- **Locale digits everywhere.** Arabic uses Arabic-Indic digits for ratings, votes and runtime by default, so the year and the date now match them.
- **The use cases are thin pass-throughs**, as for movies: the ViewModel and the app start-up depend on single-purpose entry points, not on the repository.

## 5. How it is tested
Unit tests (`testDebugUnitTest`, all pass):
- `SettingsViewModelTest` (5): the initial state from the saved theme and the current language; selecting a theme saves it and the state follows; selecting a language applies it and the state follows; re-selecting the current language does nothing; Back sends one effect.
- `ThemeModeDataSourceTest` (3, real DataStore in a temporary folder): the default is System, a saved choice reads back, and an unknown stored value falls back to System.
- `SettingsMappingTest` (3): language tags ↔ `AppLanguage` (including `ar-EG` and lists), and `ThemeMode` → AppCompat night modes.
- `MoviesViewModelTest`: the gear button sends one `NavigateToSettings`.
- `MovieUiTest` and `MovieDetailsUiTest`: the year and the date use Arabic-Indic digits for Arabic.

On the emulator (Pixel 4, API 29):
- **Settings and Arabic:** opened Settings and switched to العربية. The screen flipped to RTL with a mirrored back arrow.
- **List refetch:** back on the list, the movies were fetched again in Arabic (titles, and Arabic posters from TMDb).
- **Details in Arabic, light theme:** RTL layout, Arabic synopsis and genres, and readable status-bar icons.
- **Dark theme and restart:** chose Dark, force-stopped and relaunched. The app came back dark and in Arabic, with Arabic-Indic digits throughout.
- **Reset:** finally set both back to System default.

## 6. How to build it from scratch
1. Add `appcompat` and `datastore-preferences` (current stable) to the version catalog and the app module.
2. In the manifest, add `android:localeConfig="@xml/locales_config"` and AppCompat's `AppLocalesMetadataHolderService` with `autoStoreLocales`. Create `res/xml/locales_config.xml` listing `en` and `ar`.
3. Make the app theme `Theme.AppCompat.DayNight.NoActionBar` and `MainActivity` an `AppCompatActivity`.
4. Write the domain: `ThemeMode`, `AppLanguage`, `SettingsRepository` and the four use cases.
5. Write the data layer: `ThemeModeDataSource` (DataStore, fallback to System), `AppLanguageDataSource` (`get/setApplicationLocales` plus tag mappings), `NightModeApplier` and `SettingsRepositoryImpl`. Add `SettingsModule` to provide a singleton DataStore.
6. In `MovieApplication`, apply the saved theme once with `runBlocking { first() }`, then `onEach(apply).launchIn(MainScope())`.
7. Write the settings MVI contract, the ViewModel (theme from the flow, language held in a `MutableStateFlow`) and the screen (`selectableGroup` with whole-row `selectable` radio options).
8. Add `SettingsRoute` to navigation, the gear button and effect on Movies, and the language-keyed `movies.refresh()` in `MoviesRoute`.
9. Format years with `String.format(locale, "%d", …)` and dates with `.withDecimalStyle(DecimalStyle.of(locale))`.
10. Add the unit tests, then check on a device: switch language and theme, go back, and restart the app.
