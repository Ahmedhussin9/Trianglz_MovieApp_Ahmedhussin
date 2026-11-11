# 13: Movies list, offline cache with Room and RemoteMediator (T1.3, includes T0.7)

## 1. What was built
The caching flow the PDF requires (R3, R7–R10, R38). It also includes the Room setup moved here from T0.7.
- **Room database** (`MovieDatabase`) with two tables. The schema is exported to `app/schemas/`.
  - `movies`: validated movies plus a `position` column that keeps TMDb's popularity order.
  - `remote_keys`: the next page to fetch for the list.
- **`MoviesRemoteMediator`**: keeps Room in sync with TMDb.
  - On start it shows the cache and refreshes in the background (R8, R9).
  - A successful refresh replaces the cache, and the UI updates (R10).
  - A failed refresh keeps the cache and reports a `DomainError`.
  - Pages are appended as the user scrolls (R3).
- **`MoviesRepositoryImpl`**: a `Pager` whose single source of truth is Room, filled by the mediator. It's bound to `MoviesRepository` in Hilt.
- **Logging**: each refresh or append logs its page and how many movies were cached. Failures are logged through `toDomainException` (T0.11).

## 2. Files created or changed

| Layer | File | Change |
|-------|------|--------|
| Core / database | `core/database/MovieDatabase.kt` | New. `@Database(entities = [MovieEntity, RemoteKeyEntity], version = 1, exportSchema = true)`. |
| Core / DI | `core/di/DatabaseModule.kt` | New. Database (destructive migration, since it's a cache) and DAOs. |
| Data / local | `feature/movies/data/local/{MovieEntity, RemoteKeyEntity, MovieDao, RemoteKeyDao}.kt` | New. |
| Data / mapper | `feature/movies/data/mapper/MovieEntityMapper.kt` | New. `Movie.toEntity(position)`, `MovieEntity.toDomain()`. |
| Data / mediator | `feature/movies/data/mediator/MoviesRemoteMediator.kt` | New. |
| Data / repository | `feature/movies/data/repository/MoviesRepositoryImpl.kt` | New. |
| DI | `feature/movies/di/MoviesModule.kt` | Binds `MoviesRepository` to `MoviesRepositoryImpl`. |
| Build | `gradle/libs.versions.toml`, `app/build.gradle.kts` | `kotlinx-coroutines-android` 1.11.0 (see Decisions), plus `paging-testing` for `androidTest`. |
| Schema | `app/schemas/…MovieDatabase/1.json` | New. Generated, and committed for review. |
| Tests | `androidTest/.../testing/{FakeMoviesApi, DatabaseTestUtils}.kt` | New test helpers. |
| Tests | `androidTest/.../MovieDaoTest.kt` (5), `MoviesRemoteMediatorTest.kt` (12), `MoviesRepositoryImplTest.kt` (3) | New. Instrumented, with in-memory SQLite. |
| Tests | `test/.../MovieEntityMapperTest.kt` (1) | New. |

## 3. Data flow walkthrough

```
UI ◄── PagingData<Movie> ◄── Pager ◄── Room PagingSource (SELECT * FROM movies ORDER BY position)
                                │              ▲ invalidated on every write → UI updates (R10)
                                ▼              │
                      MoviesRemoteMediator ────┘ writes in one transaction
                                │
                     MoviesRemoteDataSource (T1.2) → TMDb /discover/movie
```

**App start** (`initialize()` → `LAUNCH_INITIAL_REFRESH`): Room's `PagingSource` loads the cached rows **at once**, while the mediator's `REFRESH` fetches page 1 in the background (R8, R9).

**Refresh succeeded**, in one transaction:
```kotlin
database.withTransaction {
    if (loadType == LoadType.REFRESH) movieDao.clearAll()
    val firstPosition = (movieDao.maxPosition() ?: -1) + 1
    movieDao.insertAll(result.movies.mapIndexed { i, movie -> movie.toEntity(firstPosition + i) })
    remoteKeyDao.upsert(RemoteKeyEntity(LIST_ID, nextPage = if (result.isLastPage) null else page + 1))
}
```

**Refresh failed:** nothing is deleted. The mediator returns `MediatorResult.Error(DomainException)`, so the UI keeps showing the cache and gets `loadState.mediator.refresh = Error(NoInternet)` for a non-blocking banner (T1.5).

**Append:** reads `remote_keys.nextPage`. It loads nothing when that's `null` (end reached, including TMDb's 500-page cap) or when no key exists yet.

**Observed on the emulator against the real TMDb API** (temporary trigger, removed before committing):

| Launch | Logcat |
|--------|--------|
| 1. Online, empty cache | `REFRESH: cached 20 movies from page 1` → shown 20 → `APPEND page 2` → shown **39** (a cross-page duplicate was kept once) → pages 3–4 → shown 60 |
| 2. Offline | the **cached 60 shown immediately**; `W/ErrorMapperKt: Load failed → NoInternet`; `mediator.refresh = Error` |
| 3. Online again | the **cached 60 shown immediately** while page 1 refreshes; then the fresh data replaces the cache (20, refilling page by page) |

The first load fills 60 items without scrolling because Paging's default `initialLoadSize` is 3 × `pageSize`. That's normal prefetching for smooth scrolling.

## 4. Decisions made, and alternatives considered
- **`RemoteMediator` instead of a hand-written "cache, then network" flow.** Paging already handles refresh and append coordination, retry, load states and database invalidation. Room as the single source of truth means the UI never has to merge two sources.
- **`position` column instead of `ORDER BY popularity`.** Popularity values change between requests. Storing TMDb's order as fetched keeps the list stable while scrolling.
- **`OnConflictStrategy.IGNORE`** for cross-page duplicates. The first occurrence keeps its place, instead of the movie jumping down the list (seen live: 39 instead of 40 after page 2).
- **One remote-key row per list** instead of one key per movie. The list always grows from page 1 and never prepends, so "next page" is the only state needed. `lastUpdated` was dropped because the app always refreshes on start (R9) and nothing would read it.
- **Destructive migration.** The database only holds data that can always be downloaded again, so a schema change can rebuild it instead of needing hand-written migrations.
- **`MovieDatabase` in `core/database`.** Room needs one class that lists every entity. Entities and DAOs still live in their feature (`feature/movies/data/local`).
- **Instrumented tests instead of Robolectric** (the decision postponed from T0.10). The cache's correctness depends on real SQLite behavior (ordering, conflict handling, transactions), so DAO, mediator and repository tests run against an in-memory database on the emulator.
- **`kotlinx-coroutines-android` declared directly.** AndroidX alone resolved `coroutines-core` 1.10.2, while `coroutines-test` is 1.11.0. Instrumented tests share the app's dependencies, so they crashed with `NoSuchMethodError: runBlockingK`. Declaring 1.11.0 puts the app and the tests on the same version.
- **`PagingDataPresenter` in the offline repository test.** Paging's `asSnapshot()` throws an internal `ReturnSnapshotStub` when a `RemoteMediator` refresh fails. The public presenter, which `LazyPagingItems` is built on, checks exactly what the UI receives: the cached items and the mediator error.

## 5. How it is tested

| Suite (instrumented, Pixel 4 AVD, Android 10) | Checks |
|----------------------------------------------|--------|
| `MovieDaoTest` (5) | Order by position (not insertion order); a duplicate ID keeps the first row and position; `maxPosition` when empty and when filled; `clearAll`; remote key upsert. |
| `MoviesRemoteMediatorTest` (12) | Startup asks for an immediate refresh. Refresh caches page 1 in API order and stores next page 2. Refresh replaces the old cache. A single-page list ends pagination. **A failed refresh keeps the cache and reports `NoInternet`.** 401 gives `Unauthorized`. Append continues from the stored page with the right positions. A repeated movie keeps its original place. No network call after the last page or without a key. A failed append keeps pages and the key for retry. Prepend never loads. |
| `MoviesRepositoryImplTest` (3) | A first online launch shows network movies. **An offline launch shows the cached movies and reports `NoInternet`.** An online launch with a cache ends with fresh movies. |

Plus `MovieEntityMapperTest` (1, JVM): a round trip from `Movie` to entity and back.

Results: `assembleDebug` passes, 68 of 68 unit tests pass, and 22 of 22 instrumented tests pass. A real-API check on the emulator was done online, offline and online again (above).

## 6. How to build it from scratch
1. Create entities `MovieEntity(id PK, …, position)` and `RemoteKeyEntity(listId PK, nextPage?)`.
2. Create DAOs: `pagingSource()` ordered by position, `insertAll` with `IGNORE`, `maxPosition`, `clearAll`, plus remote key `upsert` and `get`.
3. Create `@Database` with `exportSchema = true`, configure `room { schemaDirectory(...) }`, and provide it with Hilt (`fallbackToDestructiveMigration(dropAllTables = true)`).
4. Write the `RemoteMediator`:
   - `initialize()` returns `LAUNCH_INITIAL_REFRESH`;
   - `REFRESH` loads page 1, and `APPEND` uses the stored next page;
   - write inside `withTransaction`, clearing on refresh;
   - wrap failures with `toDomainException()`, and rethrow `CancellationException`.
5. Create the repository: `Pager(PagingConfig(20), remoteMediator, pagingSourceFactory = dao::pagingSource).flow.map { it.map(Entity::toDomain) }`, and bind it to the interface.
6. Test with an in-memory database and a fake API on a device. For the offline case, use `PagingDataPresenter` to check both the items and `mediator.refresh`.
