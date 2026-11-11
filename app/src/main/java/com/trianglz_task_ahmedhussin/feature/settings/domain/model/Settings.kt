package com.trianglz_task_ahmedhussin.feature.settings.domain.model

/** Light or dark appearance (extra E1). [System] follows the device's dark theme setting. */
enum class ThemeMode { System, Light, Dark }

/**
 * The app's display language (extra E2), which also decides the language of TMDb content (Q14).
 * [System] follows the device language (English is used for anything other than Arabic).
 */
enum class AppLanguage { System, English, Arabic }
