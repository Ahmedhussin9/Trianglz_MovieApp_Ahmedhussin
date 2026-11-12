package com.trianglz_task_ahmedhussin.feature.details.presentation.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.trianglz_task_ahmedhussin.R
import com.trianglz_task_ahmedhussin.core.designsystem.preview.ThemePreviews
import com.trianglz_task_ahmedhussin.core.designsystem.theme.MovieAppTheme
import com.trianglz_task_ahmedhussin.core.designsystem.theme.Spacing

private const val COLLAPSED_MAX_LINES = 4

/**
 * The synopsis (R6), collapsed to four lines with "Read more" when it is longer, expanding and
 * collapsing smoothly (animation A8). The button only appears when the text is actually cut off.
 */
@Composable
fun Synopsis(
    text: String?,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = stringResource(R.string.details_synopsis_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        if (text == null) {
            Text(
                text = stringResource(R.string.details_no_synopsis),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        // Known after layout, in either state: the screen can open already expanded (after rotation,
        // the ViewModel keeps the flag), so "longer than the collapsed size" is measured both ways.
        var isLong by remember(text) { mutableStateOf(false) }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = if (isExpanded) Int.MAX_VALUE else COLLAPSED_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                isLong = if (isExpanded) result.lineCount > COLLAPSED_MAX_LINES else result.hasVisualOverflow
            },
            modifier = Modifier.animateContentSize(),
        )
        if (isLong) {
            // No horizontal padding, so the label lines up with the text above (the button keeps its touch height).
            TextButton(onClick = onToggle, contentPadding = PaddingValues(0.dp)) {
                Text(stringResource(if (isExpanded) R.string.action_show_less else R.string.action_read_more))
            }
        }
    }
}

@ThemePreviews
@Composable
private fun SynopsisPreview() {
    MovieAppTheme {
        Surface {
            Synopsis(
                text = "Led by Woody, Andy's toys live happily in his room until Andy's birthday brings Buzz Lightyear " +
                    "onto the scene. Afraid of losing his place in Andy's heart, Woody plots against Buzz. But when " +
                    "circumstances separate Buzz and Woody from their owner, the duo eventually learns to put aside " +
                    "their differences.",
                isExpanded = false,
                onToggle = {},
                modifier = Modifier.padding(Spacing.lg),
            )
        }
    }
}
