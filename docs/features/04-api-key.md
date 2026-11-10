# 04: API key handling (T0.4)

## 1. What was built
A secure way to give the app its TMDb credentials (R23, R45):
- the TMDb **v4 Read Access Token** is read at build time and exposed as `BuildConfig.TMDB_ACCESS_TOKEN`;
- it is never committed: it lives in the git-ignored `local.properties`, a Gradle property, or a CI environment variable;
- a missing token prints a clear Gradle warning instead of failing the build;
- the README has a "Configuration" section explaining how to set the token.

## 2. Files created or changed

| Layer | File | Change |
|-------|------|--------|
| Build | `app/build.gradle.kts` | Token lookup (local.properties → Gradle property → environment variable), a warning when it's missing, `buildConfig = true`, and the `TMDB_ACCESS_TOKEN` build config field. |
| Docs | `README.md` | New draft containing the Configuration and Build/test sections. |
| Docs | `docs/features/04-api-key.md` | New: this document. |

`.gitignore` already excluded `local.properties`, so it needed no change (checked with `git check-ignore -v local.properties`).

## 3. Data flow walkthrough
```
local.properties / -P / env var
        │  (Gradle configuration phase, app/build.gradle.kts)
        ▼
buildConfigField("String", "TMDB_ACCESS_TOKEN", "\"…\"")
        │  (generateDebugBuildConfig)
        ▼
BuildConfig.TMDB_ACCESS_TOKEN
        │  (T0.5: injected into AuthInterceptor via the network DI module)
        ▼
Authorization: Bearer <token>   on every TMDb request
```

Key snippet:

```kotlin
val tmdbAccessToken: String = localProperties.getProperty(tmdbTokenKey)
    ?: providers.gradleProperty(tmdbTokenKey).orNull
    ?: providers.environmentVariable(tmdbTokenKey).orNull
    ?: ""
```

Only the network layer will read `BuildConfig`, and it passes the token to `AuthInterceptor` through DI. Nothing else in the app touches it, so tests can provide their own fake token.

## 4. Decisions made, and alternatives considered
- **v4 Read Access Token as a Bearer header, instead of the v3 `api_key` query parameter (Q11).** A header keeps the secret out of URLs, so it doesn't show up in logged URLs or Coil's image cache keys. TMDb recommends this approach for new integrations.
- **Warn instead of failing the build.** Failing would break unit-test runs and CI jobs that never call the network. The missing token shows up at runtime as a clear `Unauthorized` domain error (T0.6).
- **Quotes and whitespace are stripped.** `.properties` files keep quotes literally, so `TMDB_ACCESS_TOKEN="eyJ…"` would send `"eyJ…"` and get a 401. This happened during setup. The value is trimmed and any surrounding `"` or `'` is removed, so either way of writing it works.
- **Manual `Properties` reading instead of the Secrets Gradle Plugin.** It's only a few lines, needs no extra plugin to keep compatible with AGP 9, and the lookup order is easy to see.
- **Limitation (documented, accepted):** anything put in `BuildConfig` can be pulled out of a decompiled APK. That's acceptable for a free, read-only TMDb token in an assessment project. A production app would route requests through its own backend.

## 5. How it is tested
- Build **without** a token: `assembleDebug` succeeds, the warning is printed, and the generated `BuildConfig.java` contains `TMDB_ACCESS_TOKEN = ""`.
- Build **with** a token (`-PTMDB_ACCESS_TOKEN=dummy-test-token`): the generated field contains the dummy value.
- **With the real token** in `local.properties` (wrapped in quotes): the generated field holds the 244-character `eyJ…` token with no quotes, and a manual `curl` to `/discover/movie` with it returns HTTP 200.
- `git check-ignore -v local.properties` confirms the token file is ignored.
- `testDebugUnitTest` still passes.
- An end-to-end check (a real request returning 200) comes in T0.5, once networking exists.

## 6. How to build it from scratch
1. Confirm `local.properties` is in `.gitignore`.
2. At the top of `app/build.gradle.kts`, load `rootProject.file("local.properties")` into `java.util.Properties`.
3. Resolve the token: `localProperties.getProperty(key) ?: providers.gradleProperty(key).orNull ?: providers.environmentVariable(key).orNull ?: ""`.
4. If it's blank, `logger.warn(...)` with instructions.
5. In `android { buildFeatures { buildConfig = true } }`, and in `defaultConfig`, add `buildConfigField("String", "TMDB_ACCESS_TOKEN", "\"$token\"")`.
6. Document the setup in the README "Configuration" section.
7. Build without and with the token, and check `app/build/generated/source/buildConfig/debug/.../BuildConfig.java`.
