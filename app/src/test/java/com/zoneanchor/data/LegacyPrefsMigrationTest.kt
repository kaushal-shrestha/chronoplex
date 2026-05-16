package com.zoneanchor.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.zoneanchor.data.migration.LegacyPrefsMigration
import com.zoneanchor.data.settings.SettingsRepository
import com.zoneanchor.model.AppPalette
import com.zoneanchor.model.AppearanceMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LegacyPrefsMigrationTest {
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var settings: SettingsRepository

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        settings = SettingsRepository(context)
        settings.setLegacyImported(false)
        prefs("zoneanchor_alarm").edit().clear().commit()
        prefs("zoneanchor_clocks").edit().clear().commit()
        prefs("zoneanchor_settings").edit().clear().commit()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun importsSharedPreferencesIntoRoomAndDataStore() = runTest {
        seedLegacyPrefs()

        LegacyPrefsMigration(context, database, settings).runIfNeeded()

        val alarm = database.alarmDao().getAll().single()
        assertThat(alarm.label).isEqualTo("Tokyo call")
        assertThat(alarm.zoneId).isEqualTo("Asia/Tokyo")
        assertThat(alarm.daysOfWeekMask).isEqualTo(31)
        assertThat(database.clockDao().getAll().map { it.zoneId }).containsExactly("Asia/Tokyo", "UTC").inOrder()
        assertThat(settings.paletteFlow.first()).isEqualTo(AppPalette.GROVE)
        assertThat(settings.appearanceFlow.first()).isEqualTo(AppearanceMode.DARK)
        assertThat(prefs("zoneanchor_alarm").all).isEmpty()
    }

    private fun seedLegacyPrefs() {
        val alarm = JSONObject()
            .put("id", 402)
            .put("label", "Tokyo call")
            .put("zone_id", "Asia/Tokyo")
            .put("hour", 9)
            .put("minute", 15)
            .put("days", 31)
            .put("sound_mode", "silent")
            .put("vibrate", false)
            .put("enabled", true)
            .put("next_trigger", 123456L)
        prefs("zoneanchor_alarm").edit()
            .putString("alarms_json", JSONArray().put(alarm).toString())
            .commit()
        val clocks = JSONArray()
            .put(JSONObject().put("zone_id", "Asia/Tokyo").put("label", "Tokyo"))
            .put("UTC")
        prefs("zoneanchor_clocks").edit().putString("zones_json", clocks.toString()).commit()
        prefs("zoneanchor_settings").edit()
            .putString("theme_id", "grove")
            .putString("appearance", "dark")
            .commit()
    }

    private fun prefs(name: String) = context.getSharedPreferences(name, Context.MODE_PRIVATE)
}
