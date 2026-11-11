package com.trianglz_task_ahmedhussin.feature.movies.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trianglz_task_ahmedhussin.R
import com.trianglz_task_ahmedhussin.core.designsystem.preview.ThemePreviews
import com.trianglz_task_ahmedhussin.core.designsystem.theme.MovieAppTheme
import com.trianglz_task_ahmedhussin.core.designsystem.theme.Spacing
import com.trianglz_task_ahmedhussin.core.domain.error.DomainError

/**
 * Non-blocking notice shown above the list when the background refresh failed but cached
 * movies are on screen (R9): the user keeps browsing and can retry or dismiss.
 */
@Composable
fun CachedDataNotice(
    error: DomainError,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(start = Spacing.md, top = Spacing.xs, bottom = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                painter = painterResource(
                    if (error == DomainError.NoInternet || error == DomainError.Timeout) R.drawable.ic_cloud_off else R.drawable.ic_error_outline,
                ),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(R.string.movies_cached_notice),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
            IconButton(onClick = onDismiss) {
                Icon(painter = painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.action_dismiss))
            }
        }
    }
}

@ThemePreviews
@Composable
private fun CachedDataNoticePreview() {
    MovieAppTheme {
        CachedDataNotice(error = DomainError.NoInternet, onRetry = {}, onDismiss = {}, modifier = Modifier.padding(Spacing.lg))
    }
}
