package com.trianglz_task_ahmedhussin.feature.movies.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.dropUnlessResumed
import com.trianglz_task_ahmedhussin.R
import com.trianglz_task_ahmedhussin.core.designsystem.preview.ThemePreviews
import com.trianglz_task_ahmedhussin.core.designsystem.theme.MovieAppTheme
import com.trianglz_task_ahmedhussin.core.designsystem.theme.Spacing

/**
 * Movie list screen (start destination).
 *
 * Navigation-shell stub from T0.9: the real grid, search bar and states replace the body in
 * T1.5/T3.4. The sample button exists only to exercise navigation until then.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviesScreen(onMovieClick: (movieId: Int) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.placeholder_movies_body),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Button(onClick = dropUnlessResumed { onMovieClick(SAMPLE_MOVIE_ID) }) {
                Text(stringResource(R.string.placeholder_open_sample_movie))
            }
        }
    }
}

/** Toy Story (TMDb id 862), rated G, used only by the stub above. */
private const val SAMPLE_MOVIE_ID = 862

@ThemePreviews
@Composable
private fun MoviesScreenPreview() {
    MovieAppTheme { MoviesScreen(onMovieClick = {}) }
}
