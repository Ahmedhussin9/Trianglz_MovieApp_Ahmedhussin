# 19: Movie details, ViewModel (T2.3)

## 1. What was built
The presentation logic of the details screen (R6, R17, R21, R29), ready for the UI in T2.4:
- **MVI contract** (`DetailsContract.kt`):
  - `DetailsUiState(status, isRefreshing, isSynopsisExpanded)`, where `status` is `Loading`, `Content(MovieDetailsUi)` or `Error(DomainError)`;
  - actions `Retry`, `Back` and `ToggleSynopsis`;
  - effects `NavigateBack` and `ShowRefreshError(error)`.
- **`DetailsViewModel`**: receives the movie ID through Hilt assisted injection, loads on creation, and maps the repository's cache-then-network emissions to state:

  | Repository emits | Screen state |
  |---|---|
  | `Success` (any) | `Content`; `isRefreshing = true` until the flow completes |
  | `Failure` while content is shown | content stays; `ShowRefreshError` effect (snackbar) |
  | `Failure` with nothing shown | `Error(error)`, with Retry |
  | flow completes | `isRefreshing = false` |

  Retry from the error screen shows Loading again. Retry with content on screen keeps the content and sets `isRefreshing`. A Retry while a load is still running is ignored, so a double tap never starts two requests.
- **`MovieDetailsUi`** and `MovieDetails.toUi(locale)`, which format everything that depends on the locale:
  - the release date: `MEDIUM` style, e.g. "Oct 30, 1995" (in Arabic, the Arabic form);
  - the rating with one decimal, plus a 0–1 fraction for the rating ring;
  - the vote count with grouping ("18,924");
  - the original language name ("en" → "English" / "الإنجليزية").

  A movie with no votes gets no rating text and an empty ring. Runtime and status stay raw, because their wording comes from string resources in the UI.

## 2. Files created or changed

| Layer | File | Change |
|-------|------|--------|
| Presentation | `feature/details/presentation/DetailsContract.kt` | New: state, status, actions, effects |
| Presentation | `feature/details/presentation/DetailsViewModel.kt` | New |
| Presentation | `feature/details/presentation/model/MovieDetailsUi.kt` | New: display model and `toUi(locale)` |
| Tests | `test/.../feature/details/presentation/DetailsViewModelTest.kt` | New: 11 tests |
| Tests | `test/.../feature/details/presentation/model/MovieDetailsUiTest.kt` | New: 6 tests |
| Docs | `docs/PLAN.md` | T2.3 ticked; §4.2 names aligned (`isSynopsisExpanded`, `ShowRefreshError`, `Error(DomainError)`) |

The ViewModel is not yet created anywhere: T2.4 replaces the T0.9 placeholder screen and creates it in the `DetailsRoute` entry.

## 3. Data flow walkthrough

```
DetailsRoute(movieId) ─▶ hiltViewModel(creationCallback = { it.create(movieId) })   (T2.4)
DetailsViewModel.init ─▶ load() ─▶ GetMovieDetailsUseCase(movieId) ─▶ repository flow (T2.2)
     collect: Success ─▶ state.status = Content(details.toUi(locale))
              Failure ─▶ content shown? effect ShowRefreshError : state.status = Error
     complete ─▶ state.isRefreshing = false
```

The ID arrives through assisted injection, the pattern from the `navigation-3` skill's Hilt recipe:

```kotlin
@HiltViewModel(assistedFactory = DetailsViewModel.Factory::class)
class DetailsViewModel @AssistedInject constructor(
    @Assisted private val movieId: Int,
    private val getMovieDetails: GetMovieDetailsUseCase,
    private val localeProvider: LocaleProvider,
) : ViewModel() {
    @AssistedFactory
    interface Factory { fun create(movieId: Int): DetailsViewModel }
}
```

The core of `load()`:

```kotlin
getMovieDetails(movieId).collect { result ->
    when (result) {
        is AppResult.Success -> _state.update {
            it.copy(status = DetailsStatus.Content(result.data.toUi(localeProvider.current())), isRefreshing = true)
        }
        is AppResult.Failure -> if (_state.value.status is DetailsStatus.Content) {
            _effects.send(DetailsEffect.ShowRefreshError(result.error))
        } else {
            _state.update { it.copy(status = DetailsStatus.Error(result.error)) }
        }
    }
}
_state.update { it.copy(isRefreshing = false) }
```

## 4. Decisions made, and alternatives considered
- **Assisted injection, not `SavedStateHandle`.** With Navigation 3 the key is a plain object, not saved-state arguments. Assisted injection passes the ID straight to the constructor, so it is a non-null `val` and tests simply call the constructor. `rememberViewModelStoreNavEntryDecorator` (from T0.9) gives each `DetailsRoute(id)` its own ViewModel.
- **Flow completion ends `isRefreshing`.** This relies on the T2.1 contract (the flow completes after the network outcome), so no extra "refresh finished" signal is needed. When there is no cache, the content and completion arrive together, so no progress bar flashes.
- **A refresh failure over content is an effect, not state.** The snackbar should appear once. Keeping it in state would show it again after rotation, or need a "consumed" flag. A failure with nothing on screen is state (`Error`), because the screen must keep showing it.
- **Retry ignores a running load instead of restarting it.** Cancelling and restarting would race: the cancelled collector could reset `isRefreshing` after the new load set it. Retry is only offered once a load has ended (the error screen or the snackbar), so ignoring is enough.
- **Formatting in `toUi(locale)`, with `LocaleProvider` injected.** Dates and numbers follow the user's language and region (Arabic included) without logic in composables, and tests fix the locale. Runtime and status are not formatted here, because they need string resources ("%d h %d min", status labels) that only the UI reads.
- **Unknown language codes are hidden.** Java returns the code itself ("xx") for codes it does not know, and a bare code means nothing to users.
- **No Empty state.** Details are one item, not a list, so "missing" is `Error(NotFound)` from the use case (invalid ID) or the API (404), with its own message.

## 5. How it is tested
`DetailsViewModelTest` (11, Turbine + `MainDispatcherRule` with a standard test dispatcher, so the tests step through virtual time). The fake repository returns one scripted flow per call:
- the initial state is Loading;
- no cache with network ok → Content, not refreshing;
- cached details show at once with `isRefreshing`, then fresh details replace them;
- a failed refresh keeps the cached content and sends one `ShowRefreshError`;
- failure without cache → Error, then Retry → Loading → Content (2 repository calls);
- Retry with content keeps it visible while refreshing;
- Retry during a running load is ignored (1 call);
- an invalid ID → `Error(NotFound)` without a repository call;
- ToggleSynopsis flips both ways;
- Back sends one `NavigateBack`;
- dates are formatted in the provided locale.

`MovieDetailsUiTest` (6): US formatting, German separators ("8,0", "18.924", "Englisch"), the Arabic language name, no-votes handling, null and unknown values, and raw values passed through.

`gradlew.bat assembleDebug` (including Hilt's processing of the assisted factory) and `gradlew.bat testDebugUnitTest` pass. There is nothing to check on a device yet: the screen arrives in T2.4.

## 6. How to build it from scratch
1. Write `DetailsContract.kt`: `DetailsUiState`, the `DetailsStatus` sealed interface, `DetailsUiAction` and `DetailsEffect`.
2. Write `MovieDetailsUi` and `MovieDetails.toUi(locale)` with `DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)`, `NumberFormat.getIntegerInstance` and `Locale.getDisplayLanguage`, then `MovieDetailsUiTest`.
3. Write `DetailsViewModel` with `@HiltViewModel(assistedFactory = …)`, `@AssistedInject`, `@Assisted movieId` and a nested `@AssistedFactory`.
4. Add the state flow, the effect channel (`receiveAsFlow`), `onAction()`, and `load()` (guarded against running twice) that collects the use case and ends `isRefreshing` after completion.
5. Write `DetailsViewModelTest` with a queue-based fake repository and delayed flows. Use `runCurrent()` and `advanceUntilIdle()` to check the in-between states.
6. Run `gradlew.bat assembleDebug testDebugUnitTest`.
