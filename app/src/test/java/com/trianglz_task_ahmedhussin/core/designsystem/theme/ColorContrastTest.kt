package com.trianglz_task_ahmedhussin.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every text/icon colour must be readable on the container it is drawn on.
 * WCAG 2.1 AA requires a contrast ratio of at least 4.5:1 for normal-size text.
 */
class ColorContrastTest {

    @Test
    fun `light scheme pairs meet WCAG AA`() = assertReadable("light", LightColors)

    @Test
    fun `dark scheme pairs meet WCAG AA`() = assertReadable("dark", DarkColors)

    private fun assertReadable(name: String, scheme: ColorScheme) {
        val pairs = with(scheme) {
            listOf(
                "primary" to (primary to onPrimary),
                "primaryContainer" to (primaryContainer to onPrimaryContainer),
                "secondary" to (secondary to onSecondary),
                "secondaryContainer" to (secondaryContainer to onSecondaryContainer),
                "tertiary" to (tertiary to onTertiary),
                "tertiaryContainer" to (tertiaryContainer to onTertiaryContainer),
                "error" to (error to onError),
                "errorContainer" to (errorContainer to onErrorContainer),
                "background" to (background to onBackground),
                "surface" to (surface to onSurface),
                "surface/onSurfaceVariant" to (surface to onSurfaceVariant),
                "surfaceVariant" to (surfaceVariant to onSurfaceVariant),
                "surfaceContainerHighest" to (surfaceContainerHighest to onSurface),
                "surfaceContainerHigh/primary icon" to (surfaceContainerHigh to primary),
                "inverseSurface" to (inverseSurface to inverseOnSurface),
            )
        }
        val failures = pairs.mapNotNull { (role, colors) ->
            val ratio = contrast(colors.first, colors.second)
            if (ratio < MIN_CONTRAST) "$role = ${"%.2f".format(ratio)}" else null
        }
        assertTrue("$name scheme below $MIN_CONTRAST:1 → $failures", failures.isEmpty())
    }

    private fun contrast(a: Color, b: Color): Double {
        val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (light + 0.05) / (dark + 0.05)
    }

    private companion object {
        const val MIN_CONTRAST = 4.5
    }
}
