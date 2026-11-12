package com.trianglz_task_ahmedhussin.testing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Proves the rule lets `viewModelScope` run on the JVM under controllable virtual time. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRuleTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class DelayedViewModel : ViewModel() {
        var value = 0
            private set

        fun load() {
            viewModelScope.launch {
                delay(1_000)
                value = 1
            }
        }
    }

    @Test
    fun `viewModelScope work follows the test scheduler's virtual time`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = DelayedViewModel()

        viewModel.load()
        advanceTimeBy(999)
        assertEquals("not finished before the delay elapses", 0, viewModel.value)

        advanceTimeBy(2)
        assertEquals("finished right after the delay, without real waiting", 1, viewModel.value)
    }
}
