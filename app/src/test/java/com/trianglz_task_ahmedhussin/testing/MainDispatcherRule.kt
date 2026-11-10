package com.trianglz_task_ahmedhussin.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Replaces `Dispatchers.Main` with [testDispatcher] for the duration of a test.
 *
 * ViewModels launch in `viewModelScope`, which runs on Main; on the JVM there is no Android
 * main looper, so without this rule those coroutines would crash. Pass the same dispatcher to
 * `runTest(mainDispatcherRule.testDispatcher)` and to any injected `@IoDispatcher`, so a single
 * scheduler controls virtual time (`advanceTimeBy`, `advanceUntilIdle`) across all of them.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
