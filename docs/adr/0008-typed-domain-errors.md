# 0008: A closed `DomainError` type for every failure

**Status:** Accepted (Phase 0, T0.6)

## Context
Failures come from many places: no network, timeouts, HTTP status codes, malformed JSON, and the database. The task asks for proper error handling (R21), and ViewModels must not depend on Retrofit or OkHttp (Clean Architecture, R16). The UI needs a specific, localized message for each kind of failure, in English and Arabic.

## Decision
- **A sealed `DomainError` in the domain layer**, with a closed set of cases: `NoInternet`, `Timeout`, `Unauthorized`, `NotFound`, `RateLimited`, `Server`, `Unknown`.
- **The data layer converts every exception once,** with `Throwable.toDomainError()`. The order matters: timeouts are `IOException`s too, so they are matched first.
  - Non-paged calls return `AppResult<T>` (`Success` / `Failure(DomainError)`) through `safeApiCall`.
  - Paged loads, where Paging requires a `Throwable`, report `LoadState.Error(DomainException(error))`.
- **The UI maps each `DomainError` to one string resource** (`messageRes()`) and an icon, through the shared `ErrorView`, the cached-data notice and the snackbars.
- **The original exception is logged once with Timber** at the conversion point, so debugging keeps the root cause.

## Alternatives considered
- **`kotlin.Result<T>`.** Its failure is any `Throwable`, so every `when` needs an `else` branch and the UI ends up inspecting exception types.
- **Rethrowing exceptions to the ViewModel.** This couples presentation to Retrofit and OkHttp types and makes error handling easy to forget.
- **An error class per feature.** All features talk to the same API, so the same seven cases cover them all.

## Consequences
- **Every error state is exhaustive and tested.** `ErrorMapperTest` covers every exception type and status code, and the T4.1 review checked the messages on a device.
- **Adding a new kind of failure is a compile-time change:** every `when` over `DomainError` must handle it.
- **`Unknown` covers malformed data**, including a response with no usable title.
