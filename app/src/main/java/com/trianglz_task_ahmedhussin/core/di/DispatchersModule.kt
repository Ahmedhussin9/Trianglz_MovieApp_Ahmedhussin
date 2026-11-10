package com.trianglz_task_ahmedhussin.core.di

import com.trianglz_task_ahmedhussin.core.common.dispatcher.DefaultDispatcher
import com.trianglz_task_ahmedhussin.core.common.dispatcher.IoDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** Provides the app's coroutine dispatchers. This is the only place `Dispatchers.*` is referenced. */
@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
