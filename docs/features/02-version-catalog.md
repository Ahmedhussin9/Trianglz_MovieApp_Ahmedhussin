# 02: Version catalog and plugins (T0.2)

## 1. What was built
All libraries and Gradle plugins the plan needs are declared in the version catalog, on their latest stable versions (checked against Google Maven and Maven Central on 2026-11-10) and chosen so they work together. The `:app` module applies every plugin and declares every dependency, so the later tasks only add code.

**Build toolchain after this task**

| Tool | Before | After | Why it changed |
|------|--------|-------|----------------|
| Android Gradle Plugin | 9.2.1 | 9.2.1 | Kept: the highest AGP the installed Android Studio supports (see Decisions). |
| Gradle | 9.4.1 | 9.4.1 | Kept: the wrapper version the template pairs with AGP 9.2.1. |
| Kotlin (built-in Kotlin compiler) | 2.2.10 | **2.4.21** | Coil 3.6.3 and kotlinx.serialization 1.12.0 are built with Kotlin 2.4. |
| KSP | none | **2.3.13** | Annotation processing for Hilt and Room. The AGP 9 minimum is 2.3.6. |
| compileSdk | 36.1 | **37.2** | The latest Compose UI, core-ktx and OkHttp releases need API 37. |
| targetSdk / minSdk | 36 / 24 | 36 / 24 | Unchanged, so runtime behaviour stays the same (Q17). |
| Java source/target | 11 | **17** | The current baseline for AGP 9 projects. The build runs on Android Studio's bundled JDK 21. |

**Libraries**

| Area | Library | Version |
|------|---------|---------|
| AndroidX | core-ktx · activity-compose · lifecycle (runtime, compose, viewmodel-compose, viewmodel-navigation3) | 1.19.1 · 1.13.0 · 2.11.0 |
| Compose | Compose BOM (ui, graphics, tooling, material3, ui-test) | 2026.09.00 |
| Navigation | navigation3-runtime / navigation3-ui | 1.2.1 |
| DI | Hilt (android, compiler, testing) · androidx.hilt lifecycle-viewmodel-compose | 2.60.1 · 1.4.0 |
| Networking | Retrofit + kotlinx-serialization converter · OkHttp BOM (okhttp, logging-interceptor, mockwebserver3) · kotlinx-serialization-json | 3.0.0 · 5.5.1 · 1.12.0 |
| Images | Coil 3 (compose, network-okhttp) | 3.6.3 |
| Paging | paging-runtime, paging-compose, paging-testing | 3.5.3 |
| Database | Room (runtime, ktx, paging, compiler, testing) and the Room Gradle plugin | 2.8.5 |
| Unit tests | JUnit 4 · MockK · Turbine · kotlinx-coroutines-test | 4.13.2 · 1.14.12 · 1.2.1 · 1.11.0 |
| Instrumented tests | AndroidX test runner · ext-junit · Espresso · mockk-android | 1.7.0 · 1.3.0 · 3.7.0 · 1.14.12 |

DataStore is not added yet, because only the extras E1, E2 and E12 use it.

## 2. Files created or changed

| Layer | File | Change |
|-------|------|--------|
| Build | `gradle/libs.versions.toml` | Rewritten and grouped by area, with all versions, libraries and plugins. |
| Build | `build.gradle.kts` (root) | `buildscript` classpath raises KGP to the catalog's Kotlin version. The new plugins are declared with `apply false`. |
| Build | `app/build.gradle.kts` | Applies the serialization, KSP, Hilt and Room plugins. compileSdk 37.2, Java 17, Room `schemaDirectory`, all dependencies. |

No Kotlin source files were changed.

## 3. Data flow walkthrough
Not applicable. This task only sets up the build.

Here is how the Kotlin version is raised under AGP 9's built-in Kotlin:

```kotlin
// build.gradle.kts (root)
buildscript {
    dependencies {
        classpath(libs.kotlin.gradle.plugin) // 2.4.21, replacing AGP's default 2.2.10
    }
}
```

`gradlew buildEnvironment` confirms the result:
`kotlin-gradle-plugin:2.2.10 -> 2.4.21` and `symbol-processing-gradle-plugin:2.2.10-2.0.2 -> 2.3.13`.

## 4. Decisions made, and alternatives considered
- **AGP stays at 9.2.1, because the IDE supports nothing newer.** AGP 9.5.1 (with Gradle 9.6.1) was tried first and built from the command line. The installed Android Studio then refused to sync ("Latest supported version is AGP 9.2.1"). AGP 9.2.1 with Gradle 9.4.1 also compiles against API 37.2 without warnings, so every library could stay on its latest stable release. AGP can be raised once Android Studio is updated.
- **Raise Kotlin and compileSdk instead of downgrading libraries.** The other option was Kotlin 2.2 and compileSdk 36.1 with older library versions. Moving forward keeps every dependency on its current stable release, which is what CLAUDE.md asks for, and needs no code changes.
- **compileSdk 37.2 with targetSdk 36.** The newer APIs are available to compile against, without opting into Android 17's runtime behaviour changes. Raising targetSdk can be its own decision later.
- **Room 2.8.5 instead of Room 3 (`androidx.room3` 3.0.3).** Room 2.x has the mature `room-paging` integration with `RemoteMediator`, which the caching flow depends on (R7–R10), and it's much better documented. Room 3 is mainly aimed at Kotlin Multiplatform, which this app doesn't need.
- **KSP for both Hilt and Room.** kapt doesn't work with AGP 9's built-in Kotlin, and KSP is faster.
- **`mockwebserver3`.** This is the OkHttp 5 MockWebServer API. The old `mockwebserver` artifact is kept only for compatibility.
- **Every dependency wired up now.** Later tasks only add code, and this build already confirms that the whole dependency set resolves and compiles together, including the instrumentation-test set.

## 5. How it is tested
- `gradlew.bat assembleDebug` passes.
- `gradlew.bat testDebugUnitTest` passes (the template test).
- `gradlew.bat assembleDebugAndroidTest` passes, which checks that the Hilt-testing, Compose-test and Room-testing dependencies resolve.
- `gradlew.bat buildEnvironment` confirms KGP 2.4.21 and KSP 2.3.13 are in use.
- On a device: the template screen still launches. Nothing user-visible changed.

## 6. How to build it from scratch
1. Look up the latest stable versions from each artifact's `maven-metadata.xml` on Google Maven or Maven Central, skipping alpha, beta and rc versions.
2. Check the `kotlin-stdlib` version in each library's POM. The highest one (2.4.10 here) sets the minimum Kotlin compiler (2.4.x).
3. Write `libs.versions.toml` with versions, libraries and plugins.
4. In the root `build.gradle.kts`, add `buildscript { dependencies { classpath(libs.kotlin.gradle.plugin) } }`, and declare the plugins with `apply false`.
5. In `app/build.gradle.kts`, apply the plugins, add `room { schemaDirectory(...) }`, set Java 17, and add the dependencies (`ksp(...)` for the Hilt and Room compilers).
6. Run `assembleDebug` and fix whatever AGP reports. Here that was `checkDebugAarMetadata` failing until compileSdk was 37 or newer.
7. Keep AGP at the highest version the installed Android Studio supports. The sync error states it.
8. Run `testDebugUnitTest`, `assembleDebugAndroidTest` and `buildEnvironment` to confirm.
