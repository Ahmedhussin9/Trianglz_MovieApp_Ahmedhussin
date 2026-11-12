package com.trianglz_task_ahmedhussin.feature.details.presentation.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.trianglz_task_ahmedhussin.R
import com.trianglz_task_ahmedhussin.core.designsystem.preview.ThemePreviews
import com.trianglz_task_ahmedhussin.core.designsystem.theme.MovieAppTheme
import com.trianglz_task_ahmedhussin.core.designsystem.theme.Spacing

/** Height of the backdrop hero; the top bar becomes solid once it has scrolled away. */
val BackdropHeight = 260.dp
private val PosterWidth = 112.dp
private val PosterOverlap = 96.dp
private const val POSTER_ASPECT_RATIO = 2f / 3f

/** The backdrop moves at half the scroll speed (parallax, animation A6). */
private const val PARALLAX_FACTOR = 0.5f

/**
 * The top of the details screen: a full-width backdrop that fades into the surface, the poster
 * overlapping it at the start edge, and the title with the tagline beside the poster.
 *
 * [scrollState] is read only inside `graphicsLayer`, so scrolling moves the backdrop in the
 * draw phase without recomposing anything.
 */
@Composable
fun DetailsHeader(
    title: String,
    tagline: String?,
    posterUrl: String?,
    backdropUrl: String?,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Backdrop(url = backdropUrl, scrollState = scrollState)
        Row(
            modifier = Modifier
                // The backdrop goes edge to edge, but text and poster stay clear of a side navigation bar.
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(start = Spacing.lg, end = Spacing.lg, top = BackdropHeight - PosterOverlap),
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Poster(url = posterUrl, modifier = Modifier.width(PosterWidth))
            // The title starts below the backdrop and grows downwards. Aligned to the poster's
            // bottom instead, a long title or large font size pushed it up over the image, where
            // dark text on a dark backdrop was unreadable (found in the T4.1 font-scale check).
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = PosterOverlap + Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
                if (tagline != null) {
                    Text(
                        text = tagline,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Backdrop(url: String?, scrollState: ScrollState) {
    val surface = MaterialTheme.colorScheme.surface
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(BackdropHeight)
            // The parallax shifts the image down; clipping keeps it from drawing over the content below.
            .clipToBounds(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = scrollState.value * PARALLAX_FACTOR }
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = null, // decorative: the title names the movie
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        // Fades the image into the page, so the title below it reads on any backdrop.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0f to Color.Transparent, 0.45f to Color.Transparent, 1f to surface)),
        )
    }
}

@Composable
private fun Poster(url: String?, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.aspectRatio(POSTER_ASPECT_RATIO),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shadowElevation = 6.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_movie),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp),
            )
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = null, // decorative: the title names the movie
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.medium),
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun DetailsHeaderPreview() {
    MovieAppTheme {
        Surface {
            DetailsHeader(
                title = "Toy Story",
                tagline = "Hang on for the comedy that goes to infinity and beyond!",
                posterUrl = null,
                backdropUrl = null,
                scrollState = rememberScrollState(),
            )
        }
    }
}
