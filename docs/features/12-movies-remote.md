# 12: Movies list, remote data (T1.2)

## 1. What was built
The network side of the movie list (R1, R2, R22, R23):
- **`MoviesApi.discoverMovies(page, sortBy)`**: `GET /discover/movie`. The shared interceptors add the filters, token and language (T0.5).
- **DTOs** (`MoviesPageDto`, `MovieDto`) matching TMDb's JSON. Every field is nullable with a default, so one bad item never fails a whole page.
- **`MovieDto.toDomainOrNull()`**: the **single validation point**. It drops unusable items and makes optional data safe.
- **`MoviesRemoteDataSource.discoverMovies(page)`**: returns a `MoviesPage` with valid, de-duplicated movies and an `isLastPage` flag that respects TMDb's 500-page cap.
- **`MoviesModule`**: provides `MoviesApi` through Hilt.

## 2. Files created or changed

| Layer | File | Change |
|-------|------|--------|
| Data / remote | `feature/movies/data/remote/MoviesApi.kt` | New. Retrofit interface. |
| Data / remote | `feature/movies/data/remote/dto/MovieDto.kt`, `MoviesPageDto.kt` | New. `@Serializable` DTOs. |
| Data / remote | `feature/movies/data/remote/MoviesPage.kt` | New. A validated page (`page`, `movies`, `isLastPage`). |
| Data / remote | `feature/movies/data/remote/MoviesRemoteDataSource.kt` | New. Calls the API, validates, de-duplicates, applies the page cap. |
| Data / mapper | `feature/movies/data/mapper/MovieMapper.kt` | New. `toDomainOrNull()`, `toReleaseYear()`, `toValidRating()`. |
| DI | `feature/movies/di/MoviesModule.kt` | New. `@Provides MoviesApi`. |
| Tests | `test/resources/tmdb/discover_movie_page.json` | New. Real-shaped TMDb page with defects: a missing poster, an empty date, a blank title, a missing ID. |
| Tests | `MovieMapperTest.kt` (9), `MoviesRemoteDataSourceTest.kt` (8) | New. |

## 3. Data flow walkthrough

```
MoviesRemoteDataSource.discoverMovies(page = 2)
  → MoviesApi.discoverMovies(page = 2, sortBy = "popularity.desc")
      GET /3/discover/movie?page=2&sort_by=popularity.desc
          &include_adult=false&certification_country=US&certification.lte=PG-13&language=en-US
  ← MoviesPageDto(page = 2, results = [MovieDto…], totalPages = 1001)
  → results.mapNotNull { it.toDomainOrNull() }      // validate (R22)
           .distinctBy { it.id }                     // TMDb can repeat an item within a page
  → MoviesPage(page = 2, movies = [Movie…], isLastPage = 2 >= min(1001, 500))
```

**Validation rules** (`MovieMapper.kt`):

| Field | Rule | When invalid |
|-------|------|--------------|
| `id` | must be present and > 0 | the item is **dropped** |
| `title` | must not be blank; trimmed | the item is **dropped** |
| `poster_path` | built into a full `w342` URL | `null`, and the UI shows a placeholder |
| `release_date` | strict ISO `yyyy-MM-dd`, year ≥ 1880 | `null` year, so no year is shown |
| `vote_average` | limited to 0–10 | missing or NaN becomes `0.0` |

Errors aren't caught here. `HttpException`, `IOException` and `SerializationException` propagate to the `RemoteMediator` (T1.3), which turns them into `DomainException`s for Paging (T0.6).

## 4. Decisions made, and alternatives considered
- **Validate once, from DTO to domain.** Room (T1.3) will store already-validated `Movie`s plus paging metadata, so validation isn't duplicated in an entity mapper.
- **`/discover/movie` instead of `/movie/popular`.** The PDF's certification filter (R25, R26) only works on discover. `sort_by=popularity.desc` gives the same "popular movies" list.
- **A page cap of 500.** TMDb reports `total_pages=1001` but answers **HTTP 400** for page 501 (confirmed against the real API, below). `isLastPage` therefore uses `min(total_pages, 500)`, so Paging stops cleanly instead of showing an error at the end of the list.
- **No dispatcher switch in the data source.** Retrofit `suspend` functions run network I/O and JSON parsing on OkHttp's threads, so they're already safe to call from the main thread. A `withContext(io)` would only add a pointless thread hop. Injected dispatchers are still used where real blocking work happens (Room in T1.3 runs its own executors).
- **Regex date parsing instead of `java.time`.** `LocalDate.parse` needs API 26 or core-library desugaring (minSdk is 24). Only the year is needed, and a strict regex rejects malformed dates such as `2024-13-01`. *Superseded in T2.2:* minSdk became 26 in T2.1, so both features now share a strict `java.time` parser (`core/network/TmdbValues.kt`), which also rejects impossible days such as `2023-02-29`.
- **Package by feature.** All of this lives in `feature/movies/data/…`, following CLAUDE.md's "package by feature, layers inside". Each feature stays self-contained and could later become its own Gradle module (R18, extra E16). The dependency rule is the same as in a package-by-layer layout. This will be recorded as an ADR in T4.2.

## 5. How it is tested

**`MovieMapperTest`** (9): a valid DTO maps every field. Missing, zero or negative IDs and missing or blank titles are rejected. Titles are trimmed. A missing poster becomes `null`. ISO dates give the year. Nine kinds of malformed date give `null`. Implausibly early years are ignored. Ratings are limited to 0–10 and NaN becomes 0.

**`MoviesRemoteDataSourceTest`** (8, real Retrofit and interceptors against MockWebServer):
- the request path, `page`, `sort_by` and the content filter;
- the real-shaped fixture gives only the 3 valid movies, in order, with mapped fields;
- duplicate IDs within a page are kept once;
- the last page when `page == total_pages`;
- the 500-page cap (499 isn't last, 500 is);
- an empty result is an empty last page;
- HTTP 401 gives `Unauthorized`;
- malformed JSON gives `Unknown`.

**One-off check against the real TMDb API** (with the developer's token; the temporary test was deleted afterwards):

```
page1   raw=20 valid=20 total_pages=1001 isLastPage=false
  #687163 | Project Hail Mary | 2026 | 8.628 | poster ✓
  #155    | The Dark Knight   | 2008 | 8.54  | poster ✓
  …
page2   valid=20 overlapWithPage1=1
page500 valid=20 isLastPage=true
page501 → HTTP 400 → DomainError.Unknown
```

**Finding for T1.3:** one movie appeared on both page 1 and page 2, because popularity ranks shift between requests. Duplicates *across* pages are handled by the cache: the movie ID is the table key, and the first position wins.

Results: `assembleDebug` passes, and 62 of 62 unit tests pass.

## 6. How to build it from scratch
1. Write `@Serializable` DTOs with nullable fields and defaults, declaring only the fields used.
2. Write `MoviesApi` with `@GET("discover/movie") suspend fun discoverMovies(@Query("page") page, @Query("sort_by") sortBy)`.
3. Write `MovieDto.toDomainOrNull()`: reject a bad ID or title, and make poster, year and rating safe.
4. Write `MoviesRemoteDataSource`: call the API, `mapNotNull` + `distinctBy`, and `isLastPage = page >= min(totalPages, 500)`.
5. Provide `MoviesApi` from Retrofit in a feature Hilt module.
6. Test the mapper with edge cases, and test the data source with MockWebServer and a real-shaped JSON fixture under `src/test/resources`.
