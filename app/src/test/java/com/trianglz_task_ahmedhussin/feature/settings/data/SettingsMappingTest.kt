package com.trianglz_task_ahmedhussin.feature.settings.data

import androidx.appcompat.app.AppCompatDelegate
import com.trianglz_task_ahmedhussin.feature.settings.domain.model.AppLanguage
import com.trianglz_task_ahmedhussin.feature.settings.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsMappingTest {

    @Test
    fun `language tags map to app languages`() {
        assertEquals(AppLanguage.System, "".toAppLanguage())
        assertEquals(AppLanguage.Arabic, "ar".toAppLanguage())
        assertEquals(AppLanguage.Arabic, "ar-EG".toAppLanguage())
        assertEquals(AppLanguage.English, "en-US,ar".toAppLanguage())
        assertEquals(AppLanguage.System, "fr".toAppLanguage())
    }

    @Test
    fun `app languages map to language tags, System to none`() {
        assertNull(AppLanguage.System.toLanguageTag())
        assertEquals("en", AppLanguage.English.toLanguageTag())
        assertEquals("ar", AppLanguage.Arabic.toLanguageTag())
    }

    @Test
    fun `theme modes map to AppCompat night modes`() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, ThemeMode.System.toNightMode())
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, ThemeMode.Light.toNightMode())
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, ThemeMode.Dark.toNightMode())
    }
}
