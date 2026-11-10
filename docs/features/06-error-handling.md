# 06: Error handling core (T0.6)

## 1. What was built
The error-handling layer the PDF asks for: network and HTTP exceptions are mapped to clear **domain errors**, so the UI handles every state without complex logic (R21).
- **`DomainError`**: a closed set of 7 user-meaningful failures (pure Kotlin).
- **`AppResult<T>`**: `Success(data)` or `Failure(DomainError)`, the return type for one-shot operations.
- **`DomainException`**: carries a `DomainError` through APIs that only accept a `Throwable`, such as Paging's `LoadState.Error`.
- **`Throwable.toDomainError()`**: the single mapping point in the data layer.
- **`safeApiCall { }`**: runs a call and returns an `AppResult`, always letting coroutine cancellation through.
- **`DomainError.messageRes()`**: one localized message per error, in English and Arabic.

## 2. Files created or changed

| Layer | File | Change |
|-------|------|--------|
| Domain (pure Kotlin) | `core/domain/error/DomainError.kt` | New. Sealed interface with `NoInternet`, `Timeout`, `Unauthorized`, `NotFound`, `RateLimited`, `Server` and `Unknown`. |
| Domain (pure Kotlin) | `core/domain/error/DomainException.kt` | New. A `Throwable` that carries a `DomainError`. |
| Domain (pure Kotlin) | `core/domain/result/AppResult.kt` | New. `Success` / `Failure`, plus `map`. |
| Data | `core/network/error/ErrorMapper.kt` | New. `Throwable.toDomainError()`, HTTP code mapping, `toDomainException()`. |
| Data | `core/network/error/SafeApiCall.kt` | New. `safeApiCall { }`. |
| Presentation | `core/ui/error/ErrorMessages.kt` | New. `DomainError.messageRes()`. |
| Resources | `values/strings.xml`, `values-ar/strings.xml` (new) | 7 error messages in English and Arabic. `app_name` is marked `translatable="false"`. |
| Tests | `ErrorMapperTest`, `SafeApiCallTest`, `AppResultTest`, `ErrorMessagesTest` | New. 21 tests. |

`AppResult` lives in `core/domain/result`, not `core/common` as first sketched in PLAN.md, because domain repository interfaces return it and the domain layer may only depend on domain code.

## 3. Data flow walkthrough

```
OkHttp / Retrofit / kotlinx.serialization          (data layer)
   │  throws UnknownHostException / HttpException(404) / SocketTimeoutException …
   ▼
Throwable.toDomainError()  ──►  DomainError.NotFound            (single mapping point)
   │
   ├─ one-shot call:   safeApiCall { api.details(id) }  ──►  AppResult.Failure(NotFound)
   └─ paging:          LoadResult.Error(e.toDomainException())  ──►  LoadState.Error(DomainException)
   ▼
ViewModel             only ever sees DomainError                  (presentation)
   ▼
UI                    stringResource(error.messageRes())  →  "We couldn't find what you're looking for."
```

The mapping order matters, because timeouts are subclasses of `IOException`:

```kotlin
fun Throwable.toDomainError(): DomainError = when (this) {
    is DomainException -> error
    is HttpException -> code().toDomainError()        // 401/403, 404, 429, 5xx, other
    is InterruptedIOException -> DomainError.Timeout  // SocketTimeoutException + OkHttp call timeout
    is IOException -> DomainError.NoInternet
    else -> DomainError.Unknown                       // includes malformed JSON
}
```

**HTTP status mapping**

| Status | DomainError | Message (EN) |
|--------|-------------|--------------|
| 401, 403 | `Unauthorized` | "Couldn't connect to the movie service. Check the TMDb access token." |
| 404 | `NotFound` | "We couldn't find what you're looking for." |
| 429 | `RateLimited` | "Too many requests. Please wait a moment and try again." |
| 500–599 | `Server` | "The movie service is having problems. Please try again later." |
| other | `Unknown` | "Something went wrong. Please try again." |

## 4. Decisions made, and alternatives considered
- **A custom `AppResult` instead of `kotlin.Result`.** `Result`'s failure is any `Throwable`, so the UI would have to inspect exceptions again. `AppResult.Failure` only holds a `DomainError`, and `when` over it is exhaustive.
- **Data objects without payloads.** The UI only needs to know *which* failure happened in order to choose a message and offer Retry. HTTP codes and causes stay in the data layer. `DomainException` keeps the original `cause` for logging and debugging.
- **`CancellationException` is always rethrown** in `safeApiCall`. Catching it would turn "the user left the screen" into a fake error and break structured concurrency (R15). A test enforces this.
- **Malformed JSON maps to `Unknown`, not `Server`.** The user can't do anything different either way. Checking content (blank titles, bad dates) is a separate concern, handled by the mappers in T1.2 (R22).
- **Messages live in the presentation layer** (`core/ui/error`), because `@StringRes` is Android. The domain stays free of Android imports.
- **`Unauthorized` mentions the token.** In this project the only realistic cause is a missing or wrong token, so the message tells a reviewer exactly what to fix (see README › Configuration).

## 5. How it is tested

| Test class | What it checks |
|------------|----------------|
| `ErrorMapperTest` (15) | Unknown host, refused connection and other `IOException`s map to `NoInternet`. A socket timeout and the OkHttp call timeout map to `Timeout`, not `NoInternet`. HTTP 401/403/404/429/5xx/other map correctly. Malformed JSON and unexpected exceptions map to `Unknown`. A `DomainException` is not remapped. `toDomainException()` keeps the cause and returns existing instances unchanged. |
| `SafeApiCallTest` (3) | Success wraps the value, failure wraps the mapped error, and cancellation propagates. |
| `AppResultTest` (2) | `map` transforms successes and passes failures through without calling the transform. |
| `ErrorMessagesTest` (1) | Every error has a distinct message. Exhaustiveness is enforced at compile time. |

Results: 38 of 38 unit tests pass, `assembleDebug` passes, and `lintDebug` reports 0 errors. The 10 lint warnings are template leftovers, cleaned up in T0.8, plus the deliberate targetSdk and AGP versions.

## 6. How to build it from scratch
1. In `core/domain/error`, create `sealed interface DomainError` with one `data object` per user-meaningful failure.
2. Create `class DomainException(val error: DomainError, cause: Throwable?) : Exception`.
3. In `core/domain/result`, create `sealed interface AppResult<out T>` with `Success(data)` and `Failure(error)`, plus an inline `map`.
4. In `core/network/error`, write `Throwable.toDomainError()` with timeouts **before** `IOException`, and `Int.toDomainError()` for HTTP codes.
5. Add `Throwable.toDomainException()` for Paging sources.
6. Write `safeApiCall` with `catch (CancellationException) { throw it }` before the general catch.
7. In `core/ui/error`, write the exhaustive `DomainError.messageRes()`, and add the strings to `values/` and `values-ar/`.
8. Write one test per mapping branch, including the cancellation test.
