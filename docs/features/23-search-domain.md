# 23: Search, domain layer (T3.1)

## 1. What was built
The pure Kotlin core of search (R12, R22, R27):
- **`SearchQueryValidator`** turns raw search-bar text into a `SearchQuery`:
  - it trims the text and collapses runs of whitespace into one space, so `"  spider   man "` becomes `"spider man"`;
  - blank text means `SearchQuery.Idle` (show the popular list);
  - anything else is `SearchQuery.Valid(text)`, capped at 100 characters, without a trailing space after the cut.
- **`MovieSearchRepository`**: `searchMovies(query): Flow<PagingData<Movie>>`, which T3.2 implements.
- **`SearchMoviesUseCase(rawQuery)`** validates the text itself. An idle query returns an empty, finished `PagingData` without touching the repository. A valid one goes to the repository already cleaned up.

## 2. Files created or changed

| Layer | File | Change |
|-------|------|--------|
| Domain | `feature/movies/domain/usecase/SearchQueryValidator.kt` | New: `SearchQuery` and `SearchQueryValidator` |
| Domain | `feature/movies/domain/repository/MovieSearchRepository.kt` | New |
| Domain | `feature/movies/domain/usecase/SearchMoviesUseCase.kt` | New |
| Tests | `test/.../feature/movies/domain/usecase/SearchQueryValidatorTest.kt` | New: 7 tests |
| Tests | `test/.../feature/movies/domain/usecase/SearchMoviesUseCaseTest.kt` | New: 4 tests |
| Docs | `docs/PLAN.md` | T3.1 ticked; T3.2 text and the package tree name `MovieSearchRepository` |

## 3. Data flow walkthrough

```
search bar text ─▶ (T3.3: debounce in MoviesViewModel) ─▶ SearchMoviesUseCase(rawQuery)
   SearchQueryValidator.validate(rawQuery)
      Idle         ─▶ flowOf(PagingData.empty(all NotLoading, end reached))
      Valid(text)  ─▶ MovieSearchRepository.searchMovies(text)   (T3.2: /search/movie, paged)
```

```kotlin
operator fun invoke(rawQuery: String): Flow<PagingData<Movie>> =
    when (val query = SearchQueryValidator.validate(rawQuery)) {
        SearchQuery.Idle -> flowOf(PagingData.empty(NOTHING_TO_LOAD))
        is SearchQuery.Valid -> repository.searchMovies(query.text)
    }
```

## 4. Decisions made, and alternatives considered
- **A separate `MovieSearchRepository` instead of a new method on `MoviesRepository`.** Adding the method now would force a placeholder in `MoviesRepositoryImpl` until T3.2. The two are also different by nature: the popular list is cached in Room, while search is network-only paging. Separate interfaces keep each caller depending only on what it uses (interface segregation).
- **Whitespace is collapsed, not only trimmed.** "spider man" and "spider   man" return the same results from TMDb, so treating them as one query lets the ViewModel's `distinctUntilChanged` (T3.3) skip the extra request.
- **The use case validates, even though the ViewModel will validate too.** It is the domain's entry point, so no caller can send a blank or oversized query to TMDb. Validation is cheap and idempotent.
- **`SearchQuery` is a sealed type, not a nullable string.** "Idle" is a real state that the screen reacts to (show the popular list), and a `when` over it is exhaustive.
- **100 characters max.** No title needs more, and it keeps a pasted paragraph from becoming a huge request URL.
- **No minimum length.** TMDb handles one-character queries, and blocking them would make results seem to appear "late".
- **The idle result is "finished" (`NotLoading`, end reached),** so whatever shows it never displays a spinner. As learned in T1.6, `PagingData` without load states looks like it is still loading.

## 5. How it is tested
- `SearchQueryValidatorTest` (7): blank variants are idle; trimming; whitespace collapsing (spaces, tabs, newlines); a single character is valid; Arabic text is kept; the cap; no trailing space after the cap.
- `SearchMoviesUseCaseTest` (4, paging-testing `asSnapshot`): valid queries return the repository's results in order; the repository receives the cleaned-up query; blank queries return nothing and never reach the repository; oversized queries are capped first.

`gradlew.bat assembleDebug` and `testDebugUnitTest` pass (162 unit tests). Nothing changes on a device yet: the search bar arrives in T3.4.

## 6. How to build it from scratch
1. Write `SearchQuery` (`Idle`, `Valid(text)`) and `SearchQueryValidator.validate()` (trim, collapse whitespace, blank → Idle, cap, `trimEnd`).
2. Write `MovieSearchRepository` with `searchMovies(query)`.
3. Write `SearchMoviesUseCase`: validate, then return an empty finished `PagingData` for Idle, or the repository flow for Valid.
4. Write the tests: validator cases, and use-case cases with a fake repository that records the queries it receives.
5. Run `gradlew.bat testDebugUnitTest`.
