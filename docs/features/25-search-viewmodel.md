# 25: Search, ViewModel (T3.3)

## 1. What was built
Search in `MoviesViewModel` (R13, R15, R17, R29), ready for the search bar in T3.4:
- **State:** `MoviesUiState` gains `query` (exactly what is in the search bar, updated on every keystroke) and `isSearching` (results are shown instead of the popular list).
- **Actions:** `QueryChanged(text)` and `ClearQuery`.
- **`searchResults: Flow<PagingData<MovieUi>>`**, next to the existing `movies` flow:
  - it waits 350 ms after the last keystroke;
  - clearing is not delayed;
  - it compares validated queries, so `"coco"` and `"coco "` search once;
  - it cancels the previous search when the query changes (`flatMapLatest`);
  - it is cached in `viewModelScope`, so results survive rotation.
- **Privacy:** `QueryChanged` prints only the text length when logged, and the search log records only the query length, so search text never reaches Logcat.

## 2. Files created or changed

| Layer | File | Change |
|-------|------|--------|
| Presentation | `feature/movies/presentation/MoviesContract.kt` | `query` and `isSearching` in state; `QueryChanged` and `ClearQuery` actions |
| Presentation | `feature/movies/presentation/MoviesViewModel.kt` | Search pipeline, `searchResults`, new actions; injects `SearchMoviesUseCase` |
| Tests | `test/.../feature/movies/presentation/MoviesViewModelSearchTest.kt` | New: 8 virtual-time tests |
| Tests | `test/.../feature/movies/presentation/MoviesViewModelTest.kt` | Constructor update |
| Tests | `androidTest/.../core/navigation/AppNavigationTest.kt` | Its fake module also provides a `MovieSearchRepository` (it replaces `MoviesModule`) |
| Docs | `docs/PLAN.md` | T3.3 ticked; §4.1 state and actions updated |

## 3. Data flow walkthrough

```
QueryChanged(text) ─▶ state.query = text (capped at 100) ─────────────────────────────▶ search bar (T3.4)
state.query ─▶ distinctUntilChanged ─▶ debounce(350 ms, or 0 when blank) ─▶ validate ─▶ distinctUntilChanged
            ─▶ activeQuery (StateFlow<SearchQuery>)
                  ├─▶ state.isSearching = activeQuery is Valid
                  └─▶ flatMapLatest { SearchMoviesUseCase(query) } ─▶ map toUi ─▶ cachedIn ─▶ searchResults
```

```kotlin
private val activeQuery: StateFlow<SearchQuery> = _state
    .map { it.query }
    .distinctUntilChanged()
    .debounce { text -> if (SearchQueryValidator.validate(text) is SearchQuery.Idle) 0L else SEARCH_DEBOUNCE_MS }
    .map(SearchQueryValidator::validate)
    .distinctUntilChanged()
    .stateIn(viewModelScope, SharingStarted.Eagerly, SearchQuery.Idle)
```

## 4. Decisions made, and alternatives considered
- **`isSearching` follows the debounced query, not each keystroke.** If it switched on the first keystroke, the results area would show an empty "No results" for the 350 ms before the search starts. Following the debounced query keeps the popular list on screen while the user types, then swaps in the results (with their own Loading state while the first page loads).
- **No debounce when clearing.** Waiting 350 ms to show the list again after tapping "clear" would feel broken. The debounce selector returns 0 for idle text.
- **`distinctUntilChanged` after validation.** Comparing raw text would search again for `"coco "` after `"coco"`. Comparing validated queries makes whitespace edits free.
- **`flatMapLatest`.** A slow response for an old query must never replace results for a newer one, and it cancels the old request's paging.
- **One source of truth for the query.** The pipeline reads `state.query` rather than keeping a second `MutableStateFlow`, so the field and the search can never disagree.
- **The field is capped at the validator's maximum (100).** What is shown is what is searched, and nothing is cut silently.
- **Search text stays out of logs.** It is user input (CLAUDE.md logging rules), so `QueryChanged.toString()` prints only the length.
- **350 ms.** It skips the intermediate keystrokes of a typed word but still feels "as you type" (R13).

## 5. How it is tested
`MoviesViewModelSearchTest` (8, standard test dispatcher with virtual time; `searchResults` collected in `backgroundScope` like the screen does):
- the bar text updates at once, but nothing is searched and `isSearching` stays false until the debounce passes;
- typing "c", "co", "coc", "coco" 100 ms apart makes one request, for "coco";
- "coco", then "coco ", then "  coco" make a single request;
- clearing returns to the list without advancing time;
- blank text never searches;
- a new query replaces the previous results (the snapshot shows only the new query's results);
- the query is capped at 100 characters;
- the logged `QueryChanged` never contains the text.

`MoviesViewModelTest` (4) still passes. All 181 unit tests pass, the instrumented tests compile, and `AppNavigationTest` passes 4/4 on the emulator. Nothing changes on a device until T3.4 adds the search bar.

## 6. How to build it from scratch
1. Add `query` and `isSearching` to `MoviesUiState`, and `QueryChanged` (with a length-only `toString`) and `ClearQuery` to `MoviesUiAction`.
2. Inject `SearchMoviesUseCase` into `MoviesViewModel`.
3. Build `activeQuery` from `state.query`: distinct → debounce (0 for blank) → validate → distinct → `stateIn(Eagerly)`.
4. Update `isSearching` from `activeQuery` in `init`.
5. Build `searchResults`: `flatMapLatest` to the use case, map to `MovieUi`, `cachedIn(viewModelScope)`.
6. Handle the actions: set the capped query, or clear it.
7. Update every place that builds the ViewModel's dependencies (unit test helper, the navigation test's fake module).
8. Write the virtual-time tests with `advanceTimeBy`, `runCurrent` and `advanceUntilIdle`.
