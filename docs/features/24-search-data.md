# 24: Search, data layer (T3.2)

## 1. What was built
The network side of search (R12, R23, R24, R28):
- **`MoviesApi.searchMovies(query, page)`** calls `GET search/movie`. It has the same page shape as `/discover/movie`, and the shared interceptors still add the Bearer token, `language` and the three filter parameters.
- **`MoviesRemoteDataSource.searchMovies(query, page)`** reuses the list's page handling, now in one private `toMoviesPage()`: item validation, de-duplication within a page, and the 500-page cap.
- **`SearchPagingSource`** (network only, not cached):
  - it loads page 1 on refresh and the next page on append;
  - there is no next page after the last one;
  - failures become `LoadResult.Error(DomainException)` through the shared `toDomainException()`;
  - movies already returned on an earlier page are skipped.
- **`MovieSearchRepositoryImpl`**: a `Pager` over `SearchPagingSource`, bound to `MovieSearchRepository` in `MoviesModule`.
- **`MoviesRemoteDataSource.PAGE_SIZE`** (20) is now shared by both pagers, replacing the private constant in `MoviesRepositoryImpl`.

## 2. Files created or changed

| Layer | File | Change |
|-------|------|--------|
| Data, remote | `feature/movies/data/remote/MoviesApi.kt` | Adds `searchMovies` |
| Data, remote | `feature/movies/data/remote/MoviesRemoteDataSource.kt` | Adds `searchMovies`; shared `toMoviesPage()`; `PAGE_SIZE` |
| Data, remote | `feature/movies/data/remote/SearchPagingSource.kt` | New |
| Data, repository | `feature/movies/data/repository/MovieSearchRepositoryImpl.kt` | New |
| Data, repository | `feature/movies/data/repository/MoviesRepositoryImpl.kt` | Uses the shared `PAGE_SIZE` |
| DI | `feature/movies/di/MoviesModule.kt` | Binds `MovieSearchRepository` |
| Tests | `test/.../feature/movies/data/remote/MoviesRemoteDataSourceTest.kt` | 3 search cases (MockWebServer) |
| Tests | `test/.../feature/movies/data/remote/SearchPagingSourceTest.kt` | New: 8 tests |
| Tests | `androidTest/.../testing/FakeMoviesApi.kt` | Implements the new method (fails if the cache tests ever call it) |
| Docs | `docs/PLAN.md` | T3.2 ticked |

## 3. Data flow walkthrough

```
SearchMoviesUseCase(raw) ─▶ validate ─▶ MovieSearchRepositoryImpl.searchMovies(text)
   Pager(pageSize = 20) { SearchPagingSource(dataSource, text) }
      load(page) ─▶ MoviesRemoteDataSource.searchMovies(text, page)
                       ─▶ GET /3/search/movie?query=…&page=…  (+ auth, language, include_adult, certification_*)
                       ─▶ MoviesPageDto.toMoviesPage(page)  (validate items, cap at 500 pages)
                 ─▶ skip already-shown ids ─▶ LoadResult.Page(data, prevKey = null, nextKey = page + 1 | null)
      failure ─▶ LoadResult.Error(e.toDomainException())
```

```kotlin
val result = dataSource.searchMovies(query = query, page = page)
val newMovies = synchronized(shownIds) { result.movies.filter { shownIds.add(it.id) } }
LoadResult.Page(data = newMovies, prevKey = null, nextKey = if (result.isLastPage) null else page + 1)
```

## 4. Decisions made, and alternatives considered
- **Search is not cached.** R7 asks for caching the retrieved movie data, which the popular list and details do. Search results depend on free text, are short-lived, and would grow the database for little value. Offline, search shows the standard error with Retry.
- **Movies repeated across pages are skipped.** TMDb's ranking can shift between page requests, and the grid keys items by movie ID, so a repeated ID would crash it ("Key was already used"). The popular list avoids this through Room (`OnConflictStrategy.IGNORE`). Search has no database, so the source remembers what it has returned. It is a new source per query and per refresh, so memory is bounded and starts clean.
- **`getRefreshKey` returns `null`.** With prepend unused and a fresh "already shown" set per source, refreshing from the middle could repeat or skip movies. Search results are short, so restarting from page 1 is cheap.
- **The certification filter on search (requirements Q2).** All three parameters are still sent, as the PDF requires and as the new test asserts. TMDb applies only `include_adult` on `/search/movie`, so search can return titles rated above PG-13. This is a TMDb API limitation, recorded here and to be written up as an ADR in T4.2. Fetching every result's US certification to filter client-side would cost one extra request per movie.
- **No search text in logs.** The query is user input, so the debug log records only the page number and counts (CLAUDE.md logging rules).
- **One page builder for both endpoints.** The two responses share a shape, so the validation and page cap live in one place.

## 5. How it is tested
- `MoviesRemoteDataSourceTest` (MockWebServer, real Retrofit stack and interceptors), with three new search cases:
  - the request goes to `/3/search/movie` with `query`, `page`, `include_adult=false`, `certification_country=US` and `certification.lte=PG-13`;
  - results are validated like the list;
  - no matches gives an empty last page.
- `SearchPagingSourceTest` (8, paging-testing `TestPager` with a scripted fake API):
  - refresh loads page 1 for the query;
  - append loads the next page;
  - the last page has no next key;
  - no matches gives an empty final page;
  - a movie repeated on a later page is skipped;
  - an `IOException` becomes `DomainException(NoInternet)`;
  - the refresh key is `null`;
  - the repository pages through all 25 results across two pages.
- `gradlew.bat assembleDebug`, `testDebugUnitTest` (173 tests) and `assembleDebugAndroidTest` pass. Nothing changes on a device until the search bar arrives (T3.4).

## 6. How to build it from scratch
1. Add `@GET("search/movie") searchMovies(@Query("query"), @Query("page"))` to `MoviesApi`.
2. Extract `MoviesPageDto.toMoviesPage(page)` in `MoviesRemoteDataSource`, add `searchMovies(query, page)`, and add a shared `PAGE_SIZE`.
3. Write `SearchPagingSource`: page key, the "already shown" ID set, `nextKey` from `isLastPage`, `toDomainException()` on failure, and `getRefreshKey = null`.
4. Write `MovieSearchRepositoryImpl` (a `Pager` with a new source per query), and bind it in `MoviesModule`.
5. Implement the new method in any `MoviesApi` fakes.
6. Add the MockWebServer cases and the `TestPager` tests, then run `gradlew.bat testDebugUnitTest`.
