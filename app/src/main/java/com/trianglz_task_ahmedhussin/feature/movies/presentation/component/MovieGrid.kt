package com.trianglz_task_ahmedhussin.feature.movies.presentation.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.trianglz_task_ahmedhussin.R
import com.trianglz_task_ahmedhussin.core.designsystem.component.RetryButton
import com.trianglz_task_ahmedhussin.core.designsystem.theme.Spacing
import com.trianglz_task_ahmedhussin.core.ui.error.messageRes
import com.trianglz_task_ahmedhussin.feature.movies.presentation.FooterStatus
import com.trianglz_task_ahmedhussin.feature.movies.presentation.model.MovieUi

/** Cards are at least this wide: 2 columns on phones, more on tablets and in landscape. */
private val MinCardWidth = 150.dp
private const val SKELETON_CARD_COUNT = 8

/**
 * The paginated poster grid. Scrolling near the end makes Paging load the next page; the
 * footer shows that page's progress or error with Retry (R3, R4).
 */
@Composable
fun MovieGrid(
    movies: LazyPagingItems<MovieUi>,
    footerStatus: FooterStatus,
    onMovieClick: (Int) -> Unit,
    onRetry: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    state: LazyGridState,
    animateItems: Boolean = false,
) {
    // Changes only when scrolling starts or stops, so reading it here costs nothing per frame.
    val isScrolling = state.isScrollInProgress
    LazyVerticalGrid(
        columns = GridCells.Adaptive(MinCardWidth),
        state = state,
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        items(
            count = movies.itemCount,
            key = movies.itemKey { it.id },
            contentType = movies.itemContentType { "movie" },
        ) { index ->
            val movie = movies[index]
            // Only search results animate (A3): on the popular list every page cached by the
            // RemoteMediator reloads the list from Room, and placement animations would make
            // cards slide while scrolling.
            if (movie != null) {
                MovieCard(
                    movie = movie,
                    onClick = { onMovieClick(movie.id) },
                    pressFeedbackEnabled = !isScrolling,
                    modifier = if (animateItems) Modifier.animateItem() else Modifier,
                )
            } else {
                SkeletonCard()
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }, contentType = "footer") {
            PagingFooter(status = footerStatus, onRetry = onRetry)
        }
    }
}

/** Bottom of the list while paging (R4); switches with a crossfade (animation A9). */
@Composable
private fun PagingFooter(status: FooterStatus, onRetry: () -> Unit) {
    AnimatedContent(targetState = status, label = "pagingFooter") { target ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            when (target) {
                FooterStatus.None -> Unit
                FooterStatus.Loading -> {
                    val description = stringResource(R.string.loading_more)
                    CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = description })
                }
                is FooterStatus.Error -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Text(
                        text = stringResource(target.error.messageRes()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    RetryButton(onClick = onRetry)
                }
            }
        }
    }
}

/** Full-screen Loading state: card-shaped placeholders hint at the content to come. */
@Composable
fun MovieGridSkeleton(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.loading)
    LazyVerticalGrid(
        columns = GridCells.Adaptive(MinCardWidth),
        modifier = modifier.semantics { contentDescription = description },
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        userScrollEnabled = false,
    ) {
        items(SKELETON_CARD_COUNT) { SkeletonCard() }
    }
}

@Composable
private fun SkeletonCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.medium),
    )
}
