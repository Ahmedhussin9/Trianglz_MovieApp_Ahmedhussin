package com.trianglz_task_ahmedhussin.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.trianglz_task_ahmedhussin.R

/*
 * Outfit (geometric, cinematic) for display/headline/title, Inter (highly legible) for
 * body and labels. Both are bundled (OFL, see docs/licenses) so text never depends on the
 * network. Neither has Arabic glyphs; Android falls back to the system Arabic font.
 * Sizes and line heights keep the Material 3 type scale; only families and weights change.
 */

private val Outfit = FontFamily(
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold),
)

private val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

private val Baseline = Typography()

internal val MovieTypography = Typography(
    displayLarge = Baseline.displayLarge.copy(fontFamily = Outfit, fontWeight = FontWeight.Bold),
    displayMedium = Baseline.displayMedium.copy(fontFamily = Outfit, fontWeight = FontWeight.Bold),
    displaySmall = Baseline.displaySmall.copy(fontFamily = Outfit, fontWeight = FontWeight.Bold),
    headlineLarge = Baseline.headlineLarge.copy(fontFamily = Outfit, fontWeight = FontWeight.SemiBold),
    headlineMedium = Baseline.headlineMedium.copy(fontFamily = Outfit, fontWeight = FontWeight.SemiBold),
    headlineSmall = Baseline.headlineSmall.copy(fontFamily = Outfit, fontWeight = FontWeight.SemiBold),
    titleLarge = Baseline.titleLarge.copy(fontFamily = Outfit, fontWeight = FontWeight.SemiBold),
    titleMedium = Baseline.titleMedium.copy(fontFamily = Outfit, fontWeight = FontWeight.Medium),
    titleSmall = Baseline.titleSmall.copy(fontFamily = Outfit, fontWeight = FontWeight.Medium),
    bodyLarge = Baseline.bodyLarge.copy(fontFamily = Inter, fontWeight = FontWeight.Normal),
    bodyMedium = Baseline.bodyMedium.copy(fontFamily = Inter, fontWeight = FontWeight.Normal),
    bodySmall = Baseline.bodySmall.copy(fontFamily = Inter, fontWeight = FontWeight.Normal),
    labelLarge = Baseline.labelLarge.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold),
    labelMedium = Baseline.labelMedium.copy(fontFamily = Inter, fontWeight = FontWeight.Medium),
    labelSmall = Baseline.labelSmall.copy(fontFamily = Inter, fontWeight = FontWeight.Medium),
)
