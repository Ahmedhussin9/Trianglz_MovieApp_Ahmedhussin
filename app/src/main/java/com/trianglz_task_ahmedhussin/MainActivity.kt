package com.trianglz_task_ahmedhussin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.trianglz_task_ahmedhussin.core.designsystem.theme.MovieAppTheme
import com.trianglz_task_ahmedhussin.core.navigation.AppNavigation
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single activity hosting the Compose UI. Drawing edge-to-edge is required from targetSdk 35;
 * each screen's Scaffold/TopAppBar applies the system-bar insets.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MovieAppTheme {
                AppNavigation()
            }
        }
    }
}
