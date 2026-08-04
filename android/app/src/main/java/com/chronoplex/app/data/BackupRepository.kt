package com.chronoplex.app.data

import com.chronoplex.app.data.db.AlarmGroupEntity
import com.chronoplex.app.data.db.AppDatabase
import com.chronoplex.app.data.db.ClockGroupEntity
import com.chronoplex.app.data.db.StopwatchGroupEntity
import com.chronoplex.app.data.db.StopwatchLapEntity
import com.chronoplex.app.data.db.TimerGroupEntity
import com.chronoplex.app.domain.Alarm
import com.chronoplex.app.domain.AlarmRepeatType
import com.chronoplex.app.domain.AppearanceMode
import com.chronoplex.app.domain.Clock
import com.chronoplex.app.domain.Group
import com.chronoplex.app.domain.Stopwatch
import com.chronoplex.app.domain.StopwatchLap
import com.chronoplex.app.domain.StopwatchState
import com.chronoplex.app.domain.ThemePalette
import com.chronoplex.app.domain.Timer
import com.chronoplex.app.domain.TimerFinishMode
import com.chronoplex.app.domain.TimerState
import com.chronoplex.app.domain.Validate
import java.time.DayOfWeek
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class BackupRepository(
    private val db: AppDatabase,
    private val clocks: ClockRepository,
    private val alarms: AlarmRepository,
    private val timers: TimerRepository,
    private val stopwatches: StopwatchRepository,
    private val settings: SettingsRepository,
) {
    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        JSONObject()
            .put("app", "chronoplex")
            .put("schemaVersion", SCHEMA_VERSION)
            .put("exportedAt", Instant.now().toString())
            .put("settings", exportSettings())
            .put("groups", exportGroups())
            .put("clocks", JSONArray(db.clockDao().getAll().map { it.toDomain().toJson() }))
            .put("alarms", JSONArray(db.alarmDao().getAll().map { it.toDomain().toJson() }))
            .put("timers", JSONArray(db.timerDao().getAll().map { it.toDomain().toJson() }))
            .put("stopwatches", JSONArray(db.stopwatchDao().getAll().map { it.toDomain().toJson() }))
            .put("laps", JSONArray(db.stopwatchDao().getAllLaps().map { it.toDomain().toJson() }))
            .toString(2)
    }

    suspend fun importJson(json: String): ImportResult = withContext(Dispatchers.IO) {
        val root = JSONObject(json)
        val version = root.optInt("schemaVersion", 1)
        require(version <= SCHEMA_VERSION) { "This backup was created by a newer Chronoplex version." }

        val settingsCount = importSettings(root.optJSONObject("settings"))
        val groupCount = importGroups(root.optJSONObject("groups"))
        val clockCount = importClocks(root.optJSONArray("clocks"))
        val alarmCount = importAlarms(root.optJSONArray("alarms"))
        val timerCount = importTimers(root.optJSONArray("timers"))
        val stopwatchCount = importStopwatches(root.optJSONArray("stopwatches"))
        val lapCount = importLaps(root.optJSONArray("laps"))

        ImportResult(clockCount, alarmCount, timerCount, stopwatchCount, lapCount, groupCount, settingsCount)
    }

    private suspend fun exportSettings() = JSONObject()
        .put("appearance", settings.appearance.first().wireName())
        .put("palette", settings.palette.first().wireName())
        .put("defaultZoneSource", settings.alarmZoneSource.first().wireName())
        .put("alarmZoneDisplay", settings.alarmZoneDisplay.first().wireName())
        .put("firstDayOfWeek", settings.firstDayOfWeek.first().name.lowercase(Locale.US))
        .put("defaultTimerFinishMode", settings.timerFinishMode.first().wireName())
        .put("groupedClocks", settings.clocksGroupingEnabled.first())
        .put("groupedAlarms", settings.alarmsGroupingEnabled.first())
        .put("groupedTimers", settings.timersGroupingEnabled.first())
        .put("groupedStopwatches", settings.stopwatchesGroupingEnabled.first())

    private suspend fun importSettings(json: JSONObject?): Int {
        if (json == null) return 0
        var count = 0
        json.optStringOrNull("appearance")?.let { appearanceFromWire(it)?.let { v -> settings.setAppearance(v); count++ } }
        json.optStringOrNull("palette")?.let { paletteFromWire(it)?.let { v -> settings.setPalette(v); count++ } }
        json.optStringOrNull("defaultZoneSource")?.let { zoneSourceFromWire(it)?.let { v -> settings.setAlarmZoneSource(v); count++ } }
        json.optStringOrNull("alarmZoneDisplay")?.let { zoneDisplayFromWire(it)?.let { v -> settings.setAlarmZoneDisplay(v); count++ } }
        json.optStringOrNull("firstDayOfWeek")?.let { dayFromWire(it)?.let { v -> settings.setFirstDayOfWeek(v); count++ } }
        json.optStringOrNull("defaultTimerFinishMode")?.let { timerFinishFromWire(it)?.let { v -> settings.setTimerFinishMode(v); count++ } }
        if (json.has("groupedClocks")) { settings.setClocksGroupingEnabled(json.optBoolean("groupedClocks")); count++ }
        if (json.has("groupedAlarms")) { settings.setAlarmsGroupingEnabled(json.optBoolean("groupedAlarms")); count++ }
        if (json.has("groupedTimers")) { settings.setTimersGroupingEnabled(json.optBoolean("groupedTimers")); count++ }
        if (json.has("groupedStopwatches")) { settings.setStopwatchesGroupingEnabled(json.optBoolean("groupedStopwatches")); count++ }
        return count
    }

    private suspend fun exportGroups() = JSONObject()
        .put("clocks", JSONArray(db.clockDao().getAllGroups().map { it.toDomain().toJson() }))
        .put("alarms", JSONArray(db.alarmDao().getAllGroups().map { it.toDomain().toJson() }))
        .put("timers", JSONArray(db.timerDao().getAllGroups().map { it.toDomain().toJson() }))
        .put("stopwatches", JSONArray(db.stopwatchDao().getAllGroups().map { it.toDomain().toJson() }))

    private suspend fun importGroups(json: JSONObject?): Int {
        if (json == null) return 0
        var count = 0
        json.optJSONArray("clocks").forEachObject { db.clockDao().upsertGroup(ClockGroupEntity.fromDomain(it.toGroup())); count++ }
        json.optJSONArray("alarms").forEachObject { db.alarmDao().upsertGroup(AlarmGroupEntity.fromDomain(it.toGroup())); count++ }
        json.optJSONArray("timers").forEachObject { db.timerDao().upsertGroup(TimerGroupEntity.fromDomain(it.toGroup())); count++ }
        json.optJSONArray("stopwatches").forEachObject { db.stopwatchDao().upsertGroup(StopwatchGroupEntity.fromDomain(it.toGroup())); count++ }
        return count
    }

    private suspend fun importClocks(array: JSONArray?): Int {
        var count = 0
        array.forEachObject { clocks.upsert(it.toClock()); count++ }
        return count
    }

    private suspend fun importAlarms(array: JSONArray?): Int {
        var count = 0
        array.forEachObject { alarms.upsert(it.toAlarm()); count++ }
        return count
    }

    private suspend fun importTimers(array: JSONArray?): Int {
        var count = 0
        array.forEachObject { timers.upsert(it.toTimer()); count++ }
        return count
    }

    private suspend fun importStopwatches(array: JSONArray?): Int {
        var count = 0
        array.forEachObject { stopwatches.upsert(it.toStopwatch()); count++ }
        return count
    }

    private suspend fun importLaps(array: JSONArray?): Int {
        var count = 0
        array.forEachObject {
            db.stopwatchDao().insertLap(StopwatchLapEntity.fromDomain(it.toLap()))
            count++
        }
        return count
    }

    data class ImportResult(
        val clocks: Int,
        val alarms: Int,
        val timers: Int,
        val stopwatches: Int,
        val laps: Int,
        val groups: Int,
        val settings: Int,
    )

    companion object {
        private const val SCHEMA_VERSION = 1
    }
}

private fun Group.toJson() = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("sortOrder", sortOrder)
    .put("collapsed", collapsed)

private fun Clock.toJson() = JSONObject()
    .put("id", id)
    .put("label", label)
    .put("zoneId", zoneId)
    .put("sortOrder", sortOrder)
    .put("groupId", groupId)

private fun Alarm.toJson() = JSONObject()
    .put("id", id)
    .put("label", label)
    .put("zoneId", zoneId)
    .put("hour", hour)
    .put("minute", minute)
    .put("daysMask", daysMask)
    .put("soundEnabled", soundEnabled)
    .put("vibrationEnabled", vibrationEnabled)
    .put("enabled", enabled)
    .put("snoozeUntilMillis", snoozeUntilMillis)
    .put("groupId", groupId)
    .put("clockId", clockId)
    .put("repeatType", effectiveRepeatType.wireName())
    .put("repeatInterval", repeatInterval)
    .put("repeatStartDate", repeatStartDate)
    .put("monthlyDay", monthlyDay)
    .put("monthlyOrdinal", monthlyOrdinal)
    .put("monthlyWeekday", monthlyWeekday)

private fun Timer.toJson() = JSONObject()
    .put("id", id)
    .put("label", label)
    .put("durationMillis", durationMillis)
    .put("state", state.wireName())
    .put("endsAtMillis", endsAtMillis)
    .put("pausedRemainingMillis", pausedRemainingMillis)
    .put("finishMode", finishMode.wireName())
    .put("sortOrder", sortOrder)
    .put("groupId", groupId)

private fun Stopwatch.toJson() = JSONObject()
    .put("id", id)
    .put("label", label)
    .put("state", state.wireName())
    .put("startedAtMillis", startedAtMillis)
    .put("accumulatedMillis", accumulatedMillis)
    .put("sortOrder", sortOrder)
    .put("groupId", groupId)

private fun StopwatchLap.toJson() = JSONObject()
    .put("id", stopwatchId * 100_000L + lapNumber)
    .put("stopwatchId", stopwatchId)
    .put("lapNumber", lapNumber)
    .put("totalElapsedMillis", totalElapsedMillis)

private fun JSONObject.toGroup() = Group(
    id = optLong("id", 0L),
    name = Validate.label(optString("name", "Group")).ifBlank { "Group" },
    sortOrder = optLong("sortOrder", System.currentTimeMillis()),
    collapsed = optBoolean("collapsed", false),
)

private fun JSONObject.toClock() = Clock(
    id = optLong("id", 0L),
    label = Validate.label(optString("label", "")),
    zoneId = Validate.zoneId(optStringOrNull("zoneId")),
    sortOrder = optLong("sortOrder", System.currentTimeMillis()),
    groupId = optLongOrNull("groupId"),
)

private fun JSONObject.toAlarm() = Alarm(
    id = optLong("id", 0L),
    label = Validate.label(optString("label", "")),
    zoneId = Validate.zoneId(optStringOrNull("zoneId")),
    hour = Validate.hour(optInt("hour", 7)),
    minute = Validate.minute(optInt("minute", 0)),
    daysMask = optInt("daysMask", 0) and 0b1111111,
    soundEnabled = optBoolean("soundEnabled", true),
    vibrationEnabled = optBoolean("vibrationEnabled", true),
    enabled = optBoolean("enabled", true),
    snoozeUntilMillis = optLongOrNull("snoozeUntilMillis"),
    groupId = optLongOrNull("groupId"),
    clockId = optLongOrNull("clockId"),
    repeatType = alarmRepeatFromWire(optString("repeatType", "weekly")) ?: AlarmRepeatType.WEEKLY,
    repeatInterval = Validate.repeatInterval(optInt("repeatInterval", 1)),
    repeatStartDate = optString("repeatStartDate", ""),
    monthlyDay = Validate.monthlyDay(optInt("monthlyDay", 1)),
    monthlyOrdinal = Validate.monthlyOrdinal(optInt("monthlyOrdinal", 1)),
    monthlyWeekday = Validate.monthlyWeekday(optInt("monthlyWeekday", 1)),
)

private fun JSONObject.toTimer() = Timer(
    id = optLong("id", 0L),
    label = Validate.label(optString("label", "")),
    durationMillis = optLong("durationMillis", 0L).coerceAtLeast(0L),
    state = timerStateFromWire(optString("state", "idle")) ?: TimerState.IDLE,
    endsAtMillis = optLongOrNull("endsAtMillis"),
    pausedRemainingMillis = optLongOrNull("pausedRemainingMillis"),
    finishMode = timerFinishFromWire(optString("finishMode", "notification")) ?: TimerFinishMode.NOTIFICATION,
    sortOrder = optLong("sortOrder", System.currentTimeMillis()),
    groupId = optLongOrNull("groupId"),
)

private fun JSONObject.toStopwatch() = Stopwatch(
    id = optLong("id", 0L),
    label = Validate.label(optString("label", "")),
    state = stopwatchStateFromWire(optString("state", "idle")) ?: StopwatchState.IDLE,
    startedAtMillis = optLongOrNull("startedAtMillis"),
    accumulatedMillis = optLong("accumulatedMillis", 0L).coerceAtLeast(0L),
    sortOrder = optLong("sortOrder", System.currentTimeMillis()),
    groupId = optLongOrNull("groupId"),
)

private fun JSONObject.toLap() = StopwatchLap(
    stopwatchId = optLong("stopwatchId", 0L),
    lapNumber = optInt("lapNumber", 0).coerceAtLeast(0),
    totalElapsedMillis = optLong("totalElapsedMillis", 0L).coerceAtLeast(0L),
)

private suspend fun JSONArray?.forEachObject(block: suspend (JSONObject) -> Unit) {
    if (this == null) return
    for (index in 0 until length()) optJSONObject(index)?.let { block(it) }
}

private fun JSONObject.optLongOrNull(key: String): Long? =
    if (has(key) && !isNull(key)) optLong(key) else null

private fun JSONObject.optStringOrNull(key: String): String? =
    if (has(key) && !isNull(key)) optString(key).ifBlank { null } else null

private fun AppearanceMode.wireName() = name.lowercase(Locale.US)
private fun ThemePalette.wireName() = name.replaceFirstChar { it.lowercase(Locale.US) }
private fun AlarmZoneSource.wireName() = if (this == AlarmZoneSource.ADDED_CLOCKS) "myClocks" else "allZones"
private fun AlarmZoneDisplayMode.wireName() = if (this == AlarmZoneDisplayMode.CLOCK_LABEL) "clockLabel" else "zoneId"
private fun TimerFinishMode.wireName() = if (this == TimerFinishMode.FULL_SCREEN) "fullScreen" else "notification"
private fun AlarmRepeatType.wireName() = when (this) {
    AlarmRepeatType.ONCE -> "once"
    AlarmRepeatType.WEEKLY -> "weekly"
    AlarmRepeatType.MONTHLY_DAY -> "monthlyDay"
    AlarmRepeatType.MONTHLY_WEEKDAY -> "monthlyWeekday"
}
private fun TimerState.wireName() = name.lowercase(Locale.US)
private fun StopwatchState.wireName() = name.lowercase(Locale.US)

private fun appearanceFromWire(value: String) = AppearanceMode.values().firstOrNull { it.wireName() == value || it.name == value }
private fun paletteFromWire(value: String) = ThemePalette.values().firstOrNull { it.wireName() == value || it.name == value }
private fun zoneSourceFromWire(value: String) = when (value) {
    "myClocks", "ADDED_CLOCKS" -> AlarmZoneSource.ADDED_CLOCKS
    "allZones", "ALL_ZONES" -> AlarmZoneSource.ALL_ZONES
    else -> null
}
private fun zoneDisplayFromWire(value: String) = when (value) {
    "clockLabel", "CLOCK_LABEL" -> AlarmZoneDisplayMode.CLOCK_LABEL
    "zoneId", "ZONE_ID" -> AlarmZoneDisplayMode.ZONE_ID
    else -> null
}
private fun dayFromWire(value: String) = DayOfWeek.values().firstOrNull { it.name.equals(value, ignoreCase = true) }
private fun timerFinishFromWire(value: String) = when (value) {
    "notification", "NOTIFICATION" -> TimerFinishMode.NOTIFICATION
    "fullScreen", "FULL_SCREEN" -> TimerFinishMode.FULL_SCREEN
    else -> null
}
private fun alarmRepeatFromWire(value: String) = AlarmRepeatType.values().firstOrNull { it.wireName() == value || it.name == value }
private fun timerStateFromWire(value: String) = TimerState.values().firstOrNull { it.wireName() == value || it.name == value }
private fun stopwatchStateFromWire(value: String) = StopwatchState.values().firstOrNull { it.wireName() == value || it.name == value }
