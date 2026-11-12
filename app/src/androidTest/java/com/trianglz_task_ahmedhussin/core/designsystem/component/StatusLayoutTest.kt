package com.trianglz_task_ahmedhussin.core.designsystem.component

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.trianglz_task_ahmedhussin.R
import com.trianglz_task_ahmedhussin.core.designsystem.theme.MovieAppTheme
import com.trianglz_task_ahmedhussin.core.domain.error.DomainError
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Regression test from the T4.1 review: in landscape with large text the error state did not fit,
 * and the Retry button was cut off with no way to reach it.
 */
class StatusLayoutTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun retryStaysReachableWhenTheErrorDoesNotFit() {
        var retries = 0
        composeRule.setContent {
            MovieAppTheme {
                // Far smaller than the icon, a multi-line message and the button need.
                Box(Modifier.size(width = 320.dp, height = 160.dp)) {
                    ErrorView(error = DomainError.Unauthorized, onRetry = { retries++ })
                }
            }
        }

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.action_retry))
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        assertEquals(1, retries)
    }
}
