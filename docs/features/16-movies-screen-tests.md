# 16: Movies list, screen UI tests (T1.6)

## 1. What was built
Instrumented Compose tests for the Movies screen (R30, R4). They render the stateless `MoviesScreen(state, movies, onAction)` with fixed paging data and load states, so every state is checked without a network, a database or Hilt.

| Test | What it proves |
|---|---|
| `loadingStateShowsPlaceholderGrid` | First refresh with nothing cached shows the skeleton grid. |
| `contentStateShowsMovieCards` | Movie titles render, and the rating badge exposes "Rated 7.5 out of 10" to screen readers. |
| `emptyStateShowsEmptyMessage` | A finished load with no rows shows "No movies to show". |
| `errorStateWithoutCacheShowsErrorMessageAndRetry` | A failed refresh with no cache shows the message for that `DomainError`, plus Retry. |
| `clickingCardSendsMovieClicked` | Tapping a card sends `MovieClicked(id)` with the right ID. |
| `appendErrorShowsFooterWithRetry` | A failed next page shows the footer error and Retry (R4). |
| `failedRefreshWithCacheShowsNoticeAndKeepsMovies` | A failed network refresh over cached movies shows the notice and keeps the grid (R9). |
| `dismissingNoticeSendsDismissCachedNotice` | The notice's Dismiss button sends `DismissCachedNotice`. |
| `dismissedNoticeStaysHidden` | With `isCachedNoticeDismissed = true` the notice is not shown, and the movies are. |
| `retryAfterErrorLoadsMovies` | Retry really re-runs the failed load: a real `Pager` fails once, and Retry shows the movies. |

## 2. Files created or changed

| Layer | File | Change |
|-------|------|--------|
| Presentation (androidTest) | `app/src/androidTest/.../feature/movies/presentation/MoviesScreenTest.kt` | New: 10 Compose UI tests. |
| Docs | `docs/PLAN.md` | T1.6 ticked, its description corrected (see Decisions), tracker updated. |
| Docs | `docs/features/16-movies-screen-tests.md` | This document. |

No production code changed.

## 3. Data flow walkthrough
The tests replace everything below the screen with a `Flow<PagingData<MovieUi>>`. The rest of the path is the real one: `collectAsLazyPagingItems()` → `LoadStates.toListStatus()` / `toFooterStatus()` → the `when` in `MoviesScreen` → the components.

Each state is set up through the load states that `ListStatus` reads:

```kotlin
// Error without cache: no items, the source refresh failed with a domain error.
PagingData.from(emptyList(), sourceLoadStates = loadStates(refresh = errorState(DomainError.NoInternet)))

// Cached movies with a failed network refresh: Room has rows, the RemoteMediator failed (R9).
PagingData.from(
    data = movies,
    sourceLoadStates = loadStates(),
    mediatorLoadStates = loadStates(refresh = errorState(DomainError.NoInternet)),
)

private fun errorState(error: DomainError) = LoadState.Error(DomainException(error))
```

Actions are captured in a list passed as `onAction`, so the tests check exactly what the screen reports:

```kotlin
composeRule.onNodeWithText("Second Movie").performClick()
assertEquals(listOf(MoviesUiAction.MovieClicked(2)), actions)
```

Retry is the one interaction not routed through `onAction`, so it is tested against a real `Pager`:

```kotlin
private class FailOnceThenSucceedPagingSource(...) : PagingSource<Int, MovieUi>() {
    override suspend fun load(params: LoadParams<Int>) =
        if (++loadCount == 1) LoadResult.Error(DomainException(DomainError.NoInternet))
        else LoadResult.Page(data = movies, prevKey = null, nextKey = null)
}
```

## 4. Decisions made, and alternatives considered
- **Retry is tested by its effect, not by an action.** The plan said the footer Retry would send `onAction(Retry)`. T1.5 instead calls `LazyPagingItems.retry()` directly, because Paging owns the load state and a `Retry` action would only forward the call. So the test uses a `PagingSource` that fails once and checks that the movies then appear and that `load` ran twice. This proves the button is wired to Paging, which an action check could not. The PLAN.md task text was corrected.
- **Explicit load states on every `PagingData.from`.** Without them, `LazyPagingItems.loadState` keeps its initial `refresh = Loading`, so an empty list shows the skeleton and never Empty. The first test run caught this, and a comment on the `loadStates()` helper records it.
- **Look up nodes by string resources and content descriptions, not test tags.** The tests read the same strings the user sees (through `activity.getString`), so they also work in Arabic and no test-only modifiers were added to production code. The rating badge clears its child semantics and sets "Rated X out of 10", so the test asserts that description. That also checks the accessibility label.
- **Stateless screen, not the Hilt `MoviesRoute`.** Testing `MoviesScreen` keeps the tests fast and deterministic. The DI, ViewModel and navigation wiring is covered end to end in T2.5.
- **Posters are `null` in test data**, so Coil never makes a network request during the tests.

## 5. How it is tested
- `gradlew.bat assembleDebug` and `gradlew.bat testDebugUnitTest` pass.
- `gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.trianglz_task_ahmedhussin.feature.movies.presentation.MoviesScreenTest`: 10/10 pass on a Pixel 4 emulator (API 29).
- Run the full instrumented suite with `gradlew.bat connectedDebugAndroidTest`. On a low-memory machine, add `--max-workers=2` and avoid running it at the same time as the unit tests.

## 6. How to build it from scratch
1. Check the androidTest dependencies from T0.10: `compose-ui-test-junit4`, `ui-test-manifest` (debug), and the Hilt test runner. A plain `createAndroidComposeRule<ComponentActivity>()` works under `HiltTestApplication`, because these tests don't inject anything.
2. Create `MoviesScreenTest` in the same package as `MoviesScreen`, with a `MutableList<MoviesUiAction>` that collects actions.
3. Add a `setScreen(flow, state)` helper that wraps `MoviesScreen` in `MovieAppTheme` and calls `collectAsLazyPagingItems()` on the flow.
4. Add the `loadStates(refresh, append)` and `errorState(DomainError)` helpers, and always pass load states to `PagingData.from`.
5. Write one test per `ListStatus` (Loading, Content, Empty, Error), then the footer and cached-notice cases.
6. Write the click and dismiss tests and assert the captured actions.
7. Add `FailOnceThenSucceedPagingSource` and the Retry test using `Pager(PagingConfig(pageSize = 20)) { source }.flow`.
8. Run the class on an emulator.
