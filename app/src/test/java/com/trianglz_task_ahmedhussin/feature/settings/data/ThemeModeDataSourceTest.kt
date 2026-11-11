package com.trianglz_task_ahmedhussin.feature.settings.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.trianglz_task_ahmedhussin.feature.settings.domain.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** A real Preferences DataStore in a temporary file, so the stored format is exercised too. */
class ThemeModeDataSourceTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun TestScope.dataStore() = PreferenceDataStoreFactory.create(
        scope = backgroundScope,
        produceFile = { File(folder.root, "settings.preferences_pb") },
    )

    @Test
    fun `nothing saved means System`() = runTest {
        assertEquals(ThemeMode.System, ThemeModeDataSource(dataStore()).themeMode.first())
    }

    @Test
    fun `a saved choice is read back`() = runTest {
        val dataSource = ThemeModeDataSource(dataStore())

        dataSource.setThemeMode(ThemeMode.Dark)

        assertEquals(ThemeMode.Dark, dataSource.themeMode.first())
    }

    @Test
    fun `an unknown stored value falls back to System`() = runTest {
        val dataStore = dataStore()
        dataStore.edit { it[stringPreferencesKey("theme_mode")] = "Sepia" }

        assertEquals(ThemeMode.System, ThemeModeDataSource(dataStore).themeMode.first())
    }
}
