package com.trianglz_task_ahmedhussin.feature.details.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.trianglz_task_ahmedhussin.core.designsystem.preview.ThemePreviews
import com.trianglz_task_ahmedhussin.core.designsystem.theme.MovieAppTheme
import com.trianglz_task_ahmedhussin.core.designsystem.theme.Spacing
import com.trianglz_task_ahmedhussin.feature.details.domain.model.Genre

/**
 * The movie's genres (R6) as teal tonal chips that wrap onto new lines. They are labels, not
 * buttons, so they are plain surfaces instead of clickable Material chips.
 */
@Composable
fun GenreChips(genres: List<Genre>, modifier: Modifier = Modifier) {
    if (genres.isEmpty()) return
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        genres.forEach { genre ->
            key(genre.id) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ) {
                    Text(
                        text = genre.name,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    )
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun GenreChipsPreview() {
    MovieAppTheme {
        Surface {
            GenreChips(
                genres = listOf(Genre(16, "Animation"), Genre(12, "Adventure"), Genre(10751, "Family"), Genre(35, "Comedy")),
                modifier = Modifier.padding(Spacing.lg),
            )
        }
    }
}
