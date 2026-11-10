# 09: Navigation 3 shell (T0.9)

## 1. What was built
The app's navigation skeleton, built with **Navigation 3** (R5, R19):
- Two type-safe routes: `MoviesRoute` (the start destination) and `DetailsRoute(movieId)`.
- `AppNavigation`, which hosts `NavDisplay` with:
  - a **saveable back stack** that survives rotation and **process death**;
  - **per-entry state saving**, which keeps `rememberSaveable` values such as scroll position;
  - **per-entry ViewModel scoping**, so each details entry gets its own ViewModel, cleared when it's popped.
- Stub `MoviesScreen` and `DetailsScreen` in their feature packages. They have real Scaffold and top-bar structure and correct edge-to-edge insets, and their bodies are placeholders until T1.5 and T2.4.
- An **auto-mirrored back arrow**, so it points the right way in Arabic (RTL).
- Double-tap protection on navigation clicks (`dropUnlessResumed`).

## 2. Files created or changed

| Layer | File | Change |
|-------|------|--------|
| Core / navigation | `core/navigation/Routes.kt` | New. `@Serializable` `MoviesRoute` and `DetailsRoute(movieId: Int)`, both implementing `NavKey`. |
| Core / navigation | `core/navigation/AppNavigation.kt` | New. `NavDisplay`, `rememberNavBackStack` and the two entry decorators. |
| Feature / movies | `feature/movies/presentation/MoviesScreen.kt` | New stub. Top app bar and an "Open a sample movie" button. |
| Feature / details | `feature/details/presentation/DetailsScreen.kt` | New stub. Top app bar with the auto-mirrored back button. |
| App | `MainActivity.kt` | Hosts `AppNavigation()`. `enableEdgeToEdge()` runs before `super.onCreate`. |
| Resources | `drawable/ic_arrow_back.xml` | New. `android:autoMirrored="true"`. |
| Resources | `values/strings.xml`, `values-ar/strings.xml` | `action_back`, plus three `placeholder_*` strings that are removed with the stubs. |
| Tests | `test/.../core/navigation/RoutesTest.kt` | New. Serialization round trip for both routes. |

## 3. Data flow walkthrough
In Navigation 3 the back stack *is* the navigation state, a list owned by the app:

```kotlin
val backStack = rememberNavBackStack(MoviesRoute)            // saved via kotlinx.serialization

NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },               // system back / predictive back
    entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),      // per-entry rememberSaveable
        rememberViewModelStoreNavEntryDecorator(),           // per-entry ViewModelStore
    ),
    entryProvider = entryProvider {
        entry<MoviesRoute> { MoviesScreen(onMovieClick = { id -> backStack.add(DetailsRoute(id)) }) }
        entry<DetailsRoute> { route -> DetailsScreen(route.movieId, onBack = { backStack.removeLastOrNull() }) }
    },
)
```

Screens don't know about the back stack. They receive plain callbacks (`onMovieClick`, `onBack`), so they stay testable and reusable. In T2.3 the details ViewModel will receive its route through Hilt assisted injection: `hiltViewModel<DetailsViewModel, DetailsViewModel.Factory>(creationCallback = { it.create(route) })`.

## 4. Decisions made, and alternatives considered
- **Navigation 3 instead of Navigation 2 (Compose).** It's the CLAUDE.md default. Owning the back stack as a list makes navigation easy to understand and test, and it supports the adaptive list-detail scenes planned for E7.
- **The ID is the only argument.** `DetailsRoute` carries only the movie ID. The details screen loads everything else from the cache or the network (T2.2), which keeps the saved state small and makes the route usable as a deep link later.
- **Each screen owns its `Scaffold`.** Top bars differ per screen (the search bar on Movies, a collapsing hero on Details), so there's no shared outer Scaffold. `TopAppBar` and `Scaffold` apply the status and navigation bar insets, which covers the edge-to-edge requirement (targetSdk 36 forces edge-to-edge). Deeper inset polish is extra E6.
- **`dropUnlessResumed` on navigation clicks.** A quick double tap during a transition would otherwise push the same route twice.
- **Stubs live in the feature packages.** `MoviesScreen` and `DetailsScreen` will keep the same file and signature shape, and only their bodies change later, so the navigation code won't need edits. The temporary strings are named `placeholder_*` so they're easy to find and remove.
- **Default transitions and predictive back.** `NavDisplay`'s built-in transitions and predictive-back handling are used as they are. Custom motion (shared-element transitions, predictive-back polish) is extra E3 and E6.

## 5. How it is tested
- **`RoutesTest`** (2 tests): both routes survive a JSON serialization round trip. That's what `rememberNavBackStack` relies on to restore the back stack after process death.
- **Build checks:** `assembleDebug` passes, 42 of 42 unit tests pass, and `lintDebug` reports 0 errors (2 deliberate warnings).
- **On a device** (manual):
  1. Tap "Open a sample movie". "Movie #862" opens. Back returns to the list, through both the arrow and the system back gesture.
  2. Rotate on the details screen. It stays on "Movie #862".
  3. **Process death:** open "Movie #862", press Home, run `adb shell am kill com.trianglz_task_ahmedhussin`, and reopen the app from Recents. It comes back on "Movie #862", and Back still returns to the list.
  4. Edge-to-edge: the top bar sits below the status bar, and nothing is hidden behind the navigation bar, in both light and dark mode.
  5. In Arabic the layout is mirrored and the back arrow points right.
- Instrumented navigation tests come in T2.5, after the test infrastructure (T0.10) is in place.

## 6. How to build it from scratch
1. Declare the routes as `@Serializable` objects or data classes implementing `NavKey`, with only the minimal arguments.
2. In a composable, create `rememberNavBackStack(StartRoute)`.
3. Add `NavDisplay(backStack, onBack = { backStack.removeLastOrNull() }, entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()), entryProvider = entryProvider { entry<Route> { … } })`.
4. Give each screen callbacks instead of the back stack, and wrap them with `dropUnlessResumed`.
5. Give each screen its own `Scaffold` and `TopAppBar` to handle insets, and call `enableEdgeToEdge()` before `super.onCreate`.
6. Use an `autoMirrored` vector for directional icons.
7. Test the routes' serialization round trip, then check rotation and `am kill` process death on a device.
