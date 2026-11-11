# 22: List to details navigation, end-to-end UI test (T2.5)

## 1. What was built
`AppNavigationTest`, a Hilt instrumented test (R5, R30) that runs the real app: `MainActivity`, the Hilt graph, Navigation 3, `MoviesViewModel`, `DetailsViewModel` and both screens. Only the two repositories are replaced, by in-memory fakes. It needs no network, no database and no TMDb token, and gives the same result on every run.

| Test | What it proves |
|---|---|
| `listShowsTheMovies` | The start destination shows the repository's movies through the real ViewModel and grid. |
| `tappingAMovieOpensItsDetailsAndTheBackButtonReturnsToTheList` | A card tap navigates to `DetailsRoute(id)`, the details load, and the top bar's back button returns to the list. |
| `systemBackFromDetailsReturnsToTheList` | The system back gesture/key pops the details entry too. |
| `eachMovieOpensItsOwnDetails` | Opening a second movie after going back shows *its* details, not the first one's (each `DetailsRoute(id)` gets its own `DetailsViewModel` with the right ID). |

This closes Phase 2: T2.1–T2.6 are done.

## 2. Files created or changed

| Layer | File | Change |
|-------|------|--------|
| Tests | `androidTest/.../core/navigation/AppNavigationTest.kt` | New: 4 tests and the nested fake-repository module |
| Docs | `docs/PLAN.md` | T2.5 ticked; tracker moved to the Phase 2 PR and Phase 3 |

No production code changed.

## 3. Data flow walkthrough
In the test, everything below the repositories is replaced:

```
MainActivity ─▶ AppNavigation (NavDisplay)
  MoviesRoute ─▶ MoviesViewModel ─▶ GetMoviesUseCase ─▶ [fake MoviesRepository: PagingData.from(2 movies)]
     tap card ─▶ MoviesEffect.NavigateToDetails(id) ─▶ backStack.add(DetailsRoute(id))
  MovieDetailsRoute ─▶ DetailsViewModel(id) ─▶ GetMovieDetailsUseCase ─▶ [fake MovieDetailsRepository: Success(details of id)]
     back ─▶ DetailsEffect.NavigateBack ─▶ backStack.remove(route)
```

The fakes are installed for this class only:

```kotlin
@HiltAndroidTest
@UninstallModules(MoviesModule::class, DetailsModule::class)
class AppNavigationTest {
    @Module
    @InstallIn(SingletonComponent::class)
    object FakeRepositoriesModule {
        @Provides fun provideMoviesRepository(): MoviesRepository = …
        @Provides fun provideMovieDetailsRepository(): MovieDetailsRepository = …
    }
}
```

Loads finish asynchronously (Paging, ViewModel coroutines), so each step waits for its text before asserting:

```kotlin
composeRule.waitUntil(timeoutMillis = WAIT_MS) {
    composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
}
```

## 4. Decisions made, and alternatives considered
- **`@UninstallModules` and a nested module, not `@TestInstallIn`.** `@TestInstallIn` replaces a module for *every* Hilt test in the source set, which would also put fakes under `AppGraphTest`, whose job is to check the production graph. The class-scoped replacement keeps that test meaningful.
- **Fake repositories, not a fake API and an in-memory database.** This test checks navigation and wiring. The data path (Retrofit, RemoteMediator, Room, caching) already has its own unit and DAO tests, and repeating it here would make the test slower and flakier without adding coverage.
- **Details are recognized by their tagline, not the title.** The title appears twice on the details screen (the header and the top bar, where it is transparent until scrolled), while the tagline appears once.
- **Both back paths are tested.** The top bar button goes through the MVI effect (`Back` → `NavigateBack` → `backStack.remove(route)`), while system back goes through `NavDisplay`'s `onBack`. They are different code paths.
- **Explicit load states on the fake `PagingData`.** As learned in T1.6, without them the list keeps its initial Loading state.

## 5. How it is tested
- `AppNavigationTest`: 4/4 pass on a Pixel 4 emulator (API 29), and `AppGraphTest` still passes with production bindings.
- The full instrumented suite passes 48/48, and the full unit suite passes 151/151 (`connectedDebugAndroidTest`, `testDebugUnitTest`, `assembleDebug`).

## 6. How to build it from scratch
1. Create `AppNavigationTest` with `@HiltAndroidTest`, a `HiltAndroidRule` (order 0) and `createAndroidComposeRule<MainActivity>()` (order 1).
2. Add `@UninstallModules(MoviesModule::class, DetailsModule::class)` and a nested `@Module @InstallIn(SingletonComponent::class)` object providing fake `MoviesRepository` and `MovieDetailsRepository` implementations.
3. Make the fake list return `PagingData.from(movies, sourceLoadStates = …)`, and the fake details return `Success` for known IDs and `Failure(NotFound)` otherwise.
4. Write a `waitForText` helper with `composeRule.waitUntil`.
5. Write the four flows: the list shows, tap → details → back button, tap → details → `Espresso.pressBack()`, and a second movie shows its own details.
6. Run `gradlew.bat connectedDebugAndroidTest` with the class filter, then the full suite.
