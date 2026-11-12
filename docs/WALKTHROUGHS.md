# Walkthroughs

What actually happens, step by step and class by class, in each main scenario. [`ARCHITECTURE.md`](ARCHITECTURE.md) gives the overall structure; this document follows the code at run time. File paths are relative to `app/src/main/java/com/trianglz_task_ahmedhussin/`.

1. [App start](#1-app-start)
2. [First launch online: the list appears](#2-first-launch-online-the-list-appears)
3. [Launch with a cache: online and offline](#3-launch-with-a-cache-online-and-offline)
4. [Scrolling: the next page](#4-scrolling-the-next-page)
5. [Opening a movie](#5-opening-a-movie)
6. [Every Retry button](#6-every-retry-button)
7. [Searching](#7-searching)
8. [Changing the language](#8-changing-the-language)
9. [Changing the theme](#9-changing-the-theme)
10. [Back navigation](#10-back-navigation)
11. [Rotation and process death](#11-rotation-and-process-death)
12. [One request, from the button to TMDb and back](#12-one-request-from-the-button-to-tmdb-and-back)

---

## 1. App start

1. **`MovieApplication.onCreate()`** (`MovieApplication.kt`)
   - Hilt builds the singleton graph (`@HiltAndroidApp`).
   - Debug builds plant `Timber.DebugTree`; release builds plant nothing, so no logs are written.
   - `applyThemeMode()` reads the saved theme **once, blocking** (`runBlocking { observeThemeMode().first() }`) and applies it with `NightModeApplier.apply` → `AppCompatDelegate.setDefaultNightMode`. It then keeps observing for changes on a `MainScope`. Reading before any activity exists is why the first frame already has the right theme.
   - Coil's image loader is configured lazily through `SingletonImageLoader.Factory.newImageLoader`, with a 300 ms crossfade and default memory and disk caches.
2. **`MainActivity.onCreate()`** (`MainActivity.kt`)
   - `enableEdgeToEdge()`, then `super.onCreate()`. `MainActivity` is an `AppCompatActivity`, which per-app language and night mode need.
   - `syncDefaultLocaleWithResources()` makes `Locale.getDefault()` equal to the activity's resource locale, so numbers formatted in code and the TMDb `language` parameter match the translated strings.
   - `setContent { MovieAppTheme { AppNavigation() } }`.
3. **`AppNavigation`** (`core/navigation/AppNavigation.kt`)
   - `rememberNavBackStack(MoviesRoute)` creates a saveable back stack that starts at the list.
   - `NavDisplay` shows the top entry, with two decorators:
     - `rememberSaveableStateHolderNavEntryDecorator()` keeps each entry's `rememberSaveable` state;
     - `rememberViewModelStoreNavEntryDecorator()` gives each entry its own ViewModels.
   - The `MoviesRoute` entry composes `MoviesRoute(onMovieClick, onSettingsClick)` (`feature/movies/presentation/MoviesScreen.kt`).

## 2. First launch online: the list appears

1. **`MoviesRoute`** gets `MoviesViewModel` with `hiltViewModel()` and collects:
   - `state` with `collectAsStateWithLifecycle()`;
   - `movies` and `searchResults` with `collectAsLazyPagingItems()`.
2. **`MoviesViewModel.movies`** is built once, in the ViewModel:
   ```
   GetMoviesUseCase() → MoviesRepository.popularMovies()       (domain interface)
     → MoviesRepositoryImpl.popularMovies()                     (data)
        Pager(PagingConfig(pageSize = 20),
              remoteMediator = MoviesRemoteMediator(dataSource, database),
              pagingSourceFactory = { database.movieDao().pagingSource() })   // SELECT … ORDER BY position
        .flow.map { it.map(MovieEntity::toDomain) }
   → .map { it.map(Movie::toUi) } → .cachedIn(viewModelScope)
   ```
3. **Paging starts two things at once:**
   - **Reading Room.** `MovieDao.pagingSource()` returns nothing, because the database is empty.
   - **The mediator.** `MoviesRemoteMediator.initialize()` returns `LAUNCH_INITIAL_REFRESH`, so `load(REFRESH)` runs immediately.
4. **`MoviesRemoteMediator.load(REFRESH)`**
   1. Page = 1.
   2. `MoviesRemoteDataSource.discoverMovies(1)` → `MoviesApi.discoverMovies(page = 1, sortBy = "popularity.desc")`. Retrofit builds `GET discover/movie`, and the OkHttp interceptors add the token, the filters and the language ([§12](#12-one-request-from-the-button-to-tmdb-and-back)).
   3. The JSON is parsed into `MoviesPageDto`. `toMoviesPage()` then:
      - drops invalid items (`MovieDto.toDomainOrNull()`: no ID, blank title);
      - removes duplicates within the page;
      - computes `isLastPage = page >= min(total_pages, 500)`.
   4. One Room transaction (`database.withTransaction`):
      - `clearAll()` (it is a refresh);
      - `insertAll()` with `position` starting after `maxPosition()`;
      - `remoteKeyDao.upsert(RemoteKeyEntity("popular", nextPage = 2))`.
   5. Returns `MediatorResult.Success(endOfPaginationReached = isLastPage)`.
5. **Room invalidates the `PagingSource`** because the tables changed. Paging creates a new one, reads the 20 rows, and emits new `PagingData` to `LazyPagingItems`.
6. **`MoviesScreen`** computes what to show:
   - `status = items.loadState.toListStatus(items.itemCount)` (`feature/movies/presentation/ListStatus.kt`);
   - while the first refresh runs with no rows, `status` is **Loading**, so `MovieGridSkeleton` shows;
   - with rows, it is **Content**, and `AnimatedContent` crossfades to `MovieGrid` (animation A4);
   - each `MovieCard` gets a `MovieUi`: title, year in the locale's digits, and a rating with one decimal, or no badge if nobody voted.

## 3. Launch with a cache: online and offline

**Online with a cache:**
- Room's `PagingSource` returns the cached rows straight away, so the **cached list shows at once** (R8, R9).
- The mediator's refresh runs in parallel (`LAUNCH_INITIAL_REFRESH`). `toListStatus` sees items plus `mediator.refresh = Loading`, which gives `Content(isRefreshing = true)`: the grid stays and a thin `LinearProgressIndicator` shows on top.
- When the refresh succeeds, the transaction replaces the rows, Room invalidates, and the **fresh list replaces the cached one** (R10). The scroll position is kept because items are keyed by movie ID.

**Offline with a cache:**
1. The cached list shows the same way.
2. `MoviesApi.discoverMovies` throws an `IOException` (`UnknownHostException`).
3. The mediator catches it: `MediatorResult.Error(e.toDomainException())` (`core/network/error/ErrorMapper.kt`). `toDomainException()` logs the original exception once and wraps `DomainError.NoInternet`.
4. Nothing was deleted (the transaction never ran), so the rows stay.
5. `toListStatus` sees items plus `mediator.refresh = Error`, which gives `Content(refreshError = NoInternet)`, so **`CachedDataNotice`** appears: "Couldn't refresh. Showing saved movies.", with Retry and Dismiss. Dismiss sends `MoviesUiAction.DismissCachedNotice`, and the ViewModel sets `isCachedNoticeDismissed = true` for the session.

**Offline without a cache:**
- No items plus a failed refresh gives `ListStatus.Error(NoInternet)`, so the full-screen **`ErrorView`** shows with the message from `DomainError.messageRes()` and a Retry button.

## 4. Scrolling: the next page

1. `MovieGrid` (`feature/movies/presentation/component/MovieGrid.kt`) reads `movies[index]` for each visible card. Reading near the end of the loaded items tells Paging to prefetch (the prefetch distance defaults to the page size, 20).
2. Room's `PagingSource` has no more rows, so Paging asks the mediator: **`load(APPEND)`**.
3. The page comes from `remoteKeyDao.get("popular")?.nextPage`.
   - If there is no key or `nextPage` is `null`, the result is `Success(endOfPaginationReached = true)` and no network call is made.
4. The data source fetches that page. The transaction inserts the movies **after** `maxPosition()` (without `clearAll`) and stores the new next page.
   - `MovieDao.insertAll` uses `OnConflictStrategy.IGNORE`: a movie that TMDb repeats on a later page keeps its first position instead of jumping down the list.
5. Room invalidates, and the new rows appear under the old ones.
6. **The footer** (`PagingFooter`) follows `loadState.toFooterStatus()`:
   - `Loading` shows a spinner;
   - `Error` shows the message and Retry (R4);
   - `NotLoading` shows nothing.

   It switches between them with a crossfade (A9).
7. TMDb reports up to 1001 pages but refuses pages above 500, so `isLastPage` caps at 500 and appending ends cleanly.

## 5. Opening a movie

1. **Tap.** `MovieCard`'s `onClick` → `MoviesUiAction.MovieClicked(id)` → `MoviesViewModel.onAction` sends `MoviesEffect.NavigateToDetails(id)` through its `Channel`.
2. **`MoviesRoute`** collects effects only while `STARTED` (`repeatOnLifecycle`) and calls `onMovieClick(id)`. `AppNavigation` runs `backStack.add(DetailsRoute(id))`.
3. **`NavDisplay`** shows the new entry → `MovieDetailsRoute(movieId, onBack)` (`feature/details/presentation/DetailsScreen.kt`).
   - `hiltViewModel<DetailsViewModel, DetailsViewModel.Factory>(creationCallback = { it.create(movieId) })` builds the ViewModel through **assisted injection**. Its ViewModel store belongs to this entry, so every `DetailsRoute(id)` gets its own instance.
4. **`DetailsViewModel.init` → `load()`**
   1. The state becomes `DetailsStatus.Loading` (or stays `Content` with `isRefreshing = true` on a retry).
   2. `GetMovieDetailsUseCase(movieId)`: a non-positive ID fails at once with `NotFound`. Otherwise it calls `MovieDetailsRepository.movieDetails(id)`.
5. **`MovieDetailsRepositoryImpl.movieDetails(id)`** (`feature/details/data/repository/`) is a cold `flow { }`:
   1. `readCache(id)`: `MovieDetailsDao.get(id)?.toDomain()`. If a row exists, it **emits `Success(cached)`**. A read error is logged and treated as no cache.
   2. `MovieDetailsRemoteDataSource.movieDetails(id)` runs `safeApiCall { api.movieDetails(id) }` (`GET movie/{id}`). Exceptions become `AppResult.Failure(DomainError)`. A response without a usable title becomes `Failure(Unknown)`.
   3. On success, `writeCache` upserts the new row (a failed save is logged but does not block).
   4. It **emits the network result**, then the flow **completes**.
6. **The ViewModel collects:**

   | Emission | New state |
   |---|---|
   | `Success(details)` | `status = Content(details.toUi(localeProvider.current()))`, `isRefreshing = true` |
   | `Failure(error)` with content already shown | content stays; the `DetailsEffect.ShowRefreshError(error)` effect is sent |
   | `Failure(error)` with nothing shown | `status = Error(error)` |
   | flow completes | `isRefreshing = false` |

7. **`MovieDetailsRoute`** turns `ShowRefreshError` into a snackbar with Retry. If the user taps it, the route sends `DetailsUiAction.Retry`.
8. **`DetailsScreen`** renders the state:
   - a skeleton while loading;
   - `ErrorView` on error;
   - otherwise `DetailsContent`: `DetailsHeader` (backdrop with parallax, poster, title), `InfoRow`, `GenreChips`, `RatingSection` (an animated ring) and `Synopsis` (Read more).
   - `DetailsTopBar` sits on top: transparent over the image, solid after scrolling.

| Saved copy? | Network | What the user sees |
|---|---|---|
| no | ok | skeleton → details |
| no | fails | skeleton → full error with Retry |
| yes | ok | saved details at once (thin progress bar) → fresh details |
| yes | fails | saved details stay + snackbar with Retry |

## 6. Every Retry button

| Where | Calls | What reloads |
|---|---|---|
| List full-screen error | `movies.retry()` (`LazyPagingItems`) | Paging re-runs the failed load, here the mediator's REFRESH |
| "Showing saved movies" notice | `movies.retry()` | Same refresh; the notice disappears once it succeeds |
| List footer | `movies.retry()` | The failed APPEND (next page) |
| Search error or search footer | `searchResults.retry()` | The failed `SearchPagingSource` load |
| Details full-screen error | `DetailsUiAction.Retry` → `DetailsViewModel.load()` | A new collection of the repository flow (cache, then network) |
| Details snackbar | `DetailsUiAction.Retry` | Same; the content stays visible with `isRefreshing = true` |

- Paged lists retry through Paging, not through an action, because Paging owns that load state ([ADR 0002](adr/0002-pagingdata-outside-uistate.md)).
- `DetailsViewModel.load()` ignores a retry while a load is still running, so a double tap never sends two requests.

## 7. Searching

1. **Typing.** `MovieSearchBar`'s `TextField` (`feature/movies/presentation/component/MovieSearchBar.kt`) calls `onAction(QueryChanged(text))`. The ViewModel stores `state.query = text.take(100)`, so the field updates **instantly**.
2. **`MoviesViewModel.activeQuery`** decides what is actually searched:
   ```
   _state.map { it.query }.distinctUntilChanged()
     .debounce { text -> if (blank) 0 ms else 350 ms }   // wait for the user to stop typing; clear instantly
     .map(SearchQueryValidator::validate)                  // trim, collapse spaces → Idle | Valid(text)
     .distinctUntilChanged()                               // "coco" and "coco " are the same search
     .stateIn(viewModelScope, Eagerly, Idle)
   ```
   An `init` collector sets `state.isSearching = activeQuery is Valid`. Because it follows the **debounced** query, the popular list stays on screen while the user is still typing.
3. **`searchResults`** = `activeQuery.flatMapLatest { SearchMoviesUseCase(query) }.map(toUi).cachedIn(viewModelScope)`.
   - `flatMapLatest` **cancels the previous search** when the query changes.
   - `SearchMoviesUseCase` validates again. Idle returns `PagingData.empty(NotLoading)` and never calls TMDb. Valid calls `MovieSearchRepositoryImpl.searchMovies(text)`, which creates `Pager { SearchPagingSource(dataSource, text) }`.
4. **`SearchPagingSource.load(page)`** (`feature/movies/data/remote/SearchPagingSource.kt`):
   1. `MoviesRemoteDataSource.searchMovies(text, page)` → `GET search/movie?query=…&page=…`.
   2. The same `toMoviesPage()` validation as the list runs.
   3. Movies already returned on an earlier page are filtered out (`shownIds`), because the grid keys items by ID and a repeat would crash it.
   4. The result is `LoadResult.Page(data, prevKey = null, nextKey = page + 1 | null)`; a failure becomes `LoadResult.Error(DomainException)`.
5. **`MoviesScreen`**:
   - `items = if (isSearching) searchResults else movies`, each with its own `LazyGridState`;
   - the same `toListStatus` drives Loading, Empty ("No results for “…”"), Error and Content;
   - result cards animate into place (`animateItem`, A3).

   Three helpers handle the edges:
   - `HideKeyboardWhileScrolling` hides the keyboard as soon as the grid scrolls;
   - `ScrollResultsToTopOnNewSearch` scrolls back to the top when a new query's refresh starts;
   - a `LaunchedEffect(isSearching)` re-shows the collapsed app bar when switching lists.
6. **Clearing** (the ✕ button, `ClearQuery`) sets the query to `""`. The debounce selector returns 0 ms, so `isSearching` turns false at once and the popular list returns at its old scroll position.

Search results are network-only and never cached. TMDb ignores the certification filter on search ([ADR 0004](adr/0004-search-certification-filter.md)).

## 8. Changing the language

1. On Settings (`feature/settings/presentation/SettingsScreen.kt`), tapping a row sends `SettingsUiAction.LanguageSelected(Arabic)`.
2. **`SettingsViewModel.onAction`** updates its `language` `MutableStateFlow`, then calls `SetAppLanguageUseCase` → `SettingsRepositoryImpl` → `AppLanguageDataSource.setAppLanguage` → `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ar"))`.
   - Android 13+ stores the choice itself. Older versions store it through the `AppLocalesMetadataHolderService` declared in the manifest.
3. **AppCompat recreates the activity in Arabic.**
   - `MainActivity.onCreate` runs `syncDefaultLocaleWithResources()` again, so `Locale.getDefault()` becomes Arabic.
   - The layout direction becomes RTL, and strings come from `values-ar`.
   - The ViewModels survive (it is a configuration change), so Settings still shows the selection.
4. **Back on the list**, `MoviesRoute` compares the current language with `listLanguage`, which was saved with `rememberSaveable` when the list was loaded. They differ, so it calls `movies.refresh()` and `searchResults.refresh()`.
5. **The refresh request is in Arabic.** `LanguageInterceptor` reads `LocaleProvider.current()` (`Locale.getDefault()`, provided in `NetworkModule`) and sends `language=ar-SA`, so TMDb returns Arabic titles, synopses and genres (requirements Q14).
6. **Details refresh on their own.** Every time a movie opens, it shows the saved copy first, then the fresh one in the new language.

## 9. Changing the theme

1. On Settings, `ThemeSelected(Dark)` → `SettingsViewModel` → `SetThemeModeUseCase` → `ThemeModeDataSource.setThemeMode` writes `"Dark"` to Preferences DataStore under the key `theme_mode`.
2. **DataStore emits.** `MovieApplication`'s observer (`observeThemeMode().onEach(NightModeApplier::apply)`) calls `AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)`.
3. **AppCompat changes the configuration's `uiMode` and recreates the activity.** Everything follows it:
   - `isSystemInDarkTheme()` in `MovieAppTheme`;
   - `values-night` resources such as the window background;
   - `enableEdgeToEdge()`'s status-bar icon colours.
4. **Next launch.** The blocking first read in `MovieApplication.onCreate` applies "Dark" before the first frame.

## 10. Back navigation

| From | How | What happens |
|---|---|---|
| Details | Top-bar back button | `DetailsUiAction.Back` → `DetailsEffect.NavigateBack` → `onBack` → `backStack.remove(route)` (removes this exact entry, so a double tap cannot pop the list too) |
| Details or Settings | System back gesture or key | `NavDisplay(onBack = { backStack.removeLastOrNull() })` |
| Settings | Top-bar back button | `SettingsEffect.NavigateBack` → `backStack.remove(SettingsRoute)` |
| Movies (root) | System back | The app closes, as usual |

When an entry leaves the back stack, its ViewModel store is cleared, so that `DetailsViewModel` is destroyed.

## 11. Rotation and process death

| | Rotation, language or theme change (activity recreated) | Process death (app killed in the background, then restored) |
|---|---|---|
| Back stack | Kept (`rememberNavBackStack` is saveable) | Restored: route keys are `@Serializable` |
| ViewModels | **Kept** (ViewModel stores survive configuration changes) | **New instances** |
| Popular list | Kept in memory (`cachedIn`); scroll position kept (`rememberLazyGridState` is saveable) | Reloaded from the Room cache, then refreshed |
| Search text and results | Kept (state in the ViewModel) | **Lost**: the ViewModel uses no `SavedStateHandle`, so the search bar comes back empty (a known limitation) |
| Open details screen | Kept, with the expanded synopsis | Reloaded from the cache, then refreshed; "Read more" collapses |
| "Showing saved movies" dismissed | Kept | Shown again (session-only by design) |
| Language and theme | Unchanged | Restored from AndroidX locales and DataStore |

## 12. One request, from the button to TMDb and back

Taking the details request for movie 862 as the example:

```
DetailsViewModel.load()
 → GetMovieDetailsUseCase(862)
 → MovieDetailsRepositoryImpl.movieDetails(862)                 (flow)
 → MovieDetailsRemoteDataSource.movieDetails(862)
 → safeApiCall { MovieDetailsApi.movieDetails(862) }             Retrofit: GET https://api.themoviedb.org/3/movie/862
   OkHttp interceptors, in order (TmdbHttpClientFactory.okHttpClient):
     1. AuthInterceptor           Authorization: Bearer <token from BuildConfig>, Accept: application/json
     2. ContentFilterInterceptor  ?include_adult=false&certification_country=US&certification.lte=PG-13
     3. LanguageInterceptor       &language=en-US | ar-SA   (from Locale.getDefault())
     4. HttpLoggingInterceptor    debug builds only, Authorization redacted, logged through Timber (tag TmdbHttp)
   Timeouts: connect 15 s, read 20 s
 ← JSON → kotlinx.serialization (lenient: unknown keys ignored, nulls → defaults) → MovieDetailsDto
 ← MovieDetailsDto.toDomainOrNull()                               validation, image URLs (TmdbImageUrls), dates (TmdbValues)
 ← AppResult.Success(MovieDetails)   or on any exception: toDomainError() → AppResult.Failure(DomainError), logged once
```

The token comes from `local.properties` (or a Gradle property or environment variable) through `BuildConfig.TMDB_ACCESS_TOKEN`, provided by `NetworkModule`.
