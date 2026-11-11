# 0007: Navigation 3

**Status:** Accepted (planning; implemented in T0.9, extended in T2.4 and T2.6)

## Context
The app has three destinations: the movie list (start), a movie's details (needs the movie ID), and settings. The back stack must survive rotation and process death, each details entry needs its own ViewModel built with its movie ID through Hilt, and the UI is all Compose.

## Decision
Use **Navigation 3**:
- **Destinations are `@Serializable` keys:** `MoviesRoute`, `DetailsRoute(movieId)` and `SettingsRoute`.
- **The back stack is a saveable list:** `rememberNavBackStack`, shown by `NavDisplay`.
- **Every entry gets its own ViewModel store** (`rememberViewModelStoreNavEntryDecorator`). `DetailsViewModel` receives the movie ID through Hilt assisted injection: `hiltViewModel(creationCallback = { it.create(movieId) })`.
- **Navigating is a plain list operation.** It is `backStack.add(route)`, and Back removes the exact entry (`backStack.remove(route)`), so a double tap cannot pop the list as well.

## Alternatives considered
- **Navigation Compose (Navigation 2) with type-safe routes.** Mature and familiar, but the back stack is owned by the `NavController` and arguments reach ViewModels through `SavedStateHandle`. Navigation 3 makes the back stack ordinary state the app owns, which is simpler to reason about and test.
- **Manual navigation with a `mutableStateListOf` of screens.** It would need hand-written saving, ViewModel scoping per entry, and predictive back.

## Consequences
- **A route's arguments are just constructor values**, so the ViewModel's ID is a non-null `val` and tests call the constructor directly.
- **The end-to-end navigation test (T2.5) uses the real `NavDisplay`** with fake repositories.
- **Adaptive layouts can come later without restructuring.** List–detail two-pane (extra E7) can use Navigation 3 scenes on the same back stack.
