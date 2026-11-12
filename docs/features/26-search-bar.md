# 26: Search, search bar UI (T3.4)

## 1. What was built
Search on the Movies screen (R11, R12, R13, R32, R33):
- **`MovieSearchBar`**: a pill-shaped field pinned under the top app bar. When the app bar collapses on scroll, the search bar stays. It has:
  - a search icon and the hint "Search movies";
  - a clear button that fades and scales in only when there is text (**A5**);
  - a keyboard action key set to Search, which closes the keyboard (results already update while typing).
- **Results in the same grid:** while searching, the content area shows the search results with the same four states as the list:
  - Loading skeleton;
  - Content, with the paging footer and its Retry;
  - Empty: "No results for “…”" with a hint;
  - Error with Retry.

  Result cards slide and fade into place when the results change (**A3**). The popular list keeps placement animations off, as explained in T1.5.
- **Separate scroll positions:** the list and the results each keep their own. Clearing a search returns to the same place in the popular list, and a new query starts its results at the top.
- **Keyboard:**
  - it hides as soon as the grid is scrolled;
  - the content fits above it (`fitInside(WindowInsetsRulers.Ime)`), so results and states are never covered;
  - the activity declares `adjustResize`.
- **App bar:** it re-appears when switching between list and search, so it is never left hidden above a list that is already at the top.
- **Language switch:** both the list and the search results are fetched again after a switch.
- **Strings** in English and Arabic.

## 2. Files created or changed

| Layer | File | Change |
|-------|------|--------|
| Presentation | `feature/movies/presentation/component/MovieSearchBar.kt` | New |
| Presentation | `feature/movies/presentation/MoviesScreen.kt` | Search bar in the top bar; list/results switch; per-list grid state; keyboard and app-bar handling; new previews |
| Presentation | `feature/movies/presentation/component/MovieGrid.kt` | Optional `animateItems` (A3) |
| Manifest | `AndroidManifest.xml` | `windowSoftInputMode="adjustResize"` on `MainActivity` |
| Resources | `drawable/ic_search.xml`, strings in `values` and `values-ar` | Search icon, hint, clear label, empty-state texts |
| Tests | `androidTest/.../feature/movies/presentation/MoviesScreenTest.kt` | Passes the (empty) search results to the new screen signature |
| Docs | `docs/PLAN.md` | T3.4 ticked; component list updated |

## 3. Data flow walkthrough

```
MovieSearchBar ─QueryChanged(text)─▶ MoviesViewModel (T3.3: debounce → validate → flatMapLatest)
MoviesRoute: movies.collectAsLazyPagingItems(), searchResults.collectAsLazyPagingItems()
MoviesScreen:
   items     = if (state.isSearching) searchResults else movies
   gridState = if (state.isSearching) searchGridState else listGridState
   status    = items.loadState.toListStatus(items.itemCount)       (same mapper as the list, T1.4)
   AnimatedContent(ScreenContent(isSearching, status)) { Loading | Empty (list or search text) | Error | Content(grid) }
```

Keyboard and IME handling:

```kotlin
AnimatedContent(
    …,
    modifier = Modifier
        .fillMaxSize()
        .padding(top = innerPadding.calculateTopPadding())
        .fitInside(WindowInsetsRulers.Ime.current),   // edge-to-edge skill, preferred over imePadding()
)

snapshotFlow { gridState.isScrollInProgress }.filter { it }.collect {
    keyboard?.hide()
    focusManager.clearFocus()
}
```

## 4. Decisions made, and alternatives considered
- **A `TextField` pinned under the app bar, not Material's `SearchBar`.** Results are shown in the page's own grid, as R11 asks ("search on the list screen"). Material's `SearchBar` opens a separate full-screen results layer, which would duplicate the grid, its states and its paging footer.
- **The Search key only closes the keyboard.** Results already follow typing (R13), so "submit" has nothing else to do.
- **One `AnimatedContent` keyed on (list or search, kind of state).** Switching between list and results crossfades (A4). Updates within the same state do not re-animate.
- **Placement animation only for search results (A3).** The popular list reloads from Room for every page the RemoteMediator caches, and animating there would make cards slide while scrolling. Search results change as a whole per query, which is what A3 is meant to show.
- **Two grid states.** Clearing a search should not lose your place in the popular list. Reusing one state would also carry a scroll offset from one list into the other.
- **IME handling from the `edge-to-edge` skill:** `adjustResize` in the manifest, plus `fitInside(WindowInsetsRulers.Ime)` on the content (preferred over `imePadding()`, which can double-pad).
- **Re-showing the app bar on switch.** The bar's collapse is shared by both grids. Without the reset, scrolling the results and then clearing left the list at the top with the title hidden and no way to scroll it back. This was found during the device check.
- **The keyboard after Back is an adb artifact.** During the device check, the keyboard popped up after returning from details only when `adb input text` had sent key events, which take Android out of touch mode, where Compose focuses the first field. With touch-only navigation it stays closed, as verified on the emulator.

## 5. How it is tested
On the emulator (Pixel 4, API 29), checking:
- **Typing:** "batman" showed matching results with the clear button, and the grid ended above the keyboard.
- **Scrolling:** hid the keyboard; the app bar collapsed and the search bar stayed pinned.
- **Opening a result:** showed its details, and Back returned to the same query, results and position.
- **Clearing:** brought back the popular list at its old position, with the app bar shown.
- **No matches:** "qwxzqqzx" showed "No results for “qwxzqqzx”" above the keyboard.
- **Offline:** "inception" showed the "No internet" error with Retry.

Automated:
- `MoviesScreenTest` (10) and `AppNavigationTest` (4) pass 14/14 on the emulator.
- All unit tests pass, and `assembleDebug` succeeds.
- The search UI's own Compose tests are T3.5.

## 6. How to build it from scratch
1. Add the search strings (English and Arabic) and `ic_search`.
2. Write `MovieSearchBar`: a `TextField` with a pill shape, no indicator, a leading icon, an `AnimatedVisibility` clear button, `ImeAction.Search` and `onSearch` hiding the keyboard.
3. Give `MovieGrid` an `animateItems` flag that applies `Modifier.animateItem()` to cards.
4. In `MoviesRoute`, collect `searchResults` as `LazyPagingItems`, and refresh it too after a language change.
5. In `MoviesScreen`:
   - put the app bar and the search bar in a `Column` as the `topBar`;
   - pick the items and grid state by `isSearching`;
   - key `AnimatedContent` on both;
   - give the search Empty state its own text.
6. Add `HideKeyboardWhileScrolling`, `ScrollResultsToTopOnNewSearch`, and the app-bar reset on `isSearching` changes.
7. Add `adjustResize` to `MainActivity` and `fitInside(WindowInsetsRulers.Ime.current)` to the content.
8. Update callers of `MoviesScreen` (previews, `MoviesScreenTest`), then check on a device: type, scroll, open a result, clear, no results, and offline.
