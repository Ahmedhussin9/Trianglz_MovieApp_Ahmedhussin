# Testing

How the app is tested: the strategy, the tools, every test class and what it proves, the patterns that made the tests reliable, the bugs they caught, and what is deliberately not automated.

## 1. Summary

| Suite | Runs on | Tests | Command |
|---|---|---|---|
| Unit | JVM | **181** in 32 classes | `gradlew.bat testDebugUnitTest` |
| Instrumented | Emulator or device | **57** in 10 classes | `gradlew.bat connectedDebugAndroidTest` |

**Strategy.** Most logic is tested on the JVM, where tests are fast: mapping, validation, error handling, paging logic, use cases and ViewModels. The device is used only where the real thing matters:
- **SQLite**, for the DAOs and the `RemoteMediator`'s transactions;
- **Compose rendering and semantics**, for the screens;
- **the real Hilt graph and `NavDisplay`**, for wiring and navigation.

No test calls the real TMDb API, so the suites need no token and never flake on the network.

**Fakes, not mocks.** Every test double is a small hand-written fake: a repository that returns scripted flows, an API that serves scripted pages, an in-memory DAO. Fakes behave like the real contract (they emit, complete and fail the same way), and the tests read as plain Kotlin. MockK is on the test classpath but no test needed it.

## 2. Running the tests

```bat
gradlew.bat testDebugUnitTest
gradlew.bat connectedDebugAndroidTest
gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.trianglz_task_ahmedhussin.feature.movies.presentation.SearchScreenTest
```

- **Instrumented tests:**
  - they need a running emulator or device;
  - they use `HiltTestRunner`, which starts the app with `HiltTestApplication`;
  - they **uninstall the app** when they finish, so run it again from Android Studio afterwards.
- **On a low-memory machine:** add `--max-workers=2`, and do not run both suites and an emulator build at the same time.
- **Reports:**
  - unit: `app/build/reports/tests/testDebugUnitTest/`;
  - instrumented: `app/build/reports/androidTests/connected/`.

## 3. Tools

| Tool | Used for |
|---|---|
| JUnit 4 | All tests |
| `kotlinx-coroutines-test` | `runTest`, virtual time (`advanceTimeBy`, `runCurrent`, `advanceUntilIdle`), `backgroundScope` |
| `MainDispatcherRule` (`app/src/test/.../testing/`) | Replaces `Dispatchers.Main` with a test dispatcher, standard or unconfined per test class |
| Turbine | Asserting `Flow` emissions in order: `test { awaitItem(); awaitComplete() }` |
| MockWebServer (OkHttp) | Real Retrofit, OkHttp and interceptors against a local server: request paths, headers and query parameters, JSON parsing, HTTP errors |
| `paging-testing` | `asSnapshot()` to read `PagingData` as a list; `TestPager` to drive a `PagingSource` page by page |
| Room in-memory database | DAO and `RemoteMediator` tests on the device's real SQLite (`DatabaseTestUtils.inMemoryDatabase()`) |
| Compose UI Test | `createAndroidComposeRule`, semantics finders (`onNodeWithText`, `onNodeWithContentDescription`, `hasSetTextAction()`), actions, `waitUntil` |
| Hilt testing | `@HiltAndroidTest`, `HiltAndroidRule`, `@UninstallModules` with a nested test module |
| DataStore | A real Preferences DataStore in a JUnit `TemporaryFolder` |
| JSON fixtures | `app/src/test/resources/tmdb/*.json`: real-shaped TMDb responses |

## 4. Every test class

### 4.1 Core

| Class | Suite | Tests | What it proves |
|---|---|---|---|
| `TmdbHttpClientFactoryTest` | Unit | 9 | Every request carries the three filter parameters, even if a caller passes others; the Bearer token is sent; `language` is `en-US`, `ar-SA` or the `en-US` fallback, and follows a locale change; HTTP logs never contain the token; JSON parsing is lenient |
| `ErrorMapperTest` | Unit | 15 | Every exception and HTTP status maps to the right `DomainError`: no host, refused, other I/O → `NoInternet`; socket or call timeout → `Timeout` (checked before I/O); 401/403 → `Unauthorized`; 404 → `NotFound`; 429 → `RateLimited`; 5xx → `Server`; others, malformed JSON, unexpected → `Unknown`; `DomainException` is kept as it is |
| `SafeApiCallTest` | Unit | 3 | Success becomes `Success`; failure becomes `Failure(mapped error)`; cancellation is rethrown, not reported |
| `ErrorLoggingTest` | Unit | 5 | A failed call logs one warning with the original exception; success and cancellation log nothing; an existing `DomainException` is not logged twice |
| `TmdbValuesTest` | Unit | 5 | Strict dates (impossible days rejected), years before 1880 ignored, rating clamped to 0–10, blank text becomes `null` |
| `TmdbImageUrlsTest` | Unit | 5 | Image URLs per size; missing or blank paths give `null` |
| `AppResultTest` | Unit | 2 | `map` transforms success and passes failure through |
| `ErrorMessagesTest` | Unit | 1 | Every `DomainError` has its own message |
| `ColorContrastTest` | Unit | 2 | Text and background colour pairs meet WCAG AA in both themes |
| `DispatchersModuleTest` | Unit | 2 | The `@IoDispatcher` and `@DefaultDispatcher` qualifiers provide the right dispatchers |
| `RoutesTest` | Unit | 2 | Navigation keys survive serialization (needed to restore the back stack) |
| `MainDispatcherRuleTest` | Unit | 1 | The rule really replaces `Dispatchers.Main` |
| `AppGraphTest` | Device | 2 | The **production** Hilt graph builds: Retrofit points at TMDb, dispatchers resolve |
| `StatusLayoutTest` | Device | 1 | Regression test from the T4.1 review: in a space too small for an error state, Retry can be scrolled to and tapped |
| `AppNavigationTest` | Device | 4 | End to end with the real `MainActivity`, Hilt, `NavDisplay` and ViewModels, with fake repositories: the list shows; tap opens details; the back button and system back return; each movie opens its own details |

### 4.2 Movies (list and search)

| Class | Suite | Tests | What it proves |
|---|---|---|---|
| `MovieMapperTest` | Unit | 6 | Invalid IDs and blank titles are dropped; titles trimmed; missing poster → `null`; invalid date → no year; rating clamped |
| `MovieEntityMapperTest` | Unit | 1 | Domain ↔ cache entity round trip |
| `MoviesRemoteDataSourceTest` | Unit | 11 | MockWebServer: discover request (path, page, sort, filter); a real-shaped page parsed and validated; duplicates within a page; last page and the 500-page cap; empty results; 401 and malformed JSON propagate; search request (path, `query`, page, all three filters), validation and no matches |
| `SearchPagingSourceTest` | Unit | 8 | `TestPager`: refresh loads page 1; append loads the next; last page has no next key; empty results; a movie repeated on a later page is skipped; `IOException` → `DomainException(NoInternet)`; refresh key is `null`; the repository pages through all results |
| `GetMoviesUseCaseTest` | Unit | 3 | Passes the repository's movies through in order; empty; a new stream per call |
| `SearchQueryValidatorTest` | Unit | 7 | Blank → idle; trimming; whitespace collapsed; one character valid; Arabic kept; capped at 100 with no trailing space |
| `SearchMoviesUseCaseTest` | Unit | 4 | Results passed through; the cleaned query reaches the repository; blank never reaches it; oversized is capped first |
| `ListStatusTest` | Unit | 9 | Every combination of Paging load states → Loading, Content (refreshing / refresh error), Empty, Error; no premature Empty while the network refresh runs; a non-`DomainException` error → `Unknown`; the footer follows `append` |
| `MovieUiTest` | Unit | 6 | Display formatting: year, one-decimal rating, no badge when unrated, locale separators, Arabic-Indic digits for Arabic |
| `MoviesViewModelTest` | Unit | 4 | Movies exposed as display models; a card click sends one navigation effect; the settings button sends one effect; dismissing the notice updates the state |
| `MoviesViewModelSearchTest` | Unit | 8 | Virtual time: the bar updates at once, but searching waits 350 ms; fast typing makes one request; whitespace variants make no extra request; clearing returns without waiting; blank never searches; a new query replaces old results; capped at 100; the logged action never contains the search text |
| `MovieDaoTest` | Device | 5 | The `PagingSource` orders by position, not insertion; a duplicate ID keeps its first row and position; `maxPosition`; `clearAll`; the remote key upsert replaces |
| `MoviesRemoteMediatorTest` | Device | 12 | Start-up asks for an immediate refresh; refresh caches page 1 in API order and stores the next page; refresh replaces the old cache; single-page list reaches the end; failed refresh keeps the cache and reports the `DomainError`; 401 → `Unauthorized`; append loads the stored next page after the cached movies; a repeated movie keeps its place; no network call after the last page or without a key; failed append keeps pages and key for retry; prepend never loads |
| `MoviesRepositoryImplTest` | Device | 3 | The whole `Pager` with real Room: first launch online shows network movies; offline launch shows the cache and reports the error; online launch with a cache ends with the fresh movies |
| `MoviesScreenTest` | Device | 10 | Stateless screen with fixed `PagingData`: loading skeleton, cards with the rating's accessibility label, empty, error with Retry, card click, footer error, cached notice and its dismiss, dismissed notice stays hidden, Retry really reloads (a `PagingSource` that fails once) |
| `SearchScreenTest` | Device | 8 | Typing sends `QueryChanged`; ✕ hidden when empty, shown with text and sends `ClearQuery`; list vs results by `isSearching`; no-match empty state shows the query; search error Retry reloads; tapping a result sends `MovieClicked` |

### 4.3 Details

| Class | Suite | Tests | What it proves |
|---|---|---|---|
| `MovieDetailsMapperTest` | Unit | 13 | Every field mapped; ID and title required; text trimmed; missing images; invalid date; runtime 0 → `null`; rating and vote limits; invalid and duplicate genres dropped; language lower-cased; every status value; entity round trip; unknown stored status → `Unknown` |
| `MovieDetailsRemoteDataSourceTest` | Unit | 6 | MockWebServer: path, auth and language; a real-shaped fixture parsed field by field; 404 → `NotFound`; 401 → `Unauthorized`; blank title → `Unknown`; malformed JSON → `Unknown` |
| `MovieDetailsRepositoryImplTest` | Unit | 8 | All four cache/network combinations (and what ends up in the cache); cache read per movie; an unreadable cache still serves the network; a failed save still emits; a new refresh per collection |
| `GetMovieDetailsUseCaseTest` | Unit | 6 | Passes cached-then-fresh and failures through in order; a non-positive ID → `NotFound` without asking the repository |
| `MovieDetailsUiTest` | Unit | 7 | Locale formatting (US, German, Arabic language name, Arabic-Indic date digits); no rating text for no votes; unknown language → `null`; raw values passed through |
| `DetailsViewModelTest` | Unit | 11 | Virtual time: Loading; network → Content; cached then fresh (with `isRefreshing`); failed refresh keeps content and sends one snackbar effect; error → Retry → Content; Retry with content keeps it visible; Retry during a load is ignored; invalid ID → `NotFound`; synopsis toggle; Back effect; locale from the provider |
| `MovieDetailsDaoTest` | Device | 5 | Missing row → `null`; full round trip including the date and genre converters; null date and empty genres; upsert replaces; rows per movie |
| `DetailsScreenTest` | Device | 7 | Loading; content (info row, genres, the combined rating description); Read more sends the toggle; expanded shows Show less; missing synopsis message; error Retry; back button |

### 4.4 Settings

| Class | Suite | Tests | What it proves |
|---|---|---|---|
| `SettingsViewModelTest` | Unit | 5 | State from the saved theme and current language; selecting a theme or language applies it and the state follows; re-selecting the current language does nothing; Back effect |
| `ThemeModeDataSourceTest` | Unit | 3 | Real DataStore: default System; saved value read back; unknown stored value → System |
| `SettingsMappingTest` | Unit | 3 | Language tags ↔ `AppLanguage` (including `ar-EG` and lists); `ThemeMode` → AppCompat night modes |

## 5. Patterns that make the tests reliable

- **Virtual time for anything time-based.** Debounce and delayed fake responses are tested with a standard test dispatcher and `advanceTimeBy` / `runCurrent`, so a 350 ms debounce takes no real time and the in-between states can be asserted.
- **Unconfined vs standard dispatcher, chosen per class.** Classes that only need work to "just run" (`MoviesViewModelTest`, `SettingsViewModelTest`) use `UnconfinedTestDispatcher`. Classes that check in-between states use the standard one.
- **Collect what the screen collects.** Search runs only while `searchResults` is collected, so `MoviesViewModelSearchTest` collects it in `backgroundScope`, exactly as the screen would.
- **`asSnapshot()` on one `PagingData`.** A `cachedIn` flow never completes, so the tests take its first emission and snapshot that (`flowOf(viewModel.movies.first()).asSnapshot()`).
- **Always give load states to `PagingData.from()`.** Without them `LazyPagingItems` keeps its initial refresh = Loading, and an empty list shows the skeleton instead of Empty. This was found in T1.6 and is now built into the shared `loadStates()` helper (`androidTest/.../testing/PagingTestUtils.kt`).
- **Find nodes the way users do.** Screen tests look elements up by their string resource or content description (so they work in any language), not by test tags. `hasSetTextAction()` finds the search field itself.
- **Test Retry by its effect.** Paged Retry calls `LazyPagingItems.retry()`, so a `FailOnceThenSucceedPagingSource` proves the button really reloads.
- **Fixed locale.** Formatting tests pass `Locale.US`, `Locale.GERMANY` or `ar` explicitly; `DetailsViewModel` takes an injected `LocaleProvider`.
- **Swap the graph for one test class only.** `AppNavigationTest` uses `@UninstallModules` with a nested module, so `AppGraphTest` still checks the production bindings.
- **Real-shaped data.** JSON fixtures copy TMDb's real responses, including fields the app ignores.

## 6. Bugs the tests caught

| Found by | Bug | Fix |
|---|---|---|
| `MoviesScreenTest` (T1.6) | Fake `PagingData` without load states looked like it was still loading | Explicit load states, now a shared helper |
| `DetailsScreenTest` (T2.4) | When the screen opened with the synopsis already expanded (after rotation), "Show less" never appeared | Measure "longer than four lines" in both states |
| `MovieDetailsRepositoryImplTest` (T2.2) | A test design error: the second collection emits the cached copy too | The test collects whole runs with `toList()` |
| `SearchScreenTest` compilation (T3.5) | Two private helper classes with the same name in one package clashed | Shared `PagingTestUtils.kt` |
| The release test run (T4.5), on a heavily loaded emulator | `MoviesRepositoryImplTest` was flaky: `asSnapshot()` could return between the mediator's success and Room's background invalidation, so it saw the cached row | The test now waits, in real time with a timeout, for the fresh rows, as a collecting UI does. The app was never affected |
| `StatusLayoutTest` (T4.1, written after the bug was seen on a device) | Error states could not scroll, so Retry was unreachable with large text in landscape | Scrollable, still-centred layout |

## 7. What is not automated, and why

| Not automated | Why | How it is covered instead |
|---|---|---|
| Real TMDb calls | Needs a token, flaky, and the data changes | MockWebServer with real-shaped fixtures; device checks in each task |
| Rate limit (429) and server errors (5xx) on a device | Cannot be triggered on demand | `ErrorMapperTest`; the UI shows the same `ErrorView` for every error |
| Timeout end to end | Needs network shaping | `ErrorMapperTest`; device check with the emulator's network delay (T4.1) |
| Visual appearance (colours, layout, animations) | Pixel tests are brittle; screenshot tests are extra E15 | Previews (light, dark, RTL), `ColorContrastTest`, device reviews |
| Per-app language on Android 13+ | Requires a system-level language change on a newer device | Device checks; the digit-style item is still open (see `docs/features/28-quality-review.md`) |
| Process death | Needs "Don't keep activities" or `adb shell am kill` | Navigation keys' serialization (`RoutesTest`); the behavior is documented in `WALKTHROUGHS.md` §11 |

The manual device checklist for errors, accessibility, large text, landscape and RTL is in [`docs/features/28-quality-review.md`](features/28-quality-review.md).
