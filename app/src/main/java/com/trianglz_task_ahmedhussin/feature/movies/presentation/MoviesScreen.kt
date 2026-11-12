package com.trianglz_task_ahmedhussin.feature.movies.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fitInside
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.WindowInsetsRulers
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
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
import com.trianglz_task_ahmedhussin.feature.movies.presentation.component.MovieSearchBar
import com.trianglz_task_ahmedhussin.feature.movies.presentation.component.ScrollToTopButton
import com.trianglz_task_ahmedhussin.feature.movies.presentation.model.MovieUi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * Stateful entry of the Movies screen: connects the [MoviesViewModel] to the stateless
 * [MoviesScreen] and turns one-time effects into navigation.
 */
@Composable
fun MoviesRoute(
    onMovieClick: (movieId: Int) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: MoviesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val movies = viewModel.movies.collectAsLazyPagingItems()
    val searchResults = viewModel.searchResults.collectAsLazyPagingItems()
    val currentOnMovieClick by rememberUpdatedState(onMovieClick)
    val currentOnSettingsClick by rememberUpdatedState(onSettingsClick)
    val lifecycleOwner = LocalLifecycleOwner.current

    // Cached titles and synopses are in the language they were downloaded in. After a language
    // switch in Settings, both lists are fetched again so they match the new language (Q14).
    val language = LocalConfiguration.current.locales[0].language
    var listLanguage by rememberSaveable { mutableStateOf(language) }
    LaunchedEffect(language) {
        if (language != listLanguage) {
            listLanguage = language
            movies.refresh()
            searchResults.refresh()
        }
    }

    // Effects are collected only while the screen is visible, so a navigation event can
    // never fire while the app is in the background.
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is MoviesEffect.NavigateToDetails -> currentOnMovieClick(effect.movieId)
                    MoviesEffect.NavigateToSettings -> currentOnSettingsClick()
                }
            }
        }
    }

    MoviesScreen(state = state, movies = movies, searchResults = searchResults, onAction = viewModel::onAction)
}

/**
 * Stateless Movies screen (R1–R4, R11–R13): a pinned search bar over either the popular list or,
 * while searching, the search results. Both are paged grids with the same four states.
 * Retry calls `LazyPagingItems.retry()` directly because Paging owns the load state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviesScreen(
    state: MoviesUiState,
    movies: LazyPagingItems<MovieUi>,
    searchResults: LazyPagingItems<MovieUi>,
    onAction: (MoviesUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    // One grid state per list, so clearing a search returns to the same place in the popular list.
    val listGridState = rememberLazyGridState()
    val searchGridState = rememberLazyGridState()
    val items = if (state.isSearching) searchResults else movies
    val gridState = if (state.isSearching) searchGridState else listGridState
    val status = items.loadState.toListStatus(items.itemCount)
    val scope = rememberCoroutineScope()
    val layoutDirection = LocalLayoutDirection.current
    // derivedStateOf: recomposes only when the threshold is crossed, not on every scrolled pixel.
    val isDeepInList by remember(gridState) { derivedStateOf { gridState.firstVisibleItemIndex > SCROLL_TO_TOP_THRESHOLD } }

    HideKeyboardWhileScrolling(gridState)
    ScrollResultsToTopOnNewSearch(searchResults, searchGridState)
    // Each list has its own scroll position, but the app bar's collapse is shared. Switching lists
    // re-shows it, so it is never hidden above a list that is already at the top.
    LaunchedEffect(state.isSearching) { scrollBehavior.state.heightOffset = 0f }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    actions = {
                        IconButton(onClick = { onAction(MoviesUiAction.SettingsClicked) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_settings),
                                contentDescription = stringResource(R.string.action_settings),
                            )
                        }
                    },
                    // The search bar below sits on the surface colour, so the bar keeps it when scrolled.
                    colors = TopAppBarDefaults.topAppBarColors(scrolledContainerColor = MaterialTheme.colorScheme.surface),
                    scrollBehavior = scrollBehavior,
                )
                // Pinned: when the app bar collapses on scroll, the search bar stays (R11).
                MovieSearchBar(
                    query = state.query,
                    onQueryChange = { onAction(MoviesUiAction.QueryChanged(it)) },
                    onClear = { onAction(MoviesUiAction.ClearQuery) },
                    modifier = Modifier
                        // In landscape the navigation bar or a display cutout sits at the side.
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                        .padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.sm),
                )
            }
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
        // Crossfade only when switching between list and search, or between kinds of state (A4).
        AnimatedContent(
            targetState = ScreenContent(isSearching = state.isSearching, status = status),
            contentKey = { it.isSearching to it.status::class },
            label = "moviesContent",
            modifier = Modifier
                .fillMaxSize()
                // Side insets too: in landscape the navigation bar is at the start or end edge.
                .padding(
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    top = innerPadding.calculateTopPadding(),
                    end = innerPadding.calculateEndPadding(layoutDirection),
                )
                // Results stay above the keyboard while typing (edge-to-edge skill: IME insets).
                .fitInside(WindowInsetsRulers.Ime.current),
        ) { target ->
            when (val targetStatus = target.status) {
                ListStatus.Loading -> MovieGridSkeleton(contentPadding = gridPadding)
                ListStatus.Empty -> if (target.isSearching) {
                    EmptyView(
                        title = stringResource(R.string.search_empty_title, state.query.trim()),
                        message = stringResource(R.string.search_empty_message),
                    )
                } else {
                    EmptyView(
                        title = stringResource(R.string.movies_empty_title),
                        message = stringResource(R.string.movies_empty_message),
                    )
                }
                is ListStatus.Error -> ErrorView(error = targetStatus.error, onRetry = items::retry)
                is ListStatus.Content -> Column(Modifier.fillMaxSize()) {
                    if (targetStatus.isRefreshing) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                    // Search results are not cached, so the "showing saved movies" notice is list-only.
                    AnimatedVisibility(
                        visible = !target.isSearching && targetStatus.refreshError != null && !state.isCachedNoticeDismissed,
                    ) {
                        CachedDataNotice(
                            // During the exit animation the error is already cleared.
                            error = targetStatus.refreshError ?: DomainError.Unknown,
                            onRetry = movies::retry,
                            onDismiss = { onAction(MoviesUiAction.DismissCachedNotice) },
                            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        MovieGrid(
                            movies = items,
                            footerStatus = items.loadState.toFooterStatus(),
                            onMovieClick = { onAction(MoviesUiAction.MovieClicked(it)) },
                            onRetry = items::retry,
                            contentPadding = gridPadding,
                            state = gridState,
                            animateItems = target.isSearching,
                        )
                    }
                }
            }
        }
    }
}

/** What the content area shows: which list, and in which state. */
private data class ScreenContent(val isSearching: Boolean, val status: ListStatus)

/** Scrolling the grid means the user is browsing, not typing, so the keyboard gets out of the way. */
@Composable
private fun HideKeyboardWhileScrolling(gridState: LazyGridState) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.isScrollInProgress }
            .filter { it }
            .collect {
                keyboard?.hide()
                focusManager.clearFocus()
            }
    }
}

/** A new query starts its results from the top instead of where the previous results were scrolled. */
@Composable
private fun ScrollResultsToTopOnNewSearch(searchResults: LazyPagingItems<MovieUi>, searchGridState: LazyGridState) {
    val isLoadingNewResults = searchResults.loadState.refresh is LoadState.Loading
    LaunchedEffect(isLoadingNewResults) {
        if (isLoadingNewResults) searchGridState.scrollToItem(0)
    }
}

/** The button appears after about four rows on a phone (two columns). */
private const val SCROLL_TO_TOP_THRESHOLD = 8
private const val SCROLL_TO_TOP_JUMP_FROM = 30
private const val SCROLL_TO_TOP_JUMP_TO = 12

private val PreviewMovies = listOf(
    MovieUi(1, "Project Hail Mary", null, "2026", "8.6"),
    MovieUi(2, "The Dark Knight", null, "2008", "8.5"),
    MovieUi(3, "Coco", null, "2017", "8.2"),
    MovieUi(4, "A Film Without a Poster", null, null, null),
)

@ThemePreviews
@Composable
private fun MoviesScreenPreview() {
    val movies = flowOf(PagingData.from(PreviewMovies)).collectAsLazyPagingItems()
    val searchResults = flowOf(PagingData.empty<MovieUi>()).collectAsLazyPagingItems()
    MovieAppTheme { MoviesScreen(state = MoviesUiState(), movies = movies, searchResults = searchResults, onAction = {}) }
}

@ThemePreviews
@Composable
private fun MoviesScreenSearchPreview() {
    val movies = flowOf(PagingData.from(PreviewMovies)).collectAsLazyPagingItems()
    val searchResults = flowOf(PagingData.from(PreviewMovies.take(1))).collectAsLazyPagingItems()
    MovieAppTheme {
        MoviesScreen(
            state = MoviesUiState(query = "Project", isSearching = true),
            movies = movies,
            searchResults = searchResults,
            onAction = {},
        )
    }
}
