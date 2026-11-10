package com.trianglz_task_ahmedhussin.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.trianglz_task_ahmedhussin.R
import com.trianglz_task_ahmedhussin.core.designsystem.preview.ThemePreviews
import com.trianglz_task_ahmedhussin.core.designsystem.theme.MovieAppTheme
import com.trianglz_task_ahmedhussin.core.domain.error.DomainError
import com.trianglz_task_ahmedhussin.core.ui.error.messageRes

/**
 * Full-screen error state with a Retry action.
 *
 * Takes the [DomainError] itself so screens pass the error straight through: the icon and
 * localized message are chosen here, keeping error presentation identical everywhere (R21).
 */
@Composable
fun ErrorView(
    error: DomainError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StatusLayout(
        iconRes = error.iconRes(),
        title = stringResource(error.messageRes()),
        modifier = modifier,
        action = { RetryButton(onClick = onRetry) },
    )
}

/** Tonal Retry button, also used inline (for example in the paging footer). */
@Composable
fun RetryButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_refresh),
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(stringResource(R.string.action_retry))
    }
}

@DrawableRes
private fun DomainError.iconRes(): Int = when (this) {
    DomainError.NoInternet, DomainError.Timeout -> R.drawable.ic_cloud_off
    else -> R.drawable.ic_error_outline
}

@ThemePreviews
@Composable
private fun ErrorViewPreview() {
    MovieAppTheme {
        Surface {
            ErrorView(error = DomainError.NoInternet, onRetry = {})
        }
    }
}
