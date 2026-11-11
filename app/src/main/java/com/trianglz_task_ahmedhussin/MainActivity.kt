package com.trianglz_task_ahmedhussin

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.trianglz_task_ahmedhussin.core.designsystem.theme.MovieAppTheme
import com.trianglz_task_ahmedhussin.core.navigation.AppNavigation
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single activity hosting the Compose UI. Drawing edge-to-edge is required from targetSdk 35;
 * each screen's Scaffold/TopAppBar applies the system-bar insets.
 *
 * An [AppCompatActivity] (still a ComponentActivity) because the in-app language and theme use
 * AppCompat's per-app locales and night mode, which apply to AppCompat activities.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
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
