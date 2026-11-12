# 11: Movies list, domain layer (T1.1)

## 1. What was built
The domain layer of the movie list feature, the first slice of Phase 1 (R1, R16):
- **`Movie`**: the list item model, with only what the list shows (title, poster, release year) plus the rating for the badge.
- **`MoviesRepository`**: the contract the data layer implements: `popularMovies(): Flow<PagingData<Movie>>`.
- **`GetMoviesUseCase`**: the single entry point the ViewModel (T1.4) will depend on.
- **`TestData`**: a shared test factory for domain objects (postponed from T0.10 until the model existed).

All of it is plain Kotlin. The only library import is `androidx.paging.PagingData` (see Decisions).

## 2. Files created or changed

| Layer | File | Change |
|-------|------|--------|
| Domain | `feature/movies/domain/model/Movie.kt` | New. `id`, `title`, `posterUrl?`, `releaseYear?`, `rating`. |
| Domain | `feature/movies/domain/repository/MoviesRepository.kt` | New. Interface with `popularMovies()`. |
| Domain | `feature/movies/domain/usecase/GetMoviesUseCase.kt` | New. `operator fun invoke()`. |
| Tests | `test/.../testing/TestData.kt` | New. `movie(...)` and `movies(count)` builders. |
| Tests | `test/.../feature/movies/domain/usecase/GetMoviesUseCaseTest.kt` | New. 3 tests with a fake repository. |
| Docs | `docs/GIT_FLOW.md` | Remote feature branches are kept after merging, and only local ones are deleted. |

## 3. Data flow walkthrough
This task defines the middle of the chain. The data layer (T1.2/T1.3) will implement the interface, and the ViewModel (T1.4) will call the use case:

```
MoviesRepositoryImpl  ──implements──►  MoviesRepository.popularMovies(): Flow<PagingData<Movie>>
                                                   ▲
                                      GetMoviesUseCase()  ◄── MoviesViewModel (T1.4)
```

```kotlin
class GetMoviesUseCase @Inject constructor(private val repository: MoviesRepository) {
    operator fun invoke(): Flow<PagingData<Movie>> = repository.popularMovies()
}
```

The **contract** in the KDoc matters more than the code. The repository must emit cached pages first and update them when fresh data arrives (R8–R10), and it must report failures as `LoadState.Error(DomainException)` (R21). T1.3's tests verify the implementation against this contract.

## 4. Decisions made, and alternatives considered
- **`PagingData` in the domain layer.** `androidx.paging:paging-common` is a Kotlin library without Android dependencies, so the domain stays platform-free (checked: no `android.*`, Retrofit, OkHttp or Room imports under `domain/`). The alternative, a custom page type with our own load states, would re-implement what Paging already does well (prefetching, retry, load states) and lose `LazyPagingItems` in the UI.
- **A minimal `Movie`.** Only list fields. Details (synopsis, genres, runtime and so on) get their own `MovieDetails` model in Phase 2, so the list never carries data it doesn't show.
- **Already-validated values.** `title` is never blank, `rating` is limited to 0–10, and unknown values are `null` rather than magic placeholders. The mapper (T1.2) enforces this and the UI trusts it (R22).
- **A full image URL instead of a TMDb path.** Building URLs (base URL plus size) is a data-layer concern (`TmdbImageUrls`). The domain and UI just see "the poster's URL or none".
- **A pass-through use case.** It adds no logic today, but it gives the ViewModel one stable dependency and gives list rules an obvious home. That's the Clean Architecture boundary the PDF asks for (R16).

## 5. How it is tested
`GetMoviesUseCaseTest` uses a fake repository and Paging's `asSnapshot()`, which collects a `Flow<PagingData>` into a plain list:

| Test | Checks |
|------|--------|
| Returns the repository's movies unchanged and in order | The use case doesn't alter, drop or reorder items. |
| An empty repository gives an empty list | The empty case passes through cleanly (it's the input for the Empty state in T1.4). |
| Each call asks the repository for a fresh stream | No hidden caching in the use case. Caching belongs in the data layer and in `cachedIn` in the ViewModel. |

Results: `assembleDebug` passes, and 45 of 45 unit tests pass. There's no device check, since this task has no UI.

## 6. How to build it from scratch
1. Create `Movie` in `feature/movies/domain/model` with only the list fields, using nullable types for data TMDb may omit.
2. Create `interface MoviesRepository { fun popularMovies(): Flow<PagingData<Movie>> }`, and document the caching and error contract in its KDoc.
3. Create `GetMoviesUseCase @Inject constructor(repository)` with `operator fun invoke() = repository.popularMovies()`.
4. Add the `TestData.movie(...)` builders in the test source set.
5. Test with a fake repository returning `flowOf(PagingData.from(list))`, and check the result with `useCase().asSnapshot()` inside `runTest`.
6. Grep `domain/` for platform imports to confirm the layer is pure.
