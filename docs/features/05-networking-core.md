# 05: Networking core (T0.5)

## 1. What was built
The shared TMDb networking stack that every feature API uses (R23):
- an **OkHttp client** with three interceptors:
  - **authentication**: a Bearer token header;
  - **content filtering**: `include_adult=false`, `certification_country=US` and `certification.lte=PG-13` on *every* request, as the PDF requires (R24–R26);
  - **language**: `en-US` or `ar-SA`, following the app locale (Q14);
- **debug-only HTTP logging** with the `Authorization` header redacted;
- a **Retrofit** instance with a kotlinx.serialization converter and lenient JSON parsing;
- **`TmdbImageUrls`**, which turns TMDb image paths into full poster and backdrop URLs;
- a **Hilt `NetworkModule`** that provides all of the above.

## 2. Files created or changed

| Layer | File | Change |
|-------|------|--------|
| Core / network | `core/network/TmdbConfig.kt` | New. API and image base URLs. |
| Core / network | `core/network/TmdbAccessToken.kt` | New. Qualifier for the injected token. |
| Core / network | `core/network/TmdbHttpClientFactory.kt` | New. Builds the `Json`, the `OkHttpClient` and the `Retrofit` instance. Used by both DI and tests. |
| Core / network | `core/network/TmdbImageUrls.kt` | New. Poster and backdrop URL builder with size presets. |
| Core / network | `core/network/interceptor/AuthInterceptor.kt` | New. `Authorization: Bearer …`, `Accept: application/json`. |
| Core / network | `core/network/interceptor/ContentFilterInterceptor.kt` | New. Enforces R24–R26. |
| Core / network | `core/network/interceptor/LanguageInterceptor.kt` | New. `language=en-US` or `ar-SA`. |
| Core / common | `core/common/locale/LocaleProvider.kt` | New. `fun interface` that returns the current locale. |
| Core / DI | `core/di/NetworkModule.kt` | New. Provides the token, `LocaleProvider`, `Json`, `OkHttpClient` and `Retrofit`. |
| Tests | `test/.../core/network/TmdbHttpClientFactoryTest.kt` | New. 9 MockWebServer tests. |
| Tests | `test/.../core/network/TmdbImageUrlsTest.kt` | New. 5 tests. |

## 3. Data flow walkthrough
A request from a feature API (added in T1.2) goes through this chain:

```
MoviesApi.discoverMovies(page = 2)            GET /3/discover/movie?page=2
  → AuthInterceptor                           + Authorization: Bearer <token>
  → ContentFilterInterceptor                  + include_adult=false&certification_country=US&certification.lte=PG-13
  → LanguageInterceptor                       + language=en-US        (or ar-SA)
  → HttpLoggingInterceptor (debug only)       logs with Authorization: ██
  → TMDb
  ← JSON → kotlinx.serialization (lenient) → DTO
```

The content filter uses `setQueryParameter`, so it **overwrites** conflicting values instead of adding duplicates:

```kotlin
val url = original.url.newBuilder().apply {
    REQUIRED_QUERY_PARAMETERS.forEach { (name, value) -> setQueryParameter(name, value) }
}.build()
```

The language is read **per request** through `LocaleProvider`, so a language switch (extra E2) affects the very next request without rebuilding the client.

**Result of a real request** (a one-off check with the developer's token; the temporary test was deleted afterwards):

```
lang=en status=200 query=sort_by=popularity.desc&page=1&include_adult=false&certification_country=US&certification.lte=PG-13&language=en-US
        total_pages=1001 results=20 first="Project Hail Mary"
lang=ar status=200 …&language=ar-SA  results=20 first="مشروع هيل ماري"
```

## 4. Decisions made, and alternatives considered
- **Interceptors instead of parameters on each Retrofit method.** R24–R26 apply to every request. Putting them in one interceptor makes it impossible to forget them on a new endpoint, and one test covers them all. The trade-off is that the parameters don't show up in the API interfaces, so a KDoc comment documents them.
- **Filters are sent even where TMDb ignores them.** `/search/movie` and `/movie/{id}` ignore the certification parameters (Q2), but the PDF says "MUST include" for all requests, so they're sent anyway. Search can therefore only exclude adult content. This limitation is documented here and will get an ADR in T4.2.
- **A factory object shared by Hilt and the tests.** The tests check the exact production configuration (interceptor order, logging and JSON settings), not a copy of it.
- **Lenient JSON settings** (`ignoreUnknownKeys`, `explicitNulls = false`, `coerceInputValues`). New TMDb fields or unexpected `null`s don't crash parsing. Checking that the *content* makes sense (blank titles, bad dates) happens in the mappers (R22, T1.2).
- **`Locale.getDefault()` as the locale source.** It follows per-app language changes on Android 13+ and AppCompat's locale handling below that. Hiding it behind `LocaleProvider` lets tests switch locales.
- **Token redaction in logs.** `HttpLoggingInterceptor` at `BODY` level is very useful for debugging, but it would print the token. `redactHeader("Authorization")` prevents that, and a test enforces it.
- **No `Coil` client yet.** Image requests go to a different host (`image.tmdb.org`) and must not receive the auth header. Coil gets its own setup with the design system (T0.8/T1.5).

**Finding for later tasks:** TMDb reports `total_pages=1001` for `/discover/movie`, but as far as I know it only serves pages up to 500. T1.3's `RemoteMediator` therefore treats `min(total_pages, 500)` as the last page.

## 5. How it is tested
`TmdbHttpClientFactoryTest` sends real HTTP calls through the production client to a local **MockWebServer**:

| Test | Requirement |
|------|-------------|
| Every request carries the three content-filter parameters | R24, R25, R26 |
| Conflicting caller values (`include_adult=true`, `certification.lte=R`) are overwritten, with no duplicates | R24, R26 |
| The Bearer token is sent in a header and never in the URL | R23 |
| `en`, `ar` and unsupported (`fr`) locales map to `en-US`, `ar-SA` and `en-US` | Q14 |
| The language follows a locale change between two requests | Q14 |
| HTTP logs contain the `Authorization` line but never the token | Security |
| Retrofit with the lenient JSON settings ignores unknown keys and turns `null` into defaults | R22 |

`TmdbImageUrlsTest` covers the default and explicit sizes, paths without a leading slash, and `null` or blank paths (which give `null`, so the UI shows a placeholder).

Results: 17 of 17 unit tests pass (`testDebugUnitTest`), and `assembleDebug` passes. A manual one-off request to the real API returned **200** in both languages (above).

## 6. How to build it from scratch
1. Create `TmdbConfig` with `API_BASE_URL = "https://api.themoviedb.org/3/"` and `IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"`.
2. Write `AuthInterceptor(token)`, which sets the `Authorization: Bearer` and `Accept` headers.
3. Write `ContentFilterInterceptor`, which calls `setQueryParameter` for the three required parameters on every request URL.
4. Create a `LocaleProvider` fun interface, and `LanguageInterceptor(localeProvider)` that maps `ar` to `ar-SA` and everything else to `en-US`.
5. Create `TmdbHttpClientFactory` with the lenient `Json`, `okHttpClient(token, localeProvider, logger?)` (timeouts, the three interceptors, and an optional redacting logger), and `retrofit(baseUrl, client)` with `json.asConverterFactory("application/json".toMediaType())`.
6. Create `TmdbImageUrls.poster/backdrop(path, size)`, which return `null` for missing paths.
7. Create `NetworkModule` in `SingletonComponent`: the token from `BuildConfig`, `LocaleProvider { Locale.getDefault() }`, a logger only when `BuildConfig.DEBUG`, and a singleton client and Retrofit.
8. Write the MockWebServer tests: enqueue a response, run a request through the real client, then check the `RecordedRequest`'s URL and headers.
