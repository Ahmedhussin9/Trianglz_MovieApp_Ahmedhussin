# 18: Movie details, data layer and cache (T2.2)

## 1. What was built
The data layer behind `MovieDetailsRepository` (R6, R7, R22, R28, R38, requirements Q5):
- **Remote:** `MovieDetailsApi` (`GET movie/{movie_id}`), `MovieDetailsDto`/`GenreDto`, and `MovieDetailsRemoteDataSource`, which returns an `AppResult`: network and HTTP errors through `safeApiCall`, and a response without a usable ID or title as `Unknown`.
- **Validation:** `MovieDetailsDto.toDomainOrNull()`:
  - rejects a missing ID or a blank title;
  - trims text, so empty tagline, synopsis or language become `null`;
  - turns a runtime of 0 into `null`, so the UI never shows "0 min";
  - clamps the rating to 0–10 and never lets the vote count go negative;
  - drops invalid or duplicate genres;
  - maps TMDb's status text to `ReleaseStatus`, with `Unknown` for anything else.
- **Cache:** a `movie_details` Room table (`MovieDetailsEntity`, `MovieDetailsDao` with `get` and `@Upsert`). Type converters store the release date as ISO text and the genres as a JSON column. The database moves to **version 2**, and the exported schema is in `app/schemas/.../2.json`.
- **Repository:** `MovieDetailsRepositoryImpl` emits the cached details, calls the network, saves fresh details, and emits them, exactly following the T2.1 contract. A cache that cannot be read counts as empty, and a failed save still emits the fresh details. Both are logged with Timber.
- **Shared validation:** `core/network/TmdbValues.kt` (`toValidRating`, `toReleaseDate`, `trimToNull`) is now used by both the list and details mappers. With minSdk 26 (T2.1), the T1.2 regex was replaced by strict `java.time` parsing, which also rejects impossible days such as `2023-02-29`.
- **DI:** `DetailsModule` provides the API and binds the repository, and `DatabaseModule` provides the new DAO.

## 2. Files created or changed

| Layer | File | Change |
|-------|------|--------|
| Data, remote | `feature/details/data/remote/MovieDetailsApi.kt` | New |
| Data, remote | `feature/details/data/remote/dto/MovieDetailsDto.kt` | New (`MovieDetailsDto`, `GenreDto`) |
| Data, remote | `feature/details/data/remote/MovieDetailsRemoteDataSource.kt` | New |
| Data, mapper | `feature/details/data/mapper/MovieDetailsMapper.kt` | New: DTO → domain with validation |
| Data, mapper | `feature/details/data/mapper/MovieDetailsEntityMapper.kt` | New: domain ↔ entity |
| Data, local | `feature/details/data/local/MovieDetailsEntity.kt` | New (`MovieDetailsEntity`, `CachedGenre`) |
| Data, local | `feature/details/data/local/MovieDetailsConverters.kt` | New: date and genres converters |
| Data, local | `feature/details/data/local/MovieDetailsDao.kt` | New |
| Data, repository | `feature/details/data/repository/MovieDetailsRepositoryImpl.kt` | New |
| DI | `feature/details/di/DetailsModule.kt` | New |
| Core | `core/network/TmdbValues.kt` | New: shared TMDb value validation |
| Core | `core/database/MovieDatabase.kt` | Adds `MovieDetailsEntity` and `movieDetailsDao()`; version 2 |
| Core | `core/di/DatabaseModule.kt` | Provides `MovieDetailsDao` |
| Data (movies) | `feature/movies/data/mapper/MovieMapper.kt` | Uses `TmdbValues` instead of its own regex and rating clamp |
| Schema | `app/schemas/.../MovieDatabase/2.json` | Generated |
| Tests | `test/.../core/network/TmdbValuesTest.kt` | New (date and rating tests moved here from `MovieMapperTest`, plus impossible-day cases) |
| Tests | `test/.../feature/details/data/**` | New: mapper, remote data source and repository tests |
| Tests | `test/resources/tmdb/movie_details.json` | New: a real-shaped TMDb response |
| Tests | `test/.../feature/movies/data/mapper/MovieMapperTest.kt` | Moved tests removed; one test checks the mapper uses the shared validators |
| Tests | `androidTest/.../feature/details/data/local/MovieDetailsDaoTest.kt` | New: real SQLite round trips |
| Docs | `docs/PLAN.md`, `docs/features/12-movies-remote.md` | T2.2 ticked; T1.2 regex decision marked as superseded |

## 3. Data flow walkthrough

```
GetMovieDetailsUseCase ─▶ MovieDetailsRepositoryImpl
                              ├─ 1. MovieDetailsDao.get(id) ─▶ entity.toDomain() ─▶ emit Success(cached)   (if any)
                              ├─ 2. MovieDetailsRemoteDataSource.movieDetails(id)
                              │        safeApiCall { api.movieDetails(id) } ─▶ dto.toDomainOrNull()
                              ├─ 3. on Success: dao.upsert(details.toEntity())
                              └─ 4. emit the network result (Success or Failure), then complete
```

The repository is a plain cold `flow`:

```kotlin
override fun movieDetails(id: Int): Flow<AppResult<MovieDetails>> = flow {
    val cached = readCache(id)
    if (cached != null) emit(AppResult.Success(cached))

    val fresh = remoteDataSource.movieDetails(id)
    if (fresh is AppResult.Success) writeCache(fresh.data)
    emit(fresh)
}
```

The remote data source keeps HTTP details out of the repository:

```kotlin
suspend fun movieDetails(movieId: Int): AppResult<MovieDetails> =
    when (val response = safeApiCall { api.movieDetails(movieId) }) {
        is AppResult.Failure -> response
        is AppResult.Success -> response.data.toDomainOrNull()?.let { AppResult.Success(it) }
            ?: AppResult.Failure(DomainError.Unknown)
    }
```

## 4. Decisions made, and alternatives considered
- **A one-row cache read plus a network call, not a Room `Flow`.** The T2.1 contract is a finite flow, so a suspend `get` is enough. An observable query would never complete, and the ViewModel would lose its "refresh finished" signal.
- **Genres as a JSON column, not a table.** Genres are only read together with their movie, so a `genres` table with a cross-reference would add two tables and a join without enabling any query. A damaged JSON value loses only the genres (logged), and the next refresh rewrites the row.
- **Converters registered on the entity, not the database.** They belong to the details feature, and `@TypeConverters` on the entity keeps them there.
- **Status stored as text, and unknown names fall back to `Unknown`.** If a `ReleaseStatus` entry is renamed later, old rows stay readable. Room's built-in enum support would throw on an unknown name.
- **The data source returns `AppResult`, while the list's data source throws.** The list's errors must reach Paging as exceptions (`LoadState.Error`). Details have no such constraint, and `AppResult` matches the repository contract directly.
- **A response without a usable ID or title becomes `Unknown`**, the error `DomainError` reserves for malformed responses. `NotFound` would wrongly tell the user the movie was removed.
- **Cache failures never block the screen.** A read failure counts as a cache miss, and a write failure still emits the fresh data. Both are logged with `Timber.w`. `CancellationException` is always rethrown to keep structured concurrency intact.
- **No dispatcher switch.** Room suspend DAOs and Retrofit suspend calls are main-safe on their own, the same reasoning as `MoviesRemoteDataSource`.
- **Database version 2 with destructive fallback.** It is a cache, so upgrading drops the cached list once and refills it from the network, with no hand-written migration (the existing `DatabaseModule` policy).
- **Shared validation in `core/network`.** Rating and date rules were about to be duplicated, so they moved next to `TmdbImageUrls`, which the mappers already use. Strict `java.time` parsing replaced the T1.2 regex, which accepted impossible days like Feb 30.
- **Larger images on details:** poster `w500` and backdrop `w1280`, because they fill much more of the screen than list posters (`w342`).

## 5. How it is tested
Unit tests (`testDebugUnitTest`):
- `MovieDetailsMapperTest` (13): every field mapped, required fields rejected, text trimming, missing images, invalid dates, a runtime of 0, rating and vote-count limits, genre filtering and de-duplication, language normalization, every status value, the entity round trip, and the unknown cached status.
- `MovieDetailsRemoteDataSourceTest` (6, MockWebServer with the real Retrofit stack): the endpoint path, the auth header and `language`; a real-shaped fixture parsed field by field; 404 → `NotFound`; 401 → `Unauthorized`; a blank title → `Unknown`; malformed JSON → `Unknown`.
- `MovieDetailsRepositoryImplTest` (8, fake DAO and fake API behind the real data source): all four contract rows (with cache-content checks), cache isolation per movie, an unreadable cache, a failed save, and a new refresh per collection.
- `TmdbValuesTest` (5): dates (including impossible days), the rating clamp and text trimming.

Instrumented tests (`connectedDebugAndroidTest`):
- `MovieDetailsDaoTest` (5): a missing row, a full round trip with date and genres, a null date with empty genres, an upsert replacing a row, and rows kept per movie.
- `AppGraphTest` still passes, so the production Hilt graph resolves with the new module.

Results: all unit tests pass; `MovieDetailsDaoTest` and `AppGraphTest` pass 7/7 on a Pixel 4 emulator (API 29). There is no UI yet, so the device check comes with T2.4.

## 6. How to build it from scratch
1. Move `toValidRating` into `core/network/TmdbValues.kt`, add `toReleaseDate` (strict `ISO_LOCAL_DATE`) and `trimToNull`, switch `MovieMapper` to them, and move their tests to `TmdbValuesTest`.
2. Add `MovieDetailsDto` and `GenreDto` with nullable defaulted fields, and `MovieDetailsApi` with `@GET("movie/{movie_id}")`.
3. Write `MovieDetailsDto.toDomainOrNull()` and `toReleaseStatus()`, with `MovieDetailsMapperTest`.
4. Add `MovieDetailsRemoteDataSource` (`safeApiCall` plus validation → `AppResult`), a JSON fixture and the MockWebServer test.
5. Add `MovieDetailsEntity`, `CachedGenre` (`@Serializable`), `MovieDetailsConverters` (registered on the entity) and `MovieDetailsDao`, plus the domain ↔ entity mappers.
6. Register the entity in `MovieDatabase`, bump the version to 2, add `movieDetailsDao()`, and provide the DAO in `DatabaseModule`. Build once so Room exports `2.json`.
7. Write `MovieDetailsRepositoryImpl` (a cold `flow`: cache → network → save → emit) with guarded cache access, and its fake-based tests.
8. Add `DetailsModule` (API plus repository binding).
9. Add `MovieDetailsDaoTest` and run it with `AppGraphTest` on an emulator.
