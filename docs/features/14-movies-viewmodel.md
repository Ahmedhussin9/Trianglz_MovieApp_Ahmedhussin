# 14: Movies list, ViewModel and screen states (T1.4)

## 1. What was built
The presentation logic of the Movies screen, using MVI (R17, R21, R29):
- **The MVI contract** (`MoviesContract.kt`):
  - `MoviesUiState`, the immutable model;
  - `MoviesUiAction`, the intents (`MovieClicked`, `DismissCachedNotice`);
  - `MoviesEffect`, the one-time events (`NavigateToDetails`).
- **`MoviesViewModel`**:
  - `movies: Flow<PagingData<MovieUi>>`, cached in `viewModelScope`;
  - `state: StateFlow<MoviesUiState>`;
  - `effects`, delivered once through a `Channel`;
  - a single `onAction()`.
- **`MovieUi`**: a display model with formatting done ahead of time. The year becomes text, and the rating gets one decimal in the user's locale, or `null` for unrated movies.
- **`ListStatus` / `FooterStatus`**: pure mappings from Paging's load states to `Loading`, `Empty`, `Error(DomainError)` or `Content(isRefreshing, refreshError)`, plus the footer state at the bottom of the list (R4).

## 2. Files created or changed

| Layer | File | Change |
|-------|------|--------|
| Presentation | `feature/movies/presentation/MoviesContract.kt` | New. State, actions and effects. |
| Presentation | `feature/movies/presentation/MoviesViewModel.kt` | New. `@HiltViewModel`. |
| Presentation | `feature/movies/presentation/ListStatus.kt` | New. `toListStatus(itemCount)`, `toFooterStatus()`. |
| Presentation | `feature/movies/presentation/model/MovieUi.kt` | New. `Movie.toUi(locale)`. |
| Tests | `ListStatusTest` (9), `MovieUiTest` (5), `MoviesViewModelTest` (3) | New. |

## 3. Data flow walkthrough

```
GetMoviesUseCase() ─► Flow<PagingData<Movie>> ─map toUi─► cachedIn(viewModelScope) ─► movies
                                                                                       │ collectAsLazyPagingItems (T1.5)
Screen ── onAction(MovieClicked(id)) ─► MoviesViewModel ── effects ─► NavigateToDetails(id) ─► back stack
Screen ── onAction(DismissCachedNotice) ─► state.isCachedNoticeDismissed = true
LazyPagingItems.loadState ─► toListStatus(itemCount) ─► when { Loading / Empty / Error / Content }
```

**Rules of `toListStatus`.** With a RemoteMediator, the database read (`source`) and the network refresh (`mediator`) load independently:

| Items on screen? | Load states | ListStatus |
|---|---|---|
| yes | mediator refresh running | `Content(isRefreshing = true)` |
| yes | mediator refresh failed | `Content(refreshError = NoInternet)`, which drives the "Showing saved movies" notice (R9) |
| no | source or mediator loading | `Loading` (never a premature Empty while the network is still working) |
| no | refresh failed | `Error(domainError)`: full screen with Retry |
| no | everything finished | `Empty` |

The presentation layer reads errors as `DomainException.error` (a domain type), so it never imports the data-layer mapper.

## 4. Decisions made, and alternatives considered
- **`PagingData` next to the state, not inside it.** It's a stream with its own load states, and putting it in a `data class` would break equality and recomposition. This follows the PLAN.md design, and an ADR comes in T4.2.
- **No `Retry` action.** Paging owns retry and refresh, so the screen calls `LazyPagingItems.retry()` directly. A `Retry` intent would only be forwarded back to the UI.
- **Effects through a `Channel` with `receiveAsFlow`.** Navigation happens exactly once and isn't replayed after rotation. A `SharedFlow` with replay could navigate twice, and one without replay could drop the event while the screen is between collectors.
- **Formatting in `MovieUi`.** Composables only place text. Unrated movies (0 votes) get no badge, because "0.0" would read as "terrible" rather than "not rated yet".
- **Load-state mapping as a pure function, not in the ViewModel.** `LoadStates` belong to `LazyPagingItems` in the UI. A pure function keeps the composable to a simple `when` and makes every combination unit-testable.

## 5. How it is tested
- **`ListStatusTest` (9):**
  - with items: refreshing, refresh failed (with the error), idle;
  - without items: source loading, network loading (not Empty), failed refresh gives `Error`, finished with no rows gives `Empty`;
  - an unknown throwable gives `Unknown`;
  - the footer for loading, error, idle and end of list.
- **`MovieUiTest` (5):** all fields, rounding (7.97 → "8.0"), locale (German "7,5"), no badge for 0.0, unknown year and poster stay `null`.
- **`MoviesViewModelTest` (3):** movies are exposed as display models in order, a click emits exactly one `NavigateToDetails`, and dismissing the notice updates the state.
  - *Testing note:* `asSnapshot()` never completes on the `cachedIn` flow itself, so the test snapshots the first emitted `PagingData`. The test uses `UnconfinedTestDispatcher`, so work launched in `viewModelScope` runs immediately.

Results: 85 of 85 unit tests pass. The device check is done together with T1.5, as one cycle at the user's request.

## 6. How to build it from scratch
1. Define the contract: a `data class` state, a `sealed` action, a `sealed` effect.
2. Write the `@HiltViewModel` with `useCase().map { it.map(::toUi) }.cachedIn(viewModelScope)`, a `MutableStateFlow` state, a `Channel(BUFFERED)` for effects, and `onAction` with an exhaustive `when`.
3. Write `toListStatus(itemCount)`: items win, then loading, then error, then empty. Read errors through `DomainException`.
4. Unit-test the mapping with hand-built `CombinedLoadStates`, and the ViewModel with `MainDispatcherRule`, Turbine (`effects.test {}`) and `asSnapshot`.
