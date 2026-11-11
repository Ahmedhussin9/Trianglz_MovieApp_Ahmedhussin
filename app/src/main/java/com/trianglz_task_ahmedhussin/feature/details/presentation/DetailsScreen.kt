package com.trianglz_task_ahmedhussin.feature.details.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.trianglz_task_ahmedhussin.R
import com.trianglz_task_ahmedhussin.core.designsystem.component.ErrorView
import com.trianglz_task_ahmedhussin.core.designsystem.preview.ThemePreviews
import com.trianglz_task_ahmedhussin.core.designsystem.theme.MovieAppTheme
import com.trianglz_task_ahmedhussin.core.designsystem.theme.Spacing
import com.trianglz_task_ahmedhussin.core.domain.error.DomainError
import com.trianglz_task_ahmedhussin.core.ui.error.messageRes
import com.trianglz_task_ahmedhussin.feature.details.domain.model.Genre
import com.trianglz_task_ahmedhussin.feature.details.domain.model.ReleaseStatus
import com.trianglz_task_ahmedhussin.feature.details.presentation.component.BackdropHeight
import com.trianglz_task_ahmedhussin.feature.details.presentation.component.DetailsHeader
import com.trianglz_task_ahmedhussin.feature.details.presentation.component.DetailsTopBar
import com.trianglz_task_ahmedhussin.feature.details.presentation.component.GenreChips
import com.trianglz_task_ahmedhussin.feature.details.presentation.component.InfoRow
import com.trianglz_task_ahmedhussin.feature.details.presentation.component.RatingSection
import com.trianglz_task_ahmedhussin.feature.details.presentation.component.Synopsis
import com.trianglz_task_ahmedhussin.feature.details.presentation.model.MovieDetailsUi
import kotlinx.coroutines.launch

/**
 * Stateful entry of the details screen: creates the [DetailsViewModel] for [movieId] (assisted
 * injection, scoped to this navigation entry) and turns its effects into navigation and snackbars.
 */
@Composable
fun MovieDetailsRoute(
    movieId: Int,
    onBack: () -> Unit,
) {
    val viewModel = hiltViewModel<DetailsViewModel, DetailsViewModel.Factory>(
        creationCallback = { factory -> factory.create(movieId) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val currentOnBack by rememberUpdatedState(onBack)
    val lifecycleOwner = LocalLifecycleOwner.current

    // Effects are collected only while the screen is visible, so nothing fires in the background.
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    DetailsEffect.NavigateBack -> currentOnBack()
                    // Launched so a waiting snackbar never blocks the next effect (for example Back).
                    is DetailsEffect.ShowRefreshError -> launch {
                        val result = snackbarHostState.showSnackbar(
                            message = resources.getString(effect.error.messageRes()),
                            actionLabel = resources.getString(R.string.action_retry),
                        )
                        if (result == SnackbarResult.ActionPerformed) viewModel.onAction(DetailsUiAction.Retry)
                    }
                }
            }
        }
    }

    DetailsScreen(state = state, snackbarHostState = snackbarHostState, onAction = viewModel::onAction)
}

/**
 * Stateless details screen (R6): renders [state] and reports user actions. The top bar is drawn
 * over the content so the backdrop can extend behind the status bar (edge-to-edge).
 */
@Composable
fun DetailsScreen(
    state: DetailsUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (DetailsUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState, Modifier.navigationBarsPadding()) },
        // Insets are handled per element: the hero goes behind the status bar on purpose.
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Crossfade only when the kind of state changes, not when cached details are refreshed (A4).
            AnimatedContent(
                targetState = state.status,
                contentKey = { it::class },
                label = "detailsStatus",
            ) { status ->
                when (status) {
                    DetailsStatus.Loading -> DetailsSkeleton()
                    is DetailsStatus.Error -> ErrorView(error = status.error, onRetry = { onAction(DetailsUiAction.Retry) })
                    is DetailsStatus.Content -> DetailsContent(
                        details = status.details,
                        isSynopsisExpanded = state.isSynopsisExpanded,
                        scrollState = scrollState,
                        onToggleSynopsis = { onAction(DetailsUiAction.ToggleSynopsis) },
                    )
                }
            }
            DetailsTopBar(
                title = (state.status as? DetailsStatus.Content)?.details?.title,
                scrollState = scrollState,
                isRefreshing = state.isRefreshing,
                onBack = { onAction(DetailsUiAction.Back) },
            )
        }
    }
}

@Composable
private fun DetailsContent(
    details: MovieDetailsUi,
    isSynopsisExpanded: Boolean,
    scrollState: ScrollState,
    onToggleSynopsis: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
    ) {
        DetailsHeader(
            title = details.title,
            tagline = details.tagline,
            posterUrl = details.posterUrl,
            backdropUrl = details.backdropUrl,
            scrollState = scrollState,
        )
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            InfoRow(
                releaseDateText = details.releaseDateText,
                runtimeMinutes = details.runtimeMinutes,
                languageText = details.languageText,
                status = details.status,
            )
            GenreChips(genres = details.genres)
            RatingSection(
                ratingFraction = details.ratingFraction,
                ratingText = details.ratingText,
                voteCount = details.voteCount,
                voteCountText = details.voteCountText,
            )
            Synopsis(text = details.overview, isExpanded = isSynopsisExpanded, onToggle = onToggleSynopsis)
        }
        Spacer(Modifier.height(Spacing.xl))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

/** Loading state shaped like the content: a backdrop block and a few text lines. */
@Composable
private fun DetailsSkeleton() {
    val description = stringResource(R.string.loading)
    val placeholder = MaterialTheme.colorScheme.surfaceContainerHigh
    Column(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = description },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BackdropHeight)
                .background(placeholder),
        )
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            SKELETON_LINE_WIDTHS.forEach { fraction ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(16.dp)
                        .background(placeholder, MaterialTheme.shapes.small),
                )
            }
        }
    }
}

private val SKELETON_LINE_WIDTHS = listOf(0.7f, 0.5f, 0.9f, 0.85f, 0.6f)

private val PreviewDetails = MovieDetailsUi(
    id = 862,
    title = "Toy Story",
    tagline = "Hang on for the comedy that goes to infinity and beyond!",
    overview = "Led by Woody, Andy's toys live happily in his room until Andy's birthday brings Buzz Lightyear onto " +
        "the scene. Afraid of losing his place in Andy's heart, Woody plots against Buzz. But when circumstances " +
        "separate Buzz and Woody from their owner, the duo eventually learns to put aside their differences.",
    posterUrl = null,
    backdropUrl = null,
    releaseDateText = "Oct 30, 1995",
    runtimeMinutes = 81,
    ratingFraction = 0.797f,
    ratingText = "8.0",
    voteCount = 18924,
    voteCountText = "18,924",
    genres = listOf(Genre(16, "Animation"), Genre(12, "Adventure"), Genre(10751, "Family"), Genre(35, "Comedy")),
    languageText = "English",
    status = ReleaseStatus.Released,
)

@ThemePreviews
@Composable
private fun DetailsScreenContentPreview() {
    MovieAppTheme {
        DetailsScreen(
            state = DetailsUiState(status = DetailsStatus.Content(PreviewDetails)),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
        )
    }
}

@ThemePreviews
@Composable
private fun DetailsScreenLoadingPreview() {
    MovieAppTheme {
        DetailsScreen(state = DetailsUiState(), snackbarHostState = remember { SnackbarHostState() }, onAction = {})
    }
}

@ThemePreviews
@Composable
private fun DetailsScreenErrorPreview() {
    MovieAppTheme {
        DetailsScreen(
            state = DetailsUiState(status = DetailsStatus.Error(DomainError.NoInternet)),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
        )
    }
}
