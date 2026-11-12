package com.trianglz_task_ahmedhussin.core.di

import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertSame
import org.junit.Test

class DispatchersModuleTest {

    @Test
    fun `io qualifier provides the IO dispatcher`() {
        assertSame(Dispatchers.IO, DispatchersModule.provideIoDispatcher())
    }

    @Test
    fun `default qualifier provides the Default dispatcher`() {
        assertSame(Dispatchers.Default, DispatchersModule.provideDefaultDispatcher())
    }
}
