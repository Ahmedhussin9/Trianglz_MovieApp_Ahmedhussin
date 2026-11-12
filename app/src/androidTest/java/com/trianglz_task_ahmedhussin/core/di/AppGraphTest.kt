package com.trianglz_task_ahmedhussin.core.di

import com.trianglz_task_ahmedhussin.core.common.dispatcher.DefaultDispatcher
import com.trianglz_task_ahmedhussin.core.common.dispatcher.IoDispatcher
import com.trianglz_task_ahmedhussin.core.network.TmdbConfig
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import javax.inject.Inject

/**
 * Builds the production Hilt graph on a device and injects core bindings, proving the
 * singleton component (dispatchers, networking) resolves at runtime, not only at compile time.
 */
@HiltAndroidTest
class AppGraphTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var retrofit: Retrofit

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    @DefaultDispatcher
    lateinit var defaultDispatcher: CoroutineDispatcher

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun retrofitPointsAtTmdbApi() {
        assertEquals(TmdbConfig.API_BASE_URL, retrofit.baseUrl().toString())
    }

    @Test
    fun dispatcherQualifiersResolveToTheRightDispatchers() {
        assertSame(Dispatchers.IO, ioDispatcher)
        assertSame(Dispatchers.Default, defaultDispatcher)
    }
}
