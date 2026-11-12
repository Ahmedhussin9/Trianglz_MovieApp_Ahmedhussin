package com.trianglz_task_ahmedhussin.feature.movies.presentation.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.trianglz_task_ahmedhussin.R
import com.trianglz_task_ahmedhussin.core.designsystem.component.RatingBadge
import com.trianglz_task_ahmedhussin.core.designsystem.preview.ThemePreviews
import com.trianglz_task_ahmedhussin.core.designsystem.theme.MovieAppTheme
import com.trianglz_task_ahmedhussin.core.designsystem.theme.OnPosterScrim
import com.trianglz_task_ahmedhussin.core.designsystem.theme.Spacing
import com.trianglz_task_ahmedhussin.feature.movies.presentation.model.MovieUi

private const val POSTER_ASPECT_RATIO = 2f / 3f
private const val PRESSED_SCALE = 0.96f
private const val SCRIM_ALPHA = 0.85f

/**
 * A poster card showing the basic movie info the task asks for: title, poster and release
 * year (R2), plus the rating badge. It shrinks slightly while pressed (animation A1).
 *
 * @param pressFeedbackEnabled pass `false` while the list scrolls: every fling starts with a
 * finger-down on some card, which would otherwise make cards shrink and bounce back while
 * scrolling. The press animation should only answer a deliberate tap or hold.
 */
@Composable
fun MovieCard(
    movie: MovieUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pressFeedbackEnabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && pressFeedbackEnabled) PRESSED_SCALE else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardPressScale",
    )

    Card(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        interactionSource = interactionSource,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(POSTER_ASPECT_RATIO),
        ) {
            PosterPlaceholder(Modifier.fillMaxSize())
            if (movie.posterUrl != null) {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = null, // the title below already names the movie
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            TitleOverlay(
                title = movie.title,
                year = movie.yearText,
                modifier = Modifier.align(Alignment.BottomStart),
            )
            if (movie.ratingText != null) {
                RatingBadge(
                    rating = movie.ratingText,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.sm),
                )
            }
        }
    }
}

/** Shown under the image: visible while it loads and when there is no poster at all. */
@Composable
private fun PosterPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_movie),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp),
        )
    }
}

/** Dark gradient so the title stays readable on any poster. */
@Composable
private fun TitleOverlay(title: String, year: String?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA))),
            )
            .padding(start = Spacing.md, end = Spacing.md, top = Spacing.xl, bottom = Spacing.md),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = OnPosterScrim,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (year != null) {
            Text(
                text = year,
                style = MaterialTheme.typography.labelMedium,
                color = OnPosterScrim.copy(alpha = 0.8f),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun MovieCardPreview() {
    MovieAppTheme {
        MovieCard(
            movie = MovieUi(id = 1, title = "Project Hail Mary", posterUrl = null, yearText = "2026", ratingText = "8.6"),
            onClick = {},
            modifier = Modifier.width(160.dp),
        )
    }
}
