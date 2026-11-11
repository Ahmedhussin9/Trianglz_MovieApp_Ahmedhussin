package com.trianglz_task_ahmedhussin.feature.settings.data

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.trianglz_task_ahmedhussin.feature.settings.domain.model.AppLanguage
import javax.inject.Inject

/**
 * The in-app language, through AndroidX per-app locales. Android 13+ stores the choice itself
 * (and shows it in the system's App languages settings); on older versions AppCompat stores it
 * through `AppLocalesMetadataHolderService` (see the manifest). Setting it recreates visible
 * activities in the new language, which also changes `Locale.getDefault()` and so the TMDb
 * `language` parameter (Q14).
 */
class AppLanguageDataSource @Inject constructor() {

    fun appLanguage(): AppLanguage = AppCompatDelegate.getApplicationLocales().toLanguageTags().toAppLanguage()

    /** Must run on the main thread: AppCompat recreates the visible activities. */
    fun setAppLanguage(language: AppLanguage) {
        val locales = language.toLanguageTag()?.let(LocaleListCompat::forLanguageTags) ?: LocaleListCompat.getEmptyLocaleList()
        AppCompatDelegate.setApplicationLocales(locales)
    }
}

/** "" (no app-specific language) means System; otherwise the first tag's language decides. */
internal fun String.toAppLanguage(): AppLanguage = when (substringBefore(',').substringBefore('-').lowercase()) {
    "" -> AppLanguage.System
    "ar" -> AppLanguage.Arabic
    "en" -> AppLanguage.English
    else -> AppLanguage.System
}

/** `null` clears the app-specific language, so the device language applies. */
internal fun AppLanguage.toLanguageTag(): String? = when (this) {
    AppLanguage.System -> null
    AppLanguage.English -> "en"
    AppLanguage.Arabic -> "ar"
}
