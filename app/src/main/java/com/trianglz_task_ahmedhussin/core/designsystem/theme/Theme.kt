package com.trianglz_task_ahmedhussin.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * The app theme. Light and dark schemes are defined only here (CLAUDE.md); composables
 * read colours through `MaterialTheme.colorScheme` and never hardcode them.
 *
 * Dynamic colour (Material You) is intentionally not used, so the branded palette is the
 * same on every device (R33). A user-facing theme switch is extra E1.
 */
@Composable
fun MovieAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MovieTypography,
        shapes = MovieShapes,
        content = content,
    )
}
