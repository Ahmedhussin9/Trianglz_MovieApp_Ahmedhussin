# Movie App

An Android app that lists popular movies from [TMDb](https://www.themoviedb.org/), shows their details, and searches by title, with offline caching. Built with Kotlin, Jetpack Compose, Clean Architecture, MVI and Hilt.

> This README is a draft. The full version (features, architecture, screenshots) is written in task T4.3 of [`docs/PLAN.md`](docs/PLAN.md).

## Requirements
- Android Studio with support for AGP 9.2.1
- JDK 17 or newer (Android Studio's bundled JDK 21 works)
- A free TMDb account

## Configuration

The app authenticates to the TMDb API with a **v4 Read Access Token**. The token is never committed to the repository.

1. Sign in at [themoviedb.org](https://www.themoviedb.org/) and open **Settings → API**.
2. Copy the **API Read Access Token**. It's the long token that starts with `eyJ`, not the shorter "API Key".
3. Add it to `local.properties` in the project root. That file is git-ignored.

   ```properties
   TMDB_ACCESS_TOKEN=eyJhbGciOiJIUzI1NiJ9...
   ```

   Quotes around the value are optional; they are stripped.

4. Sync Gradle and run the app.

**Other ways to provide the token** (checked in this order after `local.properties`):

| Source | Example | Typical use |
|--------|---------|-------------|
| Gradle property | `./gradlew assembleDebug -PTMDB_ACCESS_TOKEN=eyJ...`, or a line in `~/.gradle/gradle.properties` | Keep one token for all your local projects |
| Environment variable | `TMDB_ACCESS_TOKEN=eyJ... ./gradlew assembleDebug` | CI (for example, a GitHub Actions secret) |

If no token is found, the build still succeeds and prints a warning. The app then can't load any data, and every request fails as *Unauthorized*.

## Build and test

```bash
./gradlew assembleDebug          # build the debug APK
./gradlew testDebugUnitTest      # unit tests
./gradlew connectedDebugAndroidTest   # instrumentation/UI tests (device or emulator required)
./gradlew createDebugUnitTestCoverageReport   # JaCoCo report → app/build/reports/coverage/test/debug/index.html
```

No TMDb token is needed to run any test: unit tests use MockWebServer and fakes, and instrumented tests run against a Hilt test graph.

On Windows, use `gradlew.bat`.
