package com.trianglz_task_ahmedhussin.feature.details.presentation.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trianglz_task_ahmedhussin.R
import com.trianglz_task_ahmedhussin.core.designsystem.preview.ThemePreviews
import com.trianglz_task_ahmedhussin.core.designsystem.theme.MovieAppTheme
import com.trianglz_task_ahmedhussin.core.designsystem.theme.Spacing

private val RingSize = 72.dp
private val RingStroke = 6.dp
private const val FILL_DURATION_MS = 900
private const val FULL_CIRCLE = 360f
private const val START_AT_TOP = -90f
private const val MAX_RATING = 10f
private const val NO_RATING = "–"

/**
 * The TMDb rating as a ring that fills from 0 and a number that counts up to it (animation A7),
 * next to the vote count. A movie without votes shows an empty ring and "Not rated yet".
 *
 * Screen readers hear one sentence ("Rated 8.0 out of 10, 18,924 votes") instead of the parts.
 */
@Composable
fun RatingSection(
    ratingFraction: Float,
    ratingText: String?,
    voteCount: Int,
    voteCountText: String,
    modifier: Modifier = Modifier,
) {
    val votesText = pluralStringResource(R.plurals.details_votes, voteCount, voteCountText)
    val description = if (ratingText != null) {
        stringResource(R.string.details_rating_description, ratingText, votesText)
    } else {
        stringResource(R.string.details_not_rated)
    }
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        RatingRing(fraction = ratingFraction, ratingText = ratingText)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(text = stringResource(R.string.details_rating_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (ratingText != null) votesText else stringResource(R.string.details_not_rated),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RatingRing(fraction: Float, ratingText: String?, modifier: Modifier = Modifier) {
    // Starts at 0 on first display, then animates to every new value (cached → fresh) from where it is.
    val progress = remember { Animatable(0f) }
    LaunchedEffect(fraction) {
        progress.animateTo(fraction, tween(durationMillis = FILL_DURATION_MS, easing = FastOutSlowInEasing))
    }
    val locale = LocalConfiguration.current.locales[0]
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val progressColor = MaterialTheme.colorScheme.primary

    Box(modifier = modifier.size(RingSize), contentAlignment = Alignment.Center) {
        // The arc is drawn from the animated value in the draw phase only: no recomposition per frame.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = RingStroke.toPx(), cap = StrokeCap.Round)
            drawArc(color = trackColor, startAngle = 0f, sweepAngle = FULL_CIRCLE, useCenter = false, style = stroke)
            drawArc(
                color = progressColor,
                startAngle = START_AT_TOP,
                sweepAngle = FULL_CIRCLE * progress.value,
                useCenter = false,
                style = stroke,
            )
        }
        // The counting number does recompose per frame, but only this Text, and only for 0.9 s.
        val number = when {
            ratingText == null -> NO_RATING
            progress.isRunning -> String.format(locale, "%.1f", progress.value * MAX_RATING)
            else -> ratingText
        }
        Text(text = number, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@ThemePreviews
@Composable
private fun RatingSectionPreview() {
    MovieAppTheme {
        Surface {
            RatingSection(
                ratingFraction = 0.797f,
                ratingText = "8.0",
                voteCount = 18924,
                voteCountText = "18,924",
                modifier = Modifier.padding(Spacing.lg),
            )
        }
    }
}
