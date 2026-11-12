# 15: Movies list, screen UI (T1.5)

## 1. What was built
The home screen the user sees (R2, R4, R9, R10, R32, R33):
- **`MoviesRoute`** (stateful): gets `MoviesViewModel` through Hilt, collects the state and `LazyPagingItems`, and turns `NavigateToDetails` effects into navigation while the screen is visible.
- **`MoviesScreen`** (stateless): one `when` over `ListStatus`, with a crossfade between kinds of state (**A4**):

  | State | UI |
  |---|---|
  | Loading | A grid of card-shaped placeholders. |
  | Empty | `EmptyView` ("No movies to show"). |
  | Error | `ErrorView` with the matching icon and message, and **Retry**. |
  | Content | The poster grid. A thin **progress bar** shows while a background refresh runs (R9). A **"Couldn't refresh. Showing saved movies."** notice with Retry and Dismiss appears when a refresh fails while cached movies are shown (R9). |

- **`MovieCard`**: a 2:3 poster with a **300 ms crossfade** (**A2**) and a placeholder icon when there's no poster. A dark gradient carries the **title and year** (R2), and the amber **rating badge** sits in the top-end corner. The card **shrinks to 96% with a spring while pressed** (**A1**).
- **`MovieGrid`**: an adaptive grid (cards at least 150 dp wide, so 2 columns on a phone and more on a tablet), keyed by movie ID. Its **paging footer** shows a spinner, or an error with Retry, at the bottom of the list (R4), switching between them with a crossfade (**A9**).
- **Scroll-to-top button** (added after the cycle check, at the user's request). It pops in (scale + fade) once the first visible item is past index 8, about four rows on a phone, and only while the list is shown. Tapping it scrolls smoothly back to the top: from beyond item 30 it first jumps to item 12, so the animation stays short. It then re-shows the top app bar, because programmatic scrolls don't pass through nested scrolling.
- **Collapsing top app bar** (`enterAlways`) and **edge-to-edge** layout: the grid scrolls behind the navigation bar with matching bottom padding.
- **Coil** is configured once in `MovieApplication`: a 300 ms crossfade, plus memory and disk caches, so **posters seen before still show offline**. Images use Coil's own HTTP client, so `image.tmdb.org` never receives the API token.

## 2. Files created or changed

| Layer | File | Change |
|-------|------|--------|
| Presentation | `feature/movies/presentation/MoviesScreen.kt` | Replaces the T0.9 stub: `MoviesRoute` and `MoviesScreen` with previews. |
| Presentation | `feature/movies/presentation/component/MovieCard.kt` | New. |
| Presentation | `feature/movies/presentation/component/MovieGrid.kt` | New. Grid, paging footer, loading skeleton. |
| Presentation | `feature/movies/presentation/component/CachedDataNotice.kt` | New. |
| Presentation | `feature/movies/presentation/component/ScrollToTopButton.kt` | New. An animated FAB, plus `drawable/ic_arrow_upward.xml`. |
| Navigation | `core/navigation/AppNavigation.kt` | The `MoviesRoute` entry. |
| App | `MovieApplication.kt` | `SingletonImageLoader.Factory` with crossfade. |
| Design system | `core/designsystem/theme/Color.kt` | `OnPosterScrim`, a theme-level color for text on the poster gradient. |
| Resources | `drawable/ic_close.xml`; `values(-ar)/strings.xml` | New strings in English and Arabic. The T0.9 movie-list stub strings are removed. |

## 3. Data flow walkthrough

```
MoviesRoute
  state  ◄── viewModel.state.collectAsStateWithLifecycle()
  movies ◄── viewModel.movies.collectAsLazyPagingItems()
  effects ── repeatOnLifecycle(STARTED) ── NavigateToDetails(id) ─► onMovieClick(id) ─► backStack.add(DetailsRoute(id))
     │
     ▼
MoviesScreen(state, movies, onAction)
  status = movies.loadState.toListStatus(movies.itemCount)          // pure, unit-tested in T1.4
  AnimatedContent(status, contentKey = kind) {
     Loading → MovieGridSkeleton
     Empty   → EmptyView
     Error   → ErrorView(error, onRetry = movies::retry)
     Content → [LinearProgressIndicator if refreshing]
               [CachedDataNotice if refreshError && !dismissed]
               MovieGrid(movies, footer = loadState.toFooterStatus(), onRetry = movies::retry)
  }
```

User actions: tapping a card sends `onAction(MovieClicked(id))`, the ViewModel emits an effect, and the app navigates. Retry calls `movies.retry()`, which repeats the failed refresh or append, including the RemoteMediator's. Dismiss sends `onAction(DismissCachedNotice)`.

## 4. Decisions made, and alternatives considered
- **Route and screen split.** The screen gets plain data and lambdas, so previews and Compose UI tests (T1.6) can render any state without Hilt or a network.
- **Effects collected with `repeatOnLifecycle(STARTED)`.** Navigation never happens while the app is in the background. Combined with the `Channel` (T1.4), each event is handled exactly once.
- **Crossfade keyed on the kind of state.** `contentKey = { it::class }` animates Loading → Content and similar changes, but not every Content update (refresh flag, notice). This keeps the grid and its scroll position stable.
- **Cached notice instead of a full-screen error.** With movies on screen, an offline refresh shouldn't block browsing (R9). The notice explains the situation and offers Retry. The icon shows whether it's a connection problem (cloud-off) or another error.
- **No rating badge for unrated movies.** That's decided in `MovieUi` (T1.4), so the card only checks for `null`.
- **Placeholder under the image** instead of Coil's `placeholder`/`error` parameters. One layer covers loading, a missing poster and a failed download, and it uses theme colors.
- **`OnPosterScrim` (white) as a theme token.** The poster gradient is always dark, so text on it must be light in both themes. Keeping the value in the theme layer follows "no hardcoded colors in composables".
- **Skeleton cards stay static.** Shimmer is extra E4.
- **No press animation while scrolling** (found in the cycle check). Every fling starts with a finger-down on some card, so the card reported a press until the gesture turned into a scroll. That made cards shrink and spring back while scrolling fast. The grid now passes `pressFeedbackEnabled = !isScrollInProgress`, so A1 only answers a deliberate tap or hold.
- **No item-placement animations on the paged list** (found in the cycle check). Every page the RemoteMediator writes to Room makes Paging reload the list, and `Modifier.animateItem()` turned each reload into cards sliding up or down. In PLAN.md, A3 was specified for search results only (T3.4), so it was removed here.

## 5. How it is tested
- **Build checks:** `assembleDebug` passes, 85 of 85 unit tests pass (the list logic is tested in T1.4), and `lintDebug` reports 0 errors.
- **On the emulator** (Pixel 4 AVD), checked with screenshots during development:
  - **Online, first launch:** the poster grid with titles, years and rating badges. Pages 1–4 were cached, per Logcat.
  - **Offline relaunch:** the cached list *with posters*, from Coil's disk cache, plus the "Couldn't refresh. Showing saved movies." notice with Retry and Dismiss.
- **Compose UI tests** for these states come in T1.6.
- **Cycle check by the developer** (T1.4 and T1.5 together, at the user's request): see the checklist in the commit and PR.

## 6. How to build it from scratch
1. Configure Coil in the `Application` (`SingletonImageLoader.Factory` with `crossfade`).
2. Build `MovieCard`: a `Card(onClick, interactionSource)` with a 2:3 `Box`, a placeholder, `AsyncImage(Crop)`, a gradient title overlay and a `RatingBadge`. Add the press scale with `collectIsPressedAsState` + `animateFloatAsState` + `graphicsLayer`.
3. Build `MovieGrid`: `LazyVerticalGrid(GridCells.Adaptive(150.dp))` using `itemKey`/`itemContentType` from paging-compose, plus a full-span footer item with `AnimatedContent`.
4. Build `MoviesScreen`: a `Scaffold` with an `enterAlways` `TopAppBar`, `toListStatus` → `AnimatedContent(contentKey = class)` → one composable per state.
5. Build `MoviesRoute`: `hiltViewModel()`, `collectAsStateWithLifecycle`, `collectAsLazyPagingItems`, and effects in `LaunchedEffect` + `repeatOnLifecycle(STARTED)`.
6. Replace the navigation stub with `MoviesRoute`, remove its stub strings, and add the new strings in `values` and `values-ar`.
