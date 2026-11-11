package com.trianglz_task_ahmedhussin.feature.movies.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.trianglz_task_ahmedhussin.R
import com.trianglz_task_ahmedhussin.core.designsystem.component.EmptyView
import com.trianglz_task_ahmedhussin.core.designsystem.component.ErrorView
import com.trianglz_task_ahmedhussin.core.designsystem.preview.ThemePreviews
import com.trianglz_task_ahmedhussin.core.designsystem.theme.MovieAppTheme
import com.trianglz_task_ahmedhussin.core.designsystem.theme.Spacing
import com.trianglz_task_ahmedhussin.core.domain.error.DomainError
import com.trianglz_task_ahmedhussin.feature.movies.presentation.component.CachedDataNotice
import com.trianglz_task_ahmedhussin.feature.movies.presentation.component.MovieGrid
import com.trianglz_task_ahmedhussin.feature.movies.presentation.component.MovieGridSkeleton
import com.trianglz_task_ahmedhussin.feature.movies.presentation.component.ScrollToTopButton
import com.trianglz_task_ahmedhussin.feature.movies.presentation.model.MovieUi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * Stateful entry of the Movies screen: connects the [MoviesViewModel] to the stateless
 * [MoviesScreen] and turns one-time effects into navigation.
 */
@Composable
fun MoviesRoute(
    onMovieClick: (movieId: Int) -> Unit,
    viewModel: MoviesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val movies = viewModel.movies.collectAsLazyPagingItems()
    val currentOnMovieClick by rememberUpdatedState(onMovieClick)
    val lifecycleOwner = LocalLifecycleOwner.current

    // Effects are collected only while the screen is visible, so a navigation event can
    // never fire while the app is in the background.
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is MoviesEffect.NavigateToDetails -> currentOnMovieClick(effect.movieId)
                }
            }
        }
    }

    MoviesScreen(state = state, movies = movies, onAction = viewModel::onAction)
}

/**
 * Stateless Movies screen: renders the state it is given and reports user actions.
 * Retry calls `LazyPagingItems.retry()` directly because Paging owns the load state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviesScreen(
    state: MoviesUiState,
    movies: LazyPagingItems<MovieUi>,
    onAction: (MoviesUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val gridState = rememberLazyGridState()
    val status = movies.loadState.toListStatus(movies.itemCount)
    val scope = rememberCoroutineScope()
    // derivedStateOf: recomposes only when the threshold is crossed, not on every scrolled pixel.
    val isDeepInList by remember { derivedStateOf { gridState.firstVisibleItemIndex > SCROLL_TO_TOP_THRESHOLD } }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ScrollToTopButton(
                visible = isDeepInList && status is ListStatus.Content,
                onClick = {
                    scope.launch {
                        // From far down, jump close to the top first so the animation stays short.
                        if (gridState.firstVisibleItemIndex > SCROLL_TO_TOP_JUMP_FROM) {
                            gridState.scrollToItem(SCROLL_TO_TOP_JUMP_TO)
                        }
                        gridState.animateScrollToItem(0)
                        // Programmatic scrolls bypass nested scrolling, so re-show the app bar.
                        scrollBehavior.state.heightOffset = 0f
                    }
                },
            )
        },
    ) { innerPadding ->
        val gridPadding = PaddingValues(
            start = Spacing.lg,
            end = Spacing.lg,
            top = Spacing.sm,
            bottom = innerPadding.calculateBottomPadding() + Spacing.lg,
        )
        // Crossfade only when the kind of state changes, not on every Content update (A4).
        AnimatedContent(
            targetState = status,
            contentKey = { it::class },
            label = "moviesStatus",
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) { target ->
            when (target) {
                ListStatus.Loading -> MovieGridSkeleton(contentPadding = gridPadding)
                ListStatus.Empty -> EmptyView(
                    title = stringResource(R.string.movies_empty_title),
                    message = stringResource(R.string.movies_empty_message),
                )
                is ListStatus.Error -> ErrorView(error = target.error, onRetry = movies::retry)
                is ListStatus.Content -> Column(Modifier.fillMaxSize()) {
                    if (target.isRefreshing) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                    AnimatedVisibility(visible = target.refreshError != null && !state.isCachedNoticeDismissed) {
                        CachedDataNotice(
                            // During the exit animation the error is already cleared.
                            error = target.refreshError ?: DomainError.Unknown,
                            onRetry = movies::retry,
                            onDismiss = { onAction(MoviesUiAction.DismissCachedNotice) },
                            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        MovieGrid(
                            movies = movies,
                            footerStatus = movies.loadState.toFooterStatus(),
                            onMovieClick = { onAction(MoviesUiAction.MovieClicked(it)) },
                            onRetry = movies::retry,
                            contentPadding = gridPadding,
                            state = gridState,
                        )
                    }
                }
            }
        }
    }
}

/** The button appears after about four rows on a phone (two columns). */
private const val SCROLL_TO_TOP_THRESHOLD = 8
private const val SCROLL_TO_TOP_JUMP_FROM = 30
private const val SCROLL_TO_TOP_JUMP_TO = 12

@ThemePreviews
@Composable
private fun MoviesScreenPreview() {
    val movies = flowOf(
        PagingData.from(
            listOf(
                MovieUi(1, "Project Hail Mary", null, "2026", "8.6"),
                MovieUi(2, "The Dark Knight", null, "2008", "8.5"),
                MovieUi(3, "Coco", null, "2017", "8.2"),
                MovieUi(4, "A Film Without a Poster", null, null, null),
            ),
        ),
    ).collectAsLazyPagingItems()
    MovieAppTheme { MoviesScreen(state = MoviesUiState(), movies = movies, onAction = {}) }
}
