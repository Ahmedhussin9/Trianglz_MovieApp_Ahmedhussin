package com.trianglz_task_ahmedhussin.feature.details.presentation.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.trianglz_task_ahmedhussin.R

private val TopAppBarHeight = 64.dp

/** Strength of the status-bar protection gradient while the bar is still transparent. */
private const val PROTECTION_ALPHA = 0.85f

/**
 * Top bar drawn over the details content (animation A6, collapsing top bar).
 *
 * Over the backdrop it is transparent, with a gradient in the surface colour behind the status bar
 * (the `edge-to-edge` skill's system-bar protection) so the status icons, which `enableEdgeToEdge`
 * colours for the theme, stay readable on any image. As the backdrop scrolls away the bar fills
 * with the surface colour and the title fades in. Both read [scrollState] in the draw phase only.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsTopBar(
    title: String?,
    scrollState: ScrollState,
    isRefreshing: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val surface = MaterialTheme.colorScheme.surface
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.getTop(density)
    // The bar is fully solid when the backdrop's bottom edge reaches it.
    val collapseDistancePx = with(density) { (BackdropHeight - TopAppBarHeight).toPx() } - statusBarHeight
    val solidFraction = { (scrollState.value / collapseDistancePx).coerceIn(0f, 1f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val fraction = solidFraction()
                drawRect(
                    brush = Brush.verticalGradient(listOf(surface.copy(alpha = PROTECTION_ALPHA * (1f - fraction)), Color.Transparent)),
                )
                drawRect(color = surface.copy(alpha = fraction))
            },
    ) {
        TopAppBar(
            title = {
                if (title != null) {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.graphicsLayer { alpha = solidFraction() },
                    )
                }
            },
            navigationIcon = {
                FilledTonalIconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.action_back),
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
        )
        if (isRefreshing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}
