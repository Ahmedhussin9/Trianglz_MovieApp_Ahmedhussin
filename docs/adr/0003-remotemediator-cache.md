# 0003: Room with a `RemoteMediator` for the list; a finite cache-then-network flow for details

**Status:** Accepted (Phase 1, T1.3; Phase 2, T2.1–T2.2)

## Context
The task requires a local cache with a specific flow (R7–R10): show cached data first, refresh from the network in the background, and update the UI when new data arrives. The list is paginated (R3); the details screen shows one movie. Offline, the app must still show what it has.

## Decision
**Popular list:**
- Room is the single source of truth, and Paging 3's `RemoteMediator` keeps it filled.
- The screen reads a Room `PagingSource` only.
- `MoviesRemoteMediator` refreshes page 1 on start (`LAUNCH_INITIAL_REFRESH`), replaces the cached pages in one transaction, and appends further pages as the user scrolls. Page keys are kept in a `remote_keys` table.
- Room invalidates the `PagingSource` whenever the tables change, so fresh data reaches the screen without extra code (R10).

**Details:**
- One `movie_details` row per movie.
- The repository returns a **finite** `Flow<AppResult<MovieDetails>>`: the cached details first (if any), then the network outcome, then completion.
- Completion tells the ViewModel the refresh is over. A failure after cached content becomes a snackbar instead of an error screen.

## Alternatives considered
- **A hand-written cache for the list** (load the database, call the API, write, reload). That duplicates what `RemoteMediator` does, without its integration with Paging's load states (`mediator.refresh` versus `source.refresh`), which the UI uses to tell "refreshing over a cache" from "loading with nothing to show".
- **Network-only list with OkHttp's HTTP cache.** It does not give the cache-first, refresh-in-background behavior, and TMDb's `max-age` decides what is shown offline.
- **An observable Room `Flow` for details.** It never completes, so the ViewModel would need a second signal for "refresh finished" and another for "refresh failed".

## Consequences
- **Offline works:** the list and every opened movie show from the cache, with clear messages when a refresh fails.
- **Schema changes are cheap.** Upgrades drop and recreate the tables (destructive fallback), which only costs a re-download, because the database holds nothing that cannot be fetched again.
- **TMDb's ranking can shift between pages.** Room's `IGNORE` conflict strategy keeps the first occurrence, so the list never repeats a movie.
- **A language switch makes cached text stale**, so the list is refreshed explicitly when it happens (T2.6).
