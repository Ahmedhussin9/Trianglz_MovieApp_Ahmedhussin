package com.trianglz_task_ahmedhussin.feature.settings.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.trianglz_task_ahmedhussin.core.common.dispatcher.IoDispatcher
import com.trianglz_task_ahmedhussin.feature.settings.data.SettingsRepositoryImpl
import com.trianglz_task_ahmedhussin.feature.settings.domain.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/** Dependencies of the settings feature. */
@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {

    /** One instance per file is required by DataStore, hence the singleton. */
    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(ioDispatcher + SupervisorJob()),
        produceFile = { context.preferencesDataStoreFile(SETTINGS_FILE) },
    )

    @Provides
    fun provideSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository = impl

    private const val SETTINGS_FILE = "settings"
}
