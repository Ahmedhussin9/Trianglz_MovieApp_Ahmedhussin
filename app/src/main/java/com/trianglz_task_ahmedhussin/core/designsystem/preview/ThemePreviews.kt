package com.trianglz_task_ahmedhussin.core.designsystem.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/** Renders a composable in light, dark and right-to-left (Arabic) configurations. */
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(name = "RTL (Arabic)", showBackground = true, locale = "ar")
annotation class ThemePreviews
