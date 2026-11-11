package com.trianglz_task_ahmedhussin.feature.movies.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.trianglz_task_ahmedhussin.feature.movies.domain.usecase.GetMoviesUseCase
import com.trianglz_task_ahmedhussin.feature.movies.domain.usecase.SearchMoviesUseCase
import com.trianglz_task_ahmedhussin.feature.movies.domain.usecase.SearchQuery
import com.trianglz_task_ahmedhussin.feature.movies.domain.usecase.SearchQueryValidator
import com.trianglz_task_ahmedhussin.feature.movies.presentation.model.MovieUi
import com.trianglz_task_ahmedhussin.feature.movies.presentation.model.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * State holder of the Movies screen (MVI, see `MoviesContract.kt`): the popular list and,
 * while the user searches, the search results (R11–R13).
 *
 * Retry/refresh of either list are not actions here: Paging owns that state, so the screen calls
 * `LazyPagingItems.retry()` directly and this ViewModel stays free of paging bookkeeping.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class MoviesViewModel @Inject constructor(
    getMovies: GetMoviesUseCase,
    private val searchMovies: SearchMoviesUseCase,
) : ViewModel() {

    /** Cached in [viewModelScope] so the list and scroll position survive rotation. */
    val movies: Flow<PagingData<MovieUi>> = getMovies()
        .map { pagingData -> pagingData.map { it.toUi() } }
        .cachedIn(viewModelScope)

    private val _state = MutableStateFlow(MoviesUiState())
    val state: StateFlow<MoviesUiState> = _state.asStateFlow()

    /**
     * The query that is actually searched (R13):
     * - waits [SEARCH_DEBOUNCE_MS] after the last keystroke, so typing "coco" fast is one request;
     * - clearing (an idle query) is not delayed, so the popular list comes back at once;
     * - compares validated queries, so "coco" and "coco " do not search twice.
     */
    private val activeQuery: StateFlow<SearchQuery> = _state
        .map { it.query }
        .distinctUntilChanged()
        .debounce { text -> if (SearchQueryValidator.validate(text) is SearchQuery.Idle) 0L else SEARCH_DEBOUNCE_MS }
        .map(SearchQueryValidator::validate)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, SearchQuery.Idle)

    /**
     * Results for [activeQuery]. `flatMapLatest` cancels the previous search as soon as the query
     * changes, so an old, slower response can never replace newer results. Cached so results and
     * scroll position survive rotation.
     */
    val searchResults: Flow<PagingData<MovieUi>> = activeQuery
        .flatMapLatest { query ->
            when (query) {
                SearchQuery.Idle -> searchMovies("")
                is SearchQuery.Valid -> {
                    Timber.d("Search started (%d characters)", query.text.length)
                    searchMovies(query.text)
                }
            }
        }
        .map { pagingData -> pagingData.map { it.toUi() } }
        .cachedIn(viewModelScope)

    private val _effects = Channel<MoviesEffect>(Channel.BUFFERED)

    /** One-time events; each is delivered to exactly one collector (no replay on rotation). */
    val effects: Flow<MoviesEffect> = _effects.receiveAsFlow()

    init {
        activeQuery
            .onEach { query -> _state.update { it.copy(isSearching = query is SearchQuery.Valid) } }
            .launchIn(viewModelScope)
    }

    fun onAction(action: MoviesUiAction) {
        Timber.d("Action: %s", action)
        when (action) {
            is MoviesUiAction.MovieClicked -> viewModelScope.launch {
                _effects.send(MoviesEffect.NavigateToDetails(action.movieId))
            }
            MoviesUiAction.DismissCachedNotice -> _state.update { it.copy(isCachedNoticeDismissed = true) }
            MoviesUiAction.SettingsClicked -> viewModelScope.launch { _effects.send(MoviesEffect.NavigateToSettings) }
            // The field itself stops at the validator's cap, so what is shown is what is searched.
            is MoviesUiAction.QueryChanged -> _state.update { it.copy(query = action.text.take(SearchQueryValidator.MAX_LENGTH)) }
            MoviesUiAction.ClearQuery -> _state.update { it.copy(query = "") }
        }
    }

    private companion object {
        /** Long enough to skip the keystrokes of a typed word, short enough to feel live. */
        const val SEARCH_DEBOUNCE_MS = 350L
    }
}
