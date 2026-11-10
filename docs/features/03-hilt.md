# 03: Hilt dependency injection (T0.3)

## 1. What was built
Hilt is the app's dependency injection framework (R20):
- `MovieApplication` creates the root (singleton) container;
- `MainActivity` is an injection entry point, so ViewModels can be injected into the Compose screens it hosts;
- coroutine dispatchers are provided through qualifiers (`@IoDispatcher`, `@DefaultDispatcher`), so no class hardcodes `Dispatchers.IO` (R15, CLAUDE.md).

## 2. Files created or changed

| Layer | File | Change |
|-------|------|--------|
| App | `MovieApplication.kt` | New. `@HiltAndroidApp` application class. |
| App | `AndroidManifest.xml` | `android:name=".MovieApplication"`. |
| App | `MainActivity.kt` | `@AndroidEntryPoint`. |
| Core / common | `core/common/dispatcher/DispatcherQualifiers.kt` | New. `@IoDispatcher` and `@DefaultDispatcher` qualifiers. |
| Core / DI | `core/di/DispatchersModule.kt` | New. Provides `Dispatchers.IO` and `Dispatchers.Default` in `SingletonComponent`. |
| Tests | `test/.../core/di/DispatchersModuleTest.kt` | New. Checks that each qualifier gets the right dispatcher. |

## 3. Data flow walkthrough
No feature data flows yet. This is how later classes will receive a dispatcher:

```kotlin
class MoviesRemoteDataSource @Inject constructor(
    private val api: MoviesApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun discover(page: Int) = withContext(ioDispatcher) { api.discoverMovies(page) }
}
```

In unit tests, the same class is constructed by hand with a `StandardTestDispatcher`, so the tests control time and threads.

## 4. Decisions made, and alternatives considered
- **Qualifier annotations instead of a `DispatcherProvider` interface.** Both make dispatchers injectable. Qualifiers need no extra type, and the dispatcher a class needs is visible right in its constructor. A provider interface would add an abstraction that only ever has one production implementation.
- **No `@MainDispatcher` qualifier.** ViewModels use `viewModelScope`, which already runs on `Dispatchers.Main.immediate`, and tests replace Main with `Dispatchers.setMain` (`MainDispatcherRule`, T0.10). A qualifier would never be used, which CLAUDE.md treats as dead code.
- **No application `CoroutineScope` yet.** Nothing needs work that outlives a screen yet. It can be added when a real use appears.
- **Hilt over manual DI or Koin.** The PDF names Hilt as its example. Hilt checks the whole dependency graph at compile time and works with ViewModels, Navigation 3 entries and instrumented tests (`HiltAndroidRule`).

## 5. How it is tested
- **Compile-time check:** Hilt validates the dependency graph during `hiltJavaCompileDebug`, so a missing or duplicate binding fails the build.
- **Unit test:** `DispatchersModuleTest` checks that each qualifier provides the intended dispatcher.
- **On a device:** the app launches. If `MovieApplication` weren't registered, `@AndroidEntryPoint` would crash on start.
- An instrumented injection test with `HiltAndroidRule` comes in T0.10, together with the custom Hilt test runner.

## 6. How to build it from scratch
1. Apply the `com.google.dagger.hilt.android` and KSP plugins, and add `hilt-android` plus `ksp(hilt-android-compiler)` (done in T0.2).
2. Create `MovieApplication : Application()` annotated with `@HiltAndroidApp`.
3. Register it in the manifest: `<application android:name=".MovieApplication" …>`.
4. Annotate `MainActivity` with `@AndroidEntryPoint`.
5. Declare the `@Qualifier` annotations `IoDispatcher` and `DefaultDispatcher` with `BINARY` retention.
6. Create `DispatchersModule`, an `object` with `@Module @InstallIn(SingletonComponent::class)` whose `@Provides` functions return `Dispatchers.IO` and `Dispatchers.Default` under those qualifiers.
7. Build. Hilt generates `Hilt_MainActivity` and the components, and reports any graph errors.
