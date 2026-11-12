# Design questions and answers

Questions a reviewer is likely to ask about this codebase, with short answers and pointers to where the details are. See also [`ARCHITECTURE.md`](ARCHITECTURE.md), [`WALKTHROUGHS.md`](WALKTHROUGHS.md), [`TESTING.md`](TESTING.md) and the [decision records](adr/README.md).

## Architecture

**Why Clean Architecture for an app this size?**
- The task asks for it (R16).
- It pays off even here:
  - the domain layer is plain Kotlin, so use cases and ViewModels are tested on the JVM with simple fakes;
  - Retrofit and Room never reach the ViewModels;
  - the cache could be swapped (for example, details moved to a `RemoteMediator`) without touching the UI.
- The cost is a few extra types per feature (interface, use case, mapper).

**Is the domain layer really pure Kotlin? It uses `PagingData`.**
- Its only third-party imports are Kotlin coroutines, `java.time`, `javax.inject` and Paging's `PagingData` / `LoadState`.
- Those Paging types come from `paging-common`, a plain Kotlin library with no Android dependency.
- Exposing `PagingData` from the domain is the standard trade-off with Paging 3. The alternative is reimplementing pagination in the domain, which costs more than it gains.

**Some use cases only call the repository. Why keep them?**
- They are the single entry point the ViewModels depend on.
- Rules have one obvious home: `SearchMoviesUseCase` validates the query, `GetMovieDetailsUseCase` rejects invalid IDs.
- A pass-through use case costs three lines and keeps the dependency direction uniform.

**Is the code "modular" if it is a single Gradle module?**
- The boundaries are in the code: one package per feature with its own data, domain, presentation and DI; a pure domain; shared code only in `core/`.
- A multi-module split is planned as extra E16. It would be mechanical because of this structure. See [ADR 0005](adr/0005-single-module.md) and [0006](adr/0006-package-by-feature.md).

## MVI and state

**How is MVI implemented?**
- Each screen has one immutable `UiState` (`StateFlow`), a sealed `UiAction` handled by a single `onAction()`, and a sealed `Effect` for one-time events.
- Screens are split into a stateful `…Route` and a stateless `…Screen(state, onAction)`. See [ADR 0001](adr/0001-mvi-naming.md).

**Why a `Channel` for effects, not a `SharedFlow`?**
- `Channel(BUFFERED).receiveAsFlow()` delivers each event once, to one collector.
- An event sent while the screen is not collecting (for example during rotation) waits in the buffer instead of being lost.
- It is never replayed after rotation, which a `StateFlow` or a replaying `SharedFlow` could do.
- Screens collect effects only while `STARTED`, so navigation never fires in the background.

**Why are the paged lists not part of the `UiState`?**
- `PagingData` is a stream with its own load states, collected once by `collectAsLazyPagingItems()`; it is not a value to copy into state.
- The lists are separate cached flows next to the state, and one tested function (`toListStatus`) turns their load states into Loading, Empty, Error or Content. See [ADR 0002](adr/0002-pagingdata-outside-uistate.md).

**Why does Retry on lists bypass `onAction`?**
- Paging owns the load state, and `LazyPagingItems.retry()` re-runs exactly the failed load (refresh or next page).
- A `Retry` action could only re-create the pager, which would lose the cached pages and the scroll position.

## Data and caching

**How does the cache meet R7–R10?**
- The list reads only from Room. A `RemoteMediator` refreshes page 1 at start (`LAUNCH_INITIAL_REFRESH`) and replaces the rows in one transaction.
- Room invalidates its `PagingSource`, so fresh data appears without extra code.
- The cached list shows first; if the refresh fails, it stays and a notice explains why.
- Details use a finite cache-then-network flow. See [ADR 0003](adr/0003-remotemediator-cache.md) and [WALKTHROUGHS §2–3](WALKTHROUGHS.md#2-first-launch-online-the-list-appears).

**Why not a `RemoteMediator` for details too?**
- Details are one item, not a paged list.
- A small flow (cache, network, save, complete) is simpler, and its completion tells the ViewModel the refresh has ended.

**Why is search not cached?**
- Results depend on free text and go out of date quickly; caching them would grow the database for little benefit.
- Offline, search shows the standard error with Retry.

**What happens to the cache when the schema changes?**
- Upgrades drop and recreate the tables (`fallbackToDestructiveMigration`), which only costs a re-download.
- The schema is exported to `app/schemas/` (versions 1 and 2), so real migrations can be added if the database ever stores user data.

**How are duplicates handled?**
- **Within a page:** `distinctBy` on the ID.
- **Across cached pages:** Room's `IGNORE` conflict strategy keeps the first position.
- **Across search pages:** the paging source skips IDs it already returned.

All three matter because the grid keys items by movie ID, and a repeated key crashes `LazyVerticalGrid`.

## Networking

**How is it guaranteed that every request has the required filter parameters?**
- One OkHttp interceptor (`ContentFilterInterceptor`) adds them to every request and overwrites any caller value with `setQueryParameter`.
- `TmdbHttpClientFactoryTest` asserts this on the same client the app uses.

**Why does search show some movies above PG-13?**
TMDb applies the certification filter only on `/discover/movie`. Search still sends the parameters, but only `include_adult` takes effect there. See [ADR 0004](adr/0004-search-certification-filter.md).

**Where is the TMDb token, and is it safe?**
- It is read from `local.properties` (or a Gradle property, or an environment variable for CI) into `BuildConfig` at build time; `local.properties` is git-ignored.
- It is sent as a Bearer header, never in URLs, and redacted from HTTP logs.
- It is **compiled into the APK, so it can be extracted**. That is acceptable for TMDb's read-only token in a demo, and the token is regenerated after the review.
- A production app would call its own backend, which holds the token and proxies TMDb.

**How is bad data from the API handled?**
- **Parsing is lenient:** unknown fields are ignored and nulls fall back to defaults, so one odd field never fails a whole page.
- **The mappers validate:**
  - movies without an ID or title are dropped;
  - ratings are clamped to 0–10;
  - dates are parsed strictly;
  - empty text becomes `null`;
  - a runtime of 0 is hidden;
  - invalid and duplicate genres are dropped;
  - unknown statuses become `Unknown`.

## Errors

**How are errors handled?**
- Every exception becomes one of seven `DomainError` cases in the data layer (`toDomainError()`).
- Non-paged calls return `AppResult`; paged loads report `LoadState.Error(DomainException)`.
- The UI maps each error to one localized message. ViewModels never see network exceptions. See [ADR 0008](adr/0008-typed-domain-errors.md).

**What does the user see for each failure?**

| Situation | What the user sees |
|---|---|
| Nothing to show | Full-screen error with Retry |
| Saved data on screen | Kept, plus a non-blocking notice (list) or snackbar (details) |
| A later page fails | Footer with Retry |

All of these were checked on a device with no internet, a wrong token and a forced timeout ([review](features/28-quality-review.md)).

## Concurrency

**Where does work run, and how are dispatchers handled?**
- Retrofit and Room `suspend` functions are main-safe, so the repositories need no dispatcher switch.
- Where a dispatcher is needed (DataStore's scope), it is injected (`@IoDispatcher`), never hard-coded.
- ViewModels use `viewModelScope`.
- `CancellationException` is always rethrown, never turned into an error.

**How does search avoid unnecessary requests and stale results?**
- `debounce(350 ms)`, with no delay when clearing.
- Validation, then `distinctUntilChanged`, so whitespace edits are free.
- `flatMapLatest`, so a new query cancels the old one.
- Tested in virtual time ([TESTING §5](TESTING.md#5-patterns-that-make-the-tests-reliable)).

## UI

**What about Compose performance?**
- **Scroll-driven effects read state in the draw phase:** the details backdrop parallax (`graphicsLayer`), the top bar fill (`drawBehind`) and the rating ring arc (`Canvas`), so scrolling does not recompose.
- **`derivedStateOf`** for the scroll-to-top threshold.
- **Lazy grids use stable keys** (movie ID) and `contentType`.
- **Formatting is done once**, in the ViewModel's display models.

**Accessibility?**
- Every tappable element has a label, and cards read as one sentence.
- Touch targets are at least 48 dp.
- Images are decorative where the title names the movie.
- Status screens scroll at large text sizes.
- `ColorContrastTest` checks WCAG AA contrast.
- The rating ring is described as one sentence.

**RTL and Arabic?**
- All strings are in `values` and `values-ar`; layouts use start/end; directional icons auto-mirror.
- Numbers and dates use the locale's digits, and plurals cover Arabic's six forms.
- TMDb content is requested in the app's language.

**Edge to edge?**
- Content draws behind the system bars, with insets applied per element, including side insets in landscape.
- A surface-coloured gradient keeps status icons readable over the backdrop.
- The search content fits above the keyboard (`fitInside(WindowInsetsRulers.Ime)`, `adjustResize`).

## Navigation

**Why Navigation 3, and how does the details screen get its movie ID?**
- The back stack is a saveable list of serializable keys the app owns.
- Each entry gets its own ViewModel store.
- `DetailsViewModel` receives the ID through Hilt assisted injection (`hiltViewModel(creationCallback = { it.create(movieId) })`), so it is a plain constructor value. See [ADR 0007](adr/0007-navigation-3.md).

**What survives rotation and process death?** See [WALKTHROUGHS §11](WALKTHROUGHS.md#11-rotation-and-process-death).

## Known limitations

| Limitation | Why | Next step |
|---|---|---|
| Search can show titles above PG-13 | TMDb ignores the certification filter on search | Server-side filtering through a backend, or per-result certification lookups |
| Search text is lost after process death | `MoviesViewModel` keeps the query in memory, not in `SavedStateHandle` | Store the query in `SavedStateHandle` |
| No automatic retry when the connection returns | Kept out of scope (extra E11) | Observe connectivity and retry failed loads |
| Token inside the APK | No backend in this task | A backend proxy |
| Release build is not minified | R8 rules for kotlinx.serialization, Retrofit and Room were not needed for the review build | Enable `isMinifyEnabled` with keep rules, then test the release build |
| No two-pane layout on tablets | Out of scope (extra E7) | Navigation 3 list–detail scene |
| Digit style with a per-app Arabic language on an English Android 13+ phone | Not verified on that device | See the [review](features/28-quality-review.md) |
