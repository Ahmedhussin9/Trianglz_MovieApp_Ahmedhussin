# 11b: Logging (T0.11)

> Added during Phase 1 at the developer's request and done before T1.3, so the cache work can be followed in Logcat. It's numbered 11b to stay in task order between docs 11 and 12.

## 1. What was built
App-wide logging for development (R21, R36, R37):
- **Timber 5.0.1**, planted **only in debug builds**. Release builds plant nothing, so every log call does nothing there.
- **HTTP traffic** through Timber under the tag `TmdbHttp`: URL, headers (token redacted), status, timing and the **full JSON body**. Timber splits the body into chunks because Logcat cuts each entry at about 4 KB.
- **Handled failures** are logged as warnings **with the original exception** at the two places every error passes through (`safeApiCall`, `toDomainException`). The UI only sees a `DomainError`, but Logcat still shows the root cause and stack trace.
- A **logging convention** in CLAUDE.md for all later tasks.

## 2. Files created or changed

| Layer | File | Change |
|-------|------|--------|
| Build | `gradle/libs.versions.toml`, `app/build.gradle.kts` | `timber 5.0.1`. |
| App | `MovieApplication.kt` | `onCreate()` plants `Timber.DebugTree()` when `BuildConfig.DEBUG`. |
| Core / DI | `core/di/NetworkModule.kt` | The HTTP logger uses `Timber.tag("TmdbHttp").d(…)` instead of `Log.d`. |
| Data / errors | `core/network/error/SafeApiCall.kt` | `Timber.w(e, "API call failed → %s", error)` before returning `Failure`. |
| Data / errors | `core/network/error/ErrorMapper.kt` | `toDomainException()` logs a raw failure once and returns existing `DomainException`s unlogged. |
| Tests | `test/.../testing/RecordingTree.kt` | New. A Timber tree that records log calls. |
| Tests | `test/.../core/network/error/ErrorLoggingTest.kt` | New. 5 tests. |
| Docs | `CLAUDE.md`, `README.md` (Debugging section), `docs/PLAN.md` (T0.11) | Convention, Logcat filters, task entry. |

## 3. Data flow walkthrough

**How a log call travels:**

```
Timber.w(e, "API call failed → %s", error)
   │
   ├─ debug build:   Timber → DebugTree → android.util.Log.w(tag = "SafeApiCallKt", message + stack trace)
   │                 (tag = calling class, long messages split into ≤4 KB chunks)
   │
   └─ release build: no tree planted → the call does nothing
```

**One failed request, end to end** (captured on the emulator with network off):

```
W/SafeApiCallKt: API call failed → NoInternet
W/SafeApiCallKt: java.net.UnknownHostException: Unable to resolve host "api.themoviedb.org": No address associated with hostname
W/SafeApiCallKt:     at java.net.Inet6AddressImpl.lookupHostByName(Inet6AddressImpl.java:156)
…
App result:  Failure(error=NoInternet)        ← the UI sees only the domain error
```

**One successful request** (online):

```
D/TmdbHttp: --> GET https://api.themoviedb.org/3/discover/movie?page=1&sort_by=popularity.desc&include_adult=false&certification_country=US&certification.lte=PG-13&language=en-US
D/TmdbHttp: Authorization: ██
D/TmdbHttp: --> END GET
D/TmdbHttp: <-- 200 https://api.themoviedb.org/3/discover/movie?… (618ms)
D/TmdbHttp: {"page":1,"results":[{…  ← 13 KB body in ~4 KB chunks, complete up to "total_results"
D/TmdbHttp: <-- END HTTP (619ms, 13206-byte body)
```

## 4. Decisions made, and alternatives considered
- **Timber instead of `android.util.Log` or a custom wrapper.**
  - It **splits long messages into chunks**. With plain `Log.d`, TMDb's 13 KB pages were cut off at about 4 KB.
  - Tags come automatically from the calling class.
  - Release builds are silenced in one place.
  - Its lint check flags stray `android.util.Log` calls.
  - In JVM unit tests it does nothing unless a test plants a tree.

  A custom `Logger` interface would mostly re-implement Timber.
- **Log at the error boundaries, not inside `toDomainError()`.** The mapper stays a pure function with no side effects. Every failure goes through `safeApiCall` (one-shot calls) or `toDomainException()` (Paging), so each one is logged **exactly once**, with its original exception.
- **No logs for cancellation.** Leaving a screen isn't a failure, and logging it would add noise.
- **No logging in `domain/`.** The domain stays pure Kotlin with no logging dependency. Logging belongs where I/O and UI events happen.
- **HTTP at `BODY` level only in debug.** It's useful for checking what TMDb really returns. The token is redacted, a test enforces that, and on the device the token string appeared 0 times in Logcat. Release builds have no logger at all.
- **No crash-reporting tree yet.** A release tree that forwards `w` and `e` logs to Crashlytics could be added later. It isn't required by the task.

## 5. How it is tested
**`ErrorLoggingTest`** (5, with a `RecordingTree` planted):
- a failed `safeApiCall` logs exactly one `WARN` with the original exception and "API call failed → NoInternet";
- a successful call logs nothing;
- cancellation logs nothing;
- `toDomainException()` logs a raw failure once;
- an existing `DomainException` isn't logged again.

**On the emulator** (a temporary trigger in `MainActivity`, removed before committing; the restored file matches the committed version exactly):
- **Online:** the full request and response appear under `TmdbHttp`, with all filters, `Authorization: ██`, a 200 in 618 ms, and the 13 KB body complete in 4 KB chunks. The token string appears 0 times in Logcat.
- **Offline** (Wi-Fi and data off): `W/SafeApiCallKt: API call failed → NoInternet` with the full `UnknownHostException` stack trace.

**Build checks:** `assembleDebug` passes, 67 of 67 unit tests pass, and `lintDebug` reports 0 errors.

## 6. How to build it from scratch
1. Add `com.jakewharton.timber:timber` to the catalog and the app module.
2. In `Application.onCreate()`, call `if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())`.
3. Route OkHttp's `HttpLoggingInterceptor.Logger` to `Timber.tag("TmdbHttp").d(message)`, keeping `redactHeader("Authorization")`.
4. At each error boundary, log `Timber.w(throwable, "… → %s", domainError)` before reducing the error. Skip cancellation, and skip errors that are already wrapped.
5. Write a `RecordingTree` for tests, `plant` it in `@Before` and `uprootAll()` in `@After`, and check priority, message and throwable.
6. Check on a device with Logcat filters `tag:TmdbHttp` and `level:warn`, online and offline.
