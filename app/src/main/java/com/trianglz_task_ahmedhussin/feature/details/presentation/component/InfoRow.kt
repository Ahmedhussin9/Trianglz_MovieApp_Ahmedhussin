package com.trianglz_task_ahmedhussin.feature.details.presentation.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.trianglz_task_ahmedhussin.R
import com.trianglz_task_ahmedhussin.core.designsystem.preview.ThemePreviews
import com.trianglz_task_ahmedhussin.core.designsystem.theme.MovieAppTheme
import com.trianglz_task_ahmedhussin.core.designsystem.theme.Spacing
import com.trianglz_task_ahmedhussin.feature.details.domain.model.ReleaseStatus

private const val SEPARATOR = " · "
private const val MINUTES_PER_HOUR = 60

/**
 * "Oct 30, 1995 · 1 h 21 min · English", leaving out whatever is unknown. A movie that is not
 * out yet also gets a status label ("In production"), since its date may be only a plan.
 */
@Composable
fun InfoRow(
    releaseDateText: String?,
    runtimeMinutes: Int?,
    languageText: String?,
    status: ReleaseStatus,
    modifier: Modifier = Modifier,
) {
    val runtimeText = runtimeMinutes?.let { runtimeText(it) }
    val facts = listOfNotNull(releaseDateText, runtimeText, languageText)
    val statusLabel = status.labelRes()
    if (facts.isEmpty() && statusLabel == null) return

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        if (facts.isNotEmpty()) {
            Text(
                text = facts.joinToString(SEPARATOR),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (statusLabel != null) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Text(
                    text = stringResource(statusLabel),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                )
            }
        }
    }
}

@Composable
private fun runtimeText(minutes: Int): String {
    val hours = minutes / MINUTES_PER_HOUR
    val rest = minutes % MINUTES_PER_HOUR
    return when {
        hours == 0 -> stringResource(R.string.details_runtime_minutes, rest)
        rest == 0 -> stringResource(R.string.details_runtime_hours, hours)
        else -> stringResource(R.string.details_runtime_hours_minutes, hours, rest)
    }
}

/** Released movies (the usual case) and unknown statuses get no label. */
@StringRes
private fun ReleaseStatus.labelRes(): Int? = when (this) {
    ReleaseStatus.Rumored -> R.string.details_status_rumored
    ReleaseStatus.Planned -> R.string.details_status_planned
    ReleaseStatus.InProduction -> R.string.details_status_in_production
    ReleaseStatus.PostProduction -> R.string.details_status_post_production
    ReleaseStatus.Canceled -> R.string.details_status_canceled
    ReleaseStatus.Released, ReleaseStatus.Unknown -> null
}

@ThemePreviews
@Composable
private fun InfoRowPreview() {
    MovieAppTheme {
        Surface {
            InfoRow(
                releaseDateText = "Oct 30, 1995",
                runtimeMinutes = 81,
                languageText = "English",
                status = ReleaseStatus.PostProduction,
                modifier = Modifier.padding(Spacing.lg),
            )
        }
    }
}
