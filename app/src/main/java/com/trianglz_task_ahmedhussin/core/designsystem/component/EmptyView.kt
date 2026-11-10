package com.trianglz_task_ahmedhussin.core.designsystem.component

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.trianglz_task_ahmedhussin.R
import com.trianglz_task_ahmedhussin.core.designsystem.preview.ThemePreviews
import com.trianglz_task_ahmedhussin.core.designsystem.theme.MovieAppTheme

/** Full-screen state shown when a load succeeded but returned nothing. */
@Composable
fun EmptyView(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    StatusLayout(
        iconRes = R.drawable.ic_movie,
        title = title,
        message = message,
        modifier = modifier,
    )
}

@ThemePreviews
@Composable
private fun EmptyViewPreview() {
    MovieAppTheme {
        Surface {
            EmptyView(
                title = stringResource(R.string.app_name),
                message = stringResource(R.string.error_not_found),
            )
        }
    }
}
