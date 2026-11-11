# 20: Movie details, screen UI (T2.4)

## 1. What was built
The real details screen (R6, R32, R33), replacing the T0.9 placeholder. Tapping a movie in the list now opens:
- **Hero** (`DetailsHeader`): a full-width backdrop that runs behind the status bar and fades into the page. The poster overlaps it at the start edge, with the title (marked as a heading for screen readers) and the tagline beside it. The backdrop moves at half the scroll speed (**A6**, parallax).
- **Top bar** (`DetailsTopBar`): a tonal back button over the image. Behind the status bar there is a surface-coloured gradient, so the status icons stay readable on any backdrop. As the backdrop scrolls away the bar fills with the surface colour and the title fades in (**A6**, collapsing top bar). A thin progress bar shows while cached details are being refreshed.
- **Info row** (`InfoRow`): "Jul 16, 2008 · 2 h 32 min · English", leaving out whatever is unknown. A movie that is not released yet gets a status label ("In production").
- **Genres** (`GenreChips`): teal tonal labels that wrap onto new lines.
- **Rating** (`RatingSection`): a ring that fills from 0 while the number counts up (**A7**), next to "TMDb rating" and the vote count with the correct plural form (English one/other; Arabic zero/one/two/few/many/other). Screen readers hear one sentence: "Rated 8.5 out of 10, 37,326 votes". A movie without votes shows an empty ring and "Not rated yet".
- **Synopsis** (`Synopsis`): collapsed to four lines, with "Read more" / "Show less" only when the text is longer. Expanding and collapsing animate (**A8**). A missing synopsis shows a short message.
- **States**: a skeleton (backdrop block and text lines) while loading, the shared `ErrorView` with Retry, and a crossfade only when the kind of state changes (**A4**). A refresh that fails while details are shown triggers a snackbar with Retry. Images crossfade in through the app-wide Coil setting (**A2**).
- **Navigation**: `MovieDetailsRoute` creates the `DetailsViewModel` for the entry's movie ID. Back (button or effect) removes exactly this entry, so a double tap can never pop the list too.

Everything was checked on an emulator: online, offline with the details saved (content plus snackbar), offline without a saved copy (full error), and Retry after the network returns.

## 2. Files created or changed

| Layer | File | Change |
|-------|------|--------|
| Presentation | `feature/details/presentation/DetailsScreen.kt` | Replaces the T0.9 stub: `MovieDetailsRoute` (stateful), `DetailsScreen` (stateless), content, skeleton, previews |
| Presentation | `feature/details/presentation/component/DetailsHeader.kt` | New: backdrop with parallax, poster, title and tagline |
| Presentation | `feature/details/presentation/component/DetailsTopBar.kt` | New: transparent-to-solid top bar, status-bar protection, refresh indicator |
| Presentation | `feature/details/presentation/component/InfoRow.kt` | New: date · runtime · language, status label |
| Presentation | `feature/details/presentation/component/GenreChips.kt` | New |
| Presentation | `feature/details/presentation/component/RatingRing.kt` | New: `RatingSection` with the animated ring |
| Presentation | `feature/details/presentation/component/Synopsis.kt` | New |
| Presentation | `feature/details/presentation/model/MovieDetailsUi.kt` | Adds the raw `voteCount` for plural selection |
| Navigation | `core/navigation/AppNavigation.kt` | The details entry uses `MovieDetailsRoute`; Back removes only its own entry |
| Resources | `res/values/strings.xml`, `res/values-ar/strings.xml` | Details strings and the `details_votes` plurals in English and Arabic; the placeholder string is removed |
| Tests | `androidTest/.../feature/details/presentation/DetailsScreenTest.kt` | New: 7 Compose UI tests |
| Tests | `test/.../feature/details/presentation/model/MovieDetailsUiTest.kt` | Checks `voteCount` is passed through |
| Docs | `docs/PLAN.md` | T2.4 ticked; component names updated |

## 3. Data flow walkthrough

```
AppNavigation: entry<DetailsRoute> ─▶ MovieDetailsRoute(movieId, onBack = { backStack.remove(route) })
  hiltViewModel<DetailsViewModel, Factory>(creationCallback = { it.create(movieId) })
  state  ─▶ DetailsScreen(state, snackbarHostState, onAction)
  effects (collected while STARTED):
      NavigateBack          ─▶ onBack()
      ShowRefreshError(err) ─▶ snackbar(err message, "Retry") ─▶ onAction(Retry) if tapped
DetailsScreen: AnimatedContent(status) { Loading ─▶ skeleton | Error ─▶ ErrorView | Content ─▶ DetailsContent }
               DetailsTopBar drawn on top
```

The scroll-driven effects read `ScrollState` only in draw-phase lambdas, so scrolling redraws but never recomposes:

```kotlin
// DetailsHeader: parallax
Modifier.graphicsLayer { translationY = scrollState.value * PARALLAX_FACTOR }

// DetailsTopBar: protection gradient fades out while the solid bar fades in
Modifier.drawBehind {
    val fraction = solidFraction()
    drawRect(brush = Brush.verticalGradient(listOf(surface.copy(alpha = PROTECTION_ALPHA * (1f - fraction)), Color.Transparent)))
    drawRect(color = surface.copy(alpha = fraction))
}
```

The rating ring draws its arc from an `Animatable` inside a `Canvas`, so only the counting number recomposes, and only for the 0.9 s of the animation:

```kotlin
val progress = remember { Animatable(0f) }
LaunchedEffect(fraction) { progress.animateTo(fraction, tween(FILL_DURATION_MS, easing = FastOutSlowInEasing)) }
```

## 4. Decisions made, and alternatives considered
- **A custom top bar instead of `TopAppBarScrollBehavior`.** Material's scroll behaviours change colour as soon as the content moves. The design needs the bar to stay transparent over the backdrop and turn solid only once the backdrop has scrolled away, so the fraction is computed from the scroll position against the backdrop height.
- **Status-bar protection gradient** (from the `edge-to-edge` skill): `enableEdgeToEdge()` already sets the icon colour for each theme, and a gradient in the theme's surface colour behind the status bar makes those icons readable on any image, in both themes. Forcing light icons on this screen was considered, but would need window calls on enter and exit, and would be wrong when the backdrop is light.
- **`verticalScroll` instead of `LazyColumn`.** The page is one header and a few sections, so there is nothing to recycle. A plain `ScrollState` also gives the exact pixel offset the parallax and the top bar need.
- **Runtime and status are worded in the UI.** Their text comes from string resources ("%1$d h %2$d min", status labels), which belong to the UI layer. Dates, numbers and language names are formatted in `toUi(locale)` (T2.3).
- **One string with two placeholders for the rating description.** Joining translated strings with `", "` would use the wrong comma in Arabic (`،`).
- **Read more / Show less measured in both states.** The first version decided "is the text cut off?" only while collapsed. The UI test for an already-expanded synopsis (as after rotation, where the ViewModel keeps the flag) showed that "Show less" then never appeared. It now compares the line count with the collapsed limit when expanded.
- **Back removes this entry (`backStack.remove(route)`), not "the last one".** Two quick Back events would otherwise remove the list as well and leave an empty back stack.
- **No status label for Released or Unknown.** Released is the normal case, and a label for it would be noise.

## 5. How it is tested
- **Emulator** (Pixel 4, API 29, dark theme):
  - **Online:** The Dark Knight showed the backdrop, poster, info row, chips, ring and synopsis.
  - **Read more:** expanded the synopsis and switched to Show less.
  - **Scrolling:** the top bar filled in.
  - **Offline with saved details:** the saved content showed, with the "No internet" snackbar and Retry.
  - **Offline without saved details:** the full error screen.
  - **Retry after the network returned:** loaded Project Hail Mary.
- **Light theme:** not checked on the emulator, since `adb` could not switch its dark mode. The light previews render, but light mode needs a manual check.
- **`DetailsScreenTest` (7 Compose UI tests, all pass):**
  - the loading placeholder;
  - the content details, including the combined rating description;
  - Read more sends `ToggleSynopsis`;
  - an expanded synopsis offers Show less;
  - a missing synopsis shows its message;
  - the error message, and Retry sends `Retry`;
  - the back button sends `Back`.
- **Unit tests and build:** `gradlew.bat assembleDebug` and `testDebugUnitTest` pass.

## 6. How to build it from scratch
1. Add the details strings and the `details_votes` plurals in `values` and `values-ar`, and remove the placeholder string.
2. Build the leaf components with previews: `InfoRow` (with `runtimeText` and the status `labelRes`), `GenreChips` (`FlowRow`), `RatingSection` (a `Canvas` ring and an `Animatable`) and `Synopsis` (`animateContentSize`, measured overflow).
3. Build `DetailsHeader`: a fixed-height, clipped backdrop with a `graphicsLayer` parallax and a fade-to-surface gradient, plus a poster row padded down so it overlaps the backdrop.
4. Build `DetailsTopBar`: a transparent `TopAppBar` with a tonal back button, a `drawBehind` gradient and a solid fill driven by the scroll offset, and the refresh indicator.
5. Write `DetailsScreen`: a `Scaffold` with `contentWindowInsets = WindowInsets(0)` and a snackbar host, `AnimatedContent` over the status, and the top bar drawn on top.
6. Write `MovieDetailsRoute`: `hiltViewModel` with `creationCallback`, state collection, and effect handling under `repeatOnLifecycle(STARTED)`.
7. Point the `DetailsRoute` entry in `AppNavigation` to `MovieDetailsRoute`, with `onBack = { backStack.remove(route) }`.
8. Add `DetailsScreenTest`, and check online, offline with cache, offline without cache, and Retry on an emulator.
