package com.trianglz_task_ahmedhussin.feature.details.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trianglz_task_ahmedhussin.core.common.locale.LocaleProvider
import com.trianglz_task_ahmedhussin.core.domain.result.AppResult
import com.trianglz_task_ahmedhussin.feature.details.domain.usecase.GetMovieDetailsUseCase
import com.trianglz_task_ahmedhussin.feature.details.presentation.model.toUi
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * State holder of the Details screen (MVI, see `DetailsContract.kt`).
 *
 * The movie id comes from the navigation key through assisted injection. Each `DetailsRoute`
 * entry gets its own instance (Navigation 3's ViewModel store decorator), so the id never changes.
 *
 * Loading follows the repository contract: cached details (if any) arrive first, then the
 * network outcome, then the flow completes. Completion is what ends [DetailsUiState.isRefreshing].
 */
@HiltViewModel(assistedFactory = DetailsViewModel.Factory::class)
class DetailsViewModel @AssistedInject constructor(
    @Assisted private val movieId: Int,
    private val getMovieDetails: GetMovieDetailsUseCase,
    private val localeProvider: LocaleProvider,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(movieId: Int): DetailsViewModel
    }

    private val _state = MutableStateFlow(DetailsUiState())
    val state: StateFlow<DetailsUiState> = _state.asStateFlow()

    private val _effects = Channel<DetailsEffect>(Channel.BUFFERED)

    /** One-time events; each is delivered to exactly one collector (no replay on rotation). */
    val effects: Flow<DetailsEffect> = _effects.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun onAction(action: DetailsUiAction) {
        Timber.d("Action: %s", action)
        when (action) {
            DetailsUiAction.Retry -> load()
            DetailsUiAction.Back -> viewModelScope.launch { _effects.send(DetailsEffect.NavigateBack) }
            DetailsUiAction.ToggleSynopsis -> _state.update { it.copy(isSynopsisExpanded = !it.isSynopsisExpanded) }
        }
    }

    /**
     * Ignored while a load is running: a double-tapped Retry must not start a second request,
     * and the running one already ends in a fresh state.
     */
    private fun load() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            Timber.d("Loading details of movie %d", movieId)
            // Content already on screen stays there during a retry; otherwise show Loading.
            _state.update { state ->
                if (state.status is DetailsStatus.Content) state.copy(isRefreshing = true) else state.copy(status = DetailsStatus.Loading)
            }
            getMovieDetails(movieId).collect { result ->
                when (result) {
                    is AppResult.Success -> _state.update {
                        it.copy(status = DetailsStatus.Content(result.data.toUi(localeProvider.current())), isRefreshing = true)
                    }
                    is AppResult.Failure -> if (_state.value.status is DetailsStatus.Content) {
                        _effects.send(DetailsEffect.ShowRefreshError(result.error))
                    } else {
                        _state.update { it.copy(status = DetailsStatus.Error(result.error)) }
                    }
                }
            }
            _state.update { it.copy(isRefreshing = false) }
            Timber.d("Finished loading details of movie %d", movieId)
        }
    }
}
