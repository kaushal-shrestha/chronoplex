package com.zoneanchor.data.migration

import android.content.Context
import android.content.SharedPreferences
import com.zoneanchor.data.AppDatabase
import com.zoneanchor.data.alarms.AlarmEntity
import com.zoneanchor.data.settings.SettingsRepository
import com.zoneanchor.model.AppPalette
import com.zoneanchor.model.AppearanceMode
import com.zoneanchor.model.ClockEntry
import com.zoneanchor.model.ZonedAlarm
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class LegacyPrefsMigration(
    private val context: Context,
    private val database: AppDatabase,
    private val settings: SettingsRepository,
) {
    suspend fun runIfNeeded() {
        if (settings.legacyImportedFlow.first()) return
        withContext(Dispatchers.IO) {
            val alarmPrefs = prefs(ALARM_PREFS)
            val clockPrefs = prefs(CLOCK_PREFS)
            val settingsPrefs = prefs(SETTINGS_PREFS)

            if (database.alarmDao().count() == 0) {
                val alarms = parseAlarms(alarmPrefs)
                if (alarms.isNotEmpty()) database.alarmDao().upsertAll(alarms)
            }
            if (database.clockDao().count() == 0) {
                val clocks = parseClocks(clockPrefs)
                if (clocks.isNotEmpty()) {
                    database.clockDao().upsertAll(clocks.mapIndexed { i, c -> c.toEntity(i) })
                }
            }
            migrateSettings(settingsPrefs)

            alarmPrefs.edit().clear().apply()
            clockPrefs.edit().clear().apply()
            settingsPrefs.edit().clear().apply()
        }
        settings.setLegacyImported(true)
    }

    private suspend fun migrateSettings(prefs: SharedPreferences) {
        if (prefs.contains(KEY_THEME_ID)) {
            settings.setPalette(AppPalette.fromLegacyId(prefs.getString(KEY_THEME_ID, null)))
        }
        if (prefs.contains(KEY_APPEARANCE)) {
            settings.setAppearance(AppearanceMode.fromLegacyId(prefs.getString(KEY_APPEARANCE, null)))
        }
    }

    private fun parseAlarms(prefs: SharedPreferences): List<AlarmEntity> {
        val imported = mutableListOf<AlarmEntity>()
        val json = prefs.getString(KEY_ALARMS_JSON, null)
        if (json != null) {
            runCatching {
                val array = JSONArray(json)
                for (index in 0 until array.length()) {
                    array.optJSONObject(index)?.let { parseAlarm(it)?.let(imported::add) }
                }
            }
        }
        parseLegacyLooseAlarm(prefs)?.let(imported::add)
        return imported.distinctBy { it.id }
    }

    private fun parseAlarm(json: JSONObject): AlarmEntity? {
        val zoneId = validZoneId(json.optString(KEY_ZONE_ID, ZonedAlarm.DEFAULT_ZONE_ID))
        val id = json.optInt(KEY_ID, ZonedAlarm.DEFAULT_ID).coerceAtLeast(ZonedAlarm.FIRST_ID)
        return AlarmEntity(
            id = id,
            label = ZonedAlarm.cleanLabel(json.optString(KEY_LABEL, "")),
            zoneId = zoneId,
            hour = json.optInt(KEY_HOUR, ZonedAlarm.DEFAULT_HOUR).coerceIn(0, 23),
            minute = json.optInt(KEY_MINUTE, ZonedAlarm.DEFAULT_MINUTE).coerceIn(0, 59),
            daysOfWeekMask = ZonedAlarm.normalizeDays(json.optInt(KEY_DAYS, ZonedAlarm.ALL_DAYS)),
            soundMode = ZonedAlarm.validSoundMode(json.optString(KEY_SOUND_MODE)),
            vibrate = json.optBoolean(KEY_VIBRATE, true),
            enabled = json.optBoolean(KEY_ENABLED, false),
            nextTriggerAtMillis = json.optLong(KEY_NEXT_TRIGGER, 0L),
        )
    }

    private fun parseLegacyLooseAlarm(prefs: SharedPreferences): AlarmEntity? {
        if (!prefs.contains(KEY_ZONE_ID)) return null
        return AlarmEntity(
            id = ZonedAlarm.DEFAULT_ID,
            label = "",
            zoneId = validZoneId(prefs.getString(KEY_ZONE_ID, ZonedAlarm.DEFAULT_ZONE_ID)),
            hour = prefs.getInt(KEY_HOUR, ZonedAlarm.DEFAULT_HOUR).coerceIn(0, 23),
            minute = prefs.getInt(KEY_MINUTE, ZonedAlarm.DEFAULT_MINUTE).coerceIn(0, 59),
            daysOfWeekMask = ZonedAlarm.ALL_DAYS,
            soundMode = ZonedAlarm.SOUND_DEFAULT,
            vibrate = true,
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            nextTriggerAtMillis = prefs.getLong(KEY_NEXT_TRIGGER, 0L),
        )
    }

    private fun parseClocks(prefs: SharedPreferences): List<ClockEntry> {
        val json = prefs.getString(KEY_ZONES_JSON, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    parseClock(array.opt(index))?.let(::add)
                }
            }.distinctBy { it.zoneId }
        }.getOrDefault(emptyList())
    }

    private fun parseClock(value: Any?): ClockEntry? {
        val zoneId: String
        val label: String
        if (value is JSONObject) {
            zoneId = value.optString(KEY_ZONE_ID, "")
            label = value.optString(KEY_LABEL, "")
        } else if (value is String) {
            zoneId = value
            label = ""
        } else {
            return null
        }
        return if (isKnownZone(zoneId)) ClockEntry(zoneId, label.trim()) else null
    }

    private fun ClockEntry.toEntity(position: Int) =
        com.zoneanchor.data.clocks.ClockEntity(zoneId, label.trim(), position)

    private fun prefs(name: String) =
        context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)

    private fun validZoneId(value: String?) = if (isKnownZone(value)) value!! else ZonedAlarm.DEFAULT_ZONE_ID

    private fun isKnownZone(value: String?) = runCatching { ZoneId.of(value ?: "") }.isSuccess

    companion object {
        private const val ALARM_PREFS = "zoneanchor_alarm"
        private const val CLOCK_PREFS = "zoneanchor_clocks"
        private const val SETTINGS_PREFS = "zoneanchor_settings"
        private const val KEY_ALARMS_JSON = "alarms_json"
        private const val KEY_ZONES_JSON = "zones_json"
        private const val KEY_THEME_ID = "theme_id"
        private const val KEY_APPEARANCE = "appearance"
        private const val KEY_ID = "id"
        private const val KEY_LABEL = "label"
        private const val KEY_ZONE_ID = "zone_id"
        private const val KEY_HOUR = "hour"
        private const val KEY_MINUTE = "minute"
        private const val KEY_DAYS = "days"
        private const val KEY_SOUND_MODE = "sound_mode"
        private const val KEY_VIBRATE = "vibrate"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_NEXT_TRIGGER = "next_trigger"
    }
}
