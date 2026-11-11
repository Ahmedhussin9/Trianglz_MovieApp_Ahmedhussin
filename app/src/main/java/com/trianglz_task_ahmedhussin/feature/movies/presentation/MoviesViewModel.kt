package com.trianglz_task_ahmedhussin.feature.movies.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.trianglz_task_ahmedhussin.feature.movies.domain.usecase.GetMoviesUseCase
import com.trianglz_task_ahmedhussin.feature.movies.presentation.model.MovieUi
import com.trianglz_task_ahmedhussin.feature.movies.presentation.model.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * State holder of the Movies screen (MVI, see `MoviesContract.kt`).
 *
 * Retry/refresh of the list are not actions here: Paging owns that state, so the screen calls
 * `LazyPagingItems.retry()` directly and this ViewModel stays free of paging bookkeeping.
 */
@HiltViewModel
class MoviesViewModel @Inject constructor(
    getMovies: GetMoviesUseCase,
) : ViewModel() {

    /** Cached in [viewModelScope] so the list and scroll position survive rotation. */
    val movies: Flow<PagingData<MovieUi>> = getMovies()
        .map { pagingData -> pagingData.map { it.toUi() } }
        .cachedIn(viewModelScope)

    private val _state = MutableStateFlow(MoviesUiState())
    val state: StateFlow<MoviesUiState> = _state.asStateFlow()

    private val _effects = Channel<MoviesEffect>(Channel.BUFFERED)

    /** One-time events; each is delivered to exactly one collector (no replay on rotation). */
    val effects: Flow<MoviesEffect> = _effects.receiveAsFlow()

    fun onAction(action: MoviesUiAction) {
        Timber.d("Action: %s", action)
        when (action) {
            is MoviesUiAction.MovieClicked -> viewModelScope.launch {
                _effects.send(MoviesEffect.NavigateToDetails(action.movieId))
            }
            MoviesUiAction.DismissCachedNotice -> _state.update { it.copy(isCachedNoticeDismissed = true) }
        }
    }
}
