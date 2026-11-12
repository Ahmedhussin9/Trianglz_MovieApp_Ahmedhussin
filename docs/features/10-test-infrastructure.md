# 10: Test infrastructure (T0.10)

## 1. What was built
The shared foundation for every later test (R27–R30):
- **`MainDispatcherRule`**: replaces `Dispatchers.Main` with a `TestDispatcher`, so ViewModel tests run on the JVM with controllable virtual time.
- **`HiltTestRunner`**: starts instrumented tests with `HiltTestApplication`, so `@HiltAndroidTest` classes get a test component and can replace bindings with fakes.
- **The first instrumented Hilt test** (`AppGraphTest`): builds the production dependency setup on a device and injects Retrofit and both dispatchers. This is the runtime injection check postponed from T0.3.
- **Unit-test coverage**: JaCoCo through AGP's built-in `enableUnitTestCoverage`.
- **Cleanup**: the template's `ExampleUnitTest` and `ExampleInstrumentedTest` are removed.

## 2. Files created or changed

| Source set | File | Change |
|------------|------|--------|
| `test` | `testing/MainDispatcherRule.kt` | New. A JUnit `TestWatcher` that calls `setMain` / `resetMain`. |
| `test` | `testing/MainDispatcherRuleTest.kt` | New. Proves `viewModelScope` follows virtual time under the rule. |
| `androidTest` | `HiltTestRunner.kt` | New. `AndroidJUnitRunner` that uses `HiltTestApplication`. |
| `androidTest` | `core/di/AppGraphTest.kt` | New. `@HiltAndroidTest` with 2 tests. |
| Build | `app/build.gradle.kts` | `testInstrumentationRunner = "…HiltTestRunner"` and `debug { enableUnitTestCoverage = true }`. |
| Docs | `README.md` | Coverage command, plus a note that no token is needed to run tests. |
| Removed | `ExampleUnitTest.kt`, `ExampleInstrumentedTest.kt` | Template tests. |

The test dependencies themselves (Hilt testing with `kspAndroidTest`, the Compose UI test libraries, MockK, Turbine, coroutines-test, paging-testing and MockWebServer) were added in T0.2.

## 3. Data flow walkthrough
**ViewModel unit tests (from T1.4).** One `TestDispatcher` drives Main, the injected I/O dispatcher, and `runTest`, so a single scheduler controls all virtual time:

```kotlin
@get:Rule val mainDispatcherRule = MainDispatcherRule()

@Test fun `search waits for the debounce`() = runTest(mainDispatcherRule.testDispatcher) {
    val viewModel = MoviesViewModel(fakeRepository, ioDispatcher = mainDispatcherRule.testDispatcher)
    viewModel.onAction(QueryChanged("toy"))
    advanceTimeBy(349)   // nothing yet
    advanceTimeBy(2)     // debounced request fires, with no real waiting
}
```

**Instrumented tests (from T1.6).** The runner swaps the application for `HiltTestApplication`, and a test can replace any binding:

```kotlin
@HiltAndroidTest
class MoviesFlowTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)
    // @TestInstallIn(replaces = [MoviesModule::class]) → FakeMoviesRepository
}
```

## 4. Decisions made, and alternatives considered
- **`StandardTestDispatcher` by default** (not `Unconfined`). Coroutines only run when the test advances the scheduler, so debounce and ordering assertions are deterministic. A test can pass `UnconfinedTestDispatcher()` when eager execution is simpler.
- **Instrumented tests on a real device or emulator, not Robolectric (for now).** The PDF evaluates "Instrumentation/UI tests" (R30), and real-device runs also check the SQLite engine and edge-to-edge rendering. Whether the Room and `RemoteMediator` tests also run on the JVM (Robolectric) is decided in T1.3.
- **AGP's built-in coverage instead of the standalone `jacoco` plugin.** `enableUnitTestCoverage` creates `createDebugUnitTestCoverageReport` with no extra plugin or task wiring. *Note:* the current 21% instruction coverage counts generated Hilt and Compose classes. It will rise as features land, and filters for generated code can be added if the number needs to reflect handwritten code only.
- **No screenshot tests yet.** They're extra E15. On AGP 9.2.1 only the standalone screenshot plugin is available, and it's deprecated. The test-suite setup needs AGP 9.5, which the installed Android Studio doesn't support yet.
- **No shared `TestData` factory yet.** It needs the domain models from T1.1, so it's created there rather than as an empty placeholder now.

## 5. How it is tested
- **`MainDispatcherRuleTest`**: a `viewModelScope.launch { delay(1000) }` hasn't finished at 999 ms of virtual time and has finished at 1001 ms, with no real waiting.
- **`AppGraphTest`** (instrumented, Pixel 4 AVD, Android 10):
  - `retrofitPointsAtTmdbApi`: the injected Retrofit uses `https://api.themoviedb.org/3/`.
  - `dispatcherQualifiersResolveToTheRightDispatchers`: `@IoDispatcher` and `@DefaultDispatcher` resolve to `Dispatchers.IO` and `Dispatchers.Default`.
- Results: `testDebugUnitTest` 42 of 42 passed, `connectedDebugAndroidTest` 2 of 2 passed, and `createDebugUnitTestCoverageReport` generated `app/build/reports/coverage/test/debug/index.html`. No compiler warnings.

## 6. How to build it from scratch
1. In `src/test/.../testing/`, write `MainDispatcherRule(testDispatcher = StandardTestDispatcher()) : TestWatcher()`, which calls `Dispatchers.setMain` in `starting` and `resetMain` in `finished`.
2. In `src/androidTest/`, write `HiltTestRunner : AndroidJUnitRunner`, overriding `newApplication` to return `HiltTestApplication`.
3. Set `defaultConfig.testInstrumentationRunner` to its fully qualified name.
4. Make sure `androidTestImplementation(hilt-android-testing)` and `kspAndroidTest(hilt-compiler)` are present.
5. Write a `@HiltAndroidTest` class with `@get:Rule val hiltRule = HiltAndroidRule(this)`, `@Inject` fields, and `hiltRule.inject()` in `@Before`.
6. Add `buildTypes.debug { enableUnitTestCoverage = true }`.
7. Run `testDebugUnitTest`, `connectedDebugAndroidTest` (with an emulator running) and `createDebugUnitTestCoverageReport`.
