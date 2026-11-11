# 17: Movie details, domain layer (T2.1)

## 1. What was built
The pure Kotlin core of the details feature (R6, R16, R27):
- **`MovieDetails`**: every field the details screen shows: title, tagline, synopsis (`overview`), poster and backdrop URLs, release date (`LocalDate`), runtime, rating with vote count, genres, original language and production status. Like `Movie`, it holds validated values: the title is never blank, the rating is within 0–10, and anything TMDb omits or sends empty is `null`.
- **`Genre(id, name)`**: the ID gives each genre chip a stable key in the UI.
- **`ReleaseStatus`**: an enum (Rumored, Planned, InProduction, PostProduction, Released, Canceled, Unknown). TMDb always sends the status as English text, so an enum lets the presentation layer show a translated label.
- **`MovieDetailsRepository.movieDetails(id): Flow<AppResult<MovieDetails>>`**: a cache-first contract (R7, requirements Q5).
- **`GetMovieDetailsUseCase(movieId)`**: passes the repository flow through, and rejects non-positive IDs as `NotFound`.

## 2. Files created or changed

| Layer | File | Change |
|-------|------|--------|
| Domain | `feature/details/domain/model/MovieDetails.kt` | New: `MovieDetails`, `Genre`, `ReleaseStatus`. |
| Domain | `feature/details/domain/repository/MovieDetailsRepository.kt` | New: the repository contract. |
| Domain | `feature/details/domain/usecase/GetMovieDetailsUseCase.kt` | New: the use case. |
| Test | `test/.../feature/details/domain/usecase/GetMovieDetailsUseCaseTest.kt` | New: 6 unit tests. |
| Test | `test/.../testing/TestData.kt` | Added the `movieDetails()` and `genre()` builders. |
| Docs | `docs/PLAN.md` | T2.1 ticked; §4.2, §5 and T2.1 text aligned with the decisions below. |
| Build | `app/build.gradle.kts` | `minSdk` 24 → 26 (see Decisions). |
| Resources | `res/mipmap-anydpi-v26/` → `res/mipmap-anydpi/` | The `-v26` qualifier is redundant once minSdk is 26. |
| Docs | `docs/requirements.md` | Q17 updated with the new minSdk decision. |

## 3. Data flow walkthrough
Only the domain part exists so far. The data layer (T2.2) will implement the repository, and the ViewModel (T2.3) will call the use case:

```
DetailsViewModel ──invoke(movieId)──▶ GetMovieDetailsUseCase ──movieDetails(id)──▶ MovieDetailsRepository (T2.2)
                 ◀── Flow<AppResult<MovieDetails>> ───────────────────────────────────────────────┘
```

The contract fixes what the flow emits, so T2.2 and T2.3 can be built against it independently:

| Cache | Network | Emissions |
|-------|---------|-----------|
| no | ok | `Success(fresh)` |
| no | fails | `Failure(error)` |
| yes | ok | `Success(cached)`, `Success(fresh)` |
| yes | fails | `Success(cached)`, `Failure(error)` |

After the network outcome the flow **completes**. The use case adds one rule:

```kotlin
operator fun invoke(movieId: Int): Flow<AppResult<MovieDetails>> =
    if (movieId > 0) repository.movieDetails(movieId)
    else flowOf(AppResult.Failure(DomainError.NotFound))
```

## 4. Decisions made, and alternatives considered
- **A finite flow, not a long-lived Room observer.** Completion tells the ViewModel when the refresh is over: it can show "refreshing" from the first cached value until completion, and Retry simply collects again. An endless Room flow would need a separate refresh signal and a separate "refresh failed" channel.
- **`Failure` after `Success` means "refresh failed, cache available".** This keeps the single `AppResult` type the rest of the app uses. A richer wrapper (`data`, `error`, `isRefreshing`) was considered. It would make the four cases explicit, but it duplicates state the ViewModel owns anyway.
- **A full release date, and minSdk raised to 26.** R6 lists the details fields only as "synopsis, rating, genre, etc.", so the format is ours to choose. A full date formatted in the user's locale ("Mar 12, 2024" or its Arabic form) tells the user more than a year. `java.time.LocalDate` is built in from API 26. The alternative, core-library desugaring, would have kept minSdk 24 at the cost of an extra library and build step, only for API 24–25 devices (a very small share). The user approved the change, and requirements Q17 records it. The list keeps `Movie.releaseYear`, because R2 asks for the release year there. `LocalDate` is plain JVM, so the domain layer stays free of Android imports.
- **`ReleaseStatus` as an enum, not a string.** It allows Arabic labels (CLAUDE.md) and an exhaustive `when` in the UI. Unknown values fall back to `Unknown`.
- **Language as an ISO code.** The presentation layer formats it with `Locale` in the user's language ("English" / "الإنجليزية"), so the domain stays free of display logic.
- **Non-positive IDs fail fast as `NotFound`.** TMDb IDs are positive, so such an ID (for example from a corrupted saved back stack) could never match. Failing early skips a database read and a network call, and the screen can show its "no longer available" state.

## 5. How it is tested
`GetMovieDetailsUseCaseTest` (JUnit, Turbine, coroutines-test) with a fake repository:
- the requested ID reaches the repository;
- cached and then fresh details arrive in order, and the flow completes;
- a failed refresh after cached details comes through unchanged;
- a failure without cache comes through unchanged;
- IDs 0 and -1 emit `NotFound` and never reach the repository;
- each invocation asks the repository for a new stream.

`gradlew.bat assembleDebug` and `gradlew.bat testDebugUnitTest` pass. A grep confirms that `feature/details/domain` has no Android, AndroidX, Retrofit or Timber imports.

## 6. How to build it from scratch
1. Create `feature/details/domain/model/MovieDetails.kt` with `MovieDetails`, `Genre` and `ReleaseStatus`. Use nullable fields for anything TMDb may omit.
2. Create `MovieDetailsRepository` with `movieDetails(id): Flow<AppResult<MovieDetails>>` and document the emission table in its KDoc.
3. Create `GetMovieDetailsUseCase` with `@Inject constructor(repository)` and the positive-ID check.
4. Add `TestData.movieDetails()` and `TestData.genre()`.
5. Write `GetMovieDetailsUseCaseTest` with a fake repository that records the requested IDs and emits a fixed sequence. Assert each case with Turbine, including `awaitComplete()`.
6. Run `gradlew.bat testDebugUnitTest`.
