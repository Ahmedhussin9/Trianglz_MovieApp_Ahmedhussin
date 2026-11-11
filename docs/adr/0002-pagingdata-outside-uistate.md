# 0002: Paged lists live next to the `UiState`, not inside it

**Status:** Accepted (Phase 1, T1.4; applied again to search in T3.3)

## Context
MVI asks for one immutable state per screen ([ADR 0001](0001-mvi-naming.md)). The movie list and the search results are paginated with Paging 3 (R3), whose output is a `Flow<PagingData<T>>`. `PagingData` is not a value: it is a stream of page events with its own load states, meant to be collected once by `collectAsLazyPagingItems()` and cached in the ViewModel with `cachedIn(viewModelScope)`.

## Decision
The ViewModel exposes paged lists as separate flows **next to** the state:

```kotlin
val state: StateFlow<MoviesUiState>                 // query, isSearching, notice dismissed…
val movies: Flow<PagingData<MovieUi>>               // cachedIn(viewModelScope)
val searchResults: Flow<PagingData<MovieUi>>        // cachedIn(viewModelScope)
```

The screen derives what to show from Paging's load states with one pure, tested function, `CombinedLoadStates.toListStatus(itemCount)`, which returns Loading, Empty, Error or Content. Retry calls `LazyPagingItems.retry()`.

## Alternatives considered
- **`PagingData` inside the `UiState`.** Every state copy would carry the stream; `distinctUntilChanged` and equality checks become meaningless, and a state update could re-submit the same `PagingData` to the UI, which Paging does not support.
- **Collect pages manually into a `List<Movie>` in the state.** This reimplements Paging (page keys, prefetch, retry, placeholders, memory limits) by hand and loses `RemoteMediator` integration.
- **A `Retry` action that forwards to Paging.** The ViewModel holds no `LazyPagingItems`, so it could only re-create the pager, which loses the scroll position and cached pages.

## Consequences
- **The "single state" rule has one documented exception per paged list.** Everything else (query, flags, dismissed notice) is still in the `UiState`.
- **Load-state logic is centralized and unit-tested** (`ListStatusTest`), not spread over composables.
- **UI tests feed fixed `PagingData` with explicit load states.** Without load states, `PagingData.from()` looks like it is still loading (learned in T1.6, now a shared test helper).
