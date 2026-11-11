# 27: Search, UI tests (T3.5)

## 1. What was built
`SearchScreenTest`: 8 Compose tests on the stateless `MoviesScreen` for its search behavior (R13, R30).

| Test | What it proves |
|---|---|
| `typingInTheSearchBarSendsQueryChanged` | Text typed into the field reaches the ViewModel as `QueryChanged(text)`. |
| `clearButtonIsHiddenWhenTheSearchBarIsEmpty` | No ✕ on an empty field (A5's hidden state). |
| `clearButtonShowsWithTextAndSendsClearQuery` | With text, the ✕ is there and sends `ClearQuery`. |
| `notSearchingShowsThePopularList` | Typed text alone does not switch the content: the list stays until the ViewModel says it is searching (debounced, T3.3). |
| `searchingShowsTheResultsInsteadOfThePopularList` | While searching, the results grid replaces the popular list. |
| `noMatchesShowsTheQueryInTheEmptyState` | No matches shows "No results for “qwxz”" (trimmed query) and the hint. |
| `searchErrorShowsRetryWhichLoadsTheResults` | A failed search shows the error, and Retry re-runs the load for real (a `PagingSource` that fails once). |
| `openingASearchResultSendsMovieClicked` | Tapping a result opens its details like any card. |

The paging helpers that `MoviesScreenTest` (T1.6) kept privately now live in a shared `testing/PagingTestUtils.kt`, so both screen tests use one copy: `loadStates()`, `errorState()` and the generic `FailOnceThenSucceedPagingSource<T>`.

This closes Phase 3: T3.1–T3.5 are done.

## 2. Files created or changed

| Layer | File | Change |
|-------|------|--------|
| Tests | `androidTest/.../feature/movies/presentation/SearchScreenTest.kt` | New: 8 tests |
| Tests | `androidTest/.../testing/PagingTestUtils.kt` | New: shared paging test helpers |
| Tests | `androidTest/.../feature/movies/presentation/MoviesScreenTest.kt` | Uses the shared helpers instead of private copies |
| Docs | `docs/PLAN.md` | T3.5 ticked; tracker moved to the Phase 3 PR and T4.1 |

No production code changed.

## 3. Data flow walkthrough
The tests give the screen a fixed `MoviesUiState` and two paging flows, then check what it shows and which actions it sends:

```
MoviesUiState(query, isSearching) ──┐
movies        (Popular Movie)  ─────┼─▶ MoviesScreen ─▶ shows list or results, sends actions to a list
searchResults (Search Hit / empty / failing Pager) ┘
```

```kotlin
composeRule.onNode(hasSetTextAction()).performTextInput("coco")
assertEquals(listOf(MoviesUiAction.QueryChanged("coco")), actions)
```

## 4. Decisions made, and alternatives considered
- **Screen tests on the stateless composable, not through the ViewModel.** The ViewModel's search logic (debounce, de-duplication, cancellation) is covered in virtual time by `MoviesViewModelSearchTest` (T3.3). Here the screen is checked for what it does with a given state, which is fast and deterministic.
- **Retry is tested by its effect.** As in T1.6, Retry calls `LazyPagingItems.retry()`, so a source that fails once proves the button re-runs the load.
- **Shared helpers instead of copies.** Two private `FailOnceThenSucceedPagingSource` classes in one package do not even compile (same JVM class name), and the `loadStates()` lesson from T1.6 belongs in one place. The paging source is now generic.
- **Locating the field by `hasSetTextAction()`.** That finds the text field itself, not its placeholder text, and works in either language.

## 5. How it is tested
- `SearchScreenTest`: 8/8 pass on a Pixel 4 emulator (API 29), and `MoviesScreenTest` still passes 10/10 with the shared helpers.
- The full instrumented suite passes 56/56 and the full unit suite passes 181/181 (`connectedDebugAndroidTest`, `testDebugUnitTest`, `assembleDebug`).

## 6. How to build it from scratch
1. Move `loadStates()`, `errorState()` and a generic `FailOnceThenSucceedPagingSource<T>` to `androidTest/.../testing/PagingTestUtils.kt`, and switch `MoviesScreenTest` to them.
2. Create `SearchScreenTest` with a `setScreen(state, searchResults)` helper that renders `MoviesScreen` with a one-movie popular list.
3. Write the action tests: type with `performTextInput`, click the ✕ by its content description, tap a result.
4. Write the content tests: `isSearching` false shows the list, true shows the results, and empty results show the query text.
5. Write the Retry test with `Pager { FailOnceThenSucceedPagingSource(...) }`.
6. Run the class on an emulator, then the full suite.
