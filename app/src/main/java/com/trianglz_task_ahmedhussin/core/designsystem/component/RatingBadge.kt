package com.trianglz_task_ahmedhussin.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.trianglz_task_ahmedhussin.R
import com.trianglz_task_ahmedhussin.core.designsystem.preview.ThemePreviews
import com.trianglz_task_ahmedhussin.core.designsystem.theme.MovieAppTheme
import com.trianglz_task_ahmedhussin.core.designsystem.theme.Spacing

/**
 * Amber pill showing a TMDb rating (0–10) with one decimal, for example "★ 7.8".
 *
 * Screen readers hear the full sentence ("Rated 7.8 out of 10") instead of "star, 7.8".
 *
 * @param rating already-formatted rating text, so number formatting stays in the mapper.
 */
@Composable
fun RatingBadge(rating: String, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.rating_content_description, rating)
    Surface(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_star),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
            Text(text = rating, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@ThemePreviews
@Composable
private fun RatingBadgePreview() {
    MovieAppTheme {
        Surface { RatingBadge(rating = "7.8", modifier = Modifier.padding(Spacing.sm)) }
    }
}
