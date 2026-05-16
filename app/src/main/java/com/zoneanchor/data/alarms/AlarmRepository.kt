package com.zoneanchor.data.alarms

import com.zoneanchor.model.ZonedAlarm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlarmRepository(private val dao: AlarmDao) {
    val alarms: Flow<List<ZonedAlarm>> = dao.observeAll().map { rows -> rows.map { it.toModel() } }

    suspend fun allOnce(): List<ZonedAlarm> = dao.getAll().map { it.toModel() }

    suspend fun enabledOnce(): List<ZonedAlarm> = dao.getEnabled().map { it.toModel() }

    suspend fun find(id: Int): ZonedAlarm? = dao.findById(id)?.toModel()

    suspend fun nextId(): Int {
        val next = (dao.maxId() ?: (ZonedAlarm.FIRST_ID - 1)) + 1
        return next.coerceAtLeast(ZonedAlarm.FIRST_ID)
    }

    suspend fun upsert(alarm: ZonedAlarm) = dao.upsert(alarm.toEntity())

    suspend fun upsertAll(alarms: List<ZonedAlarm>) = dao.upsertAll(alarms.map { it.toEntity() })

    suspend fun delete(id: Int) = dao.deleteById(id)

    private fun AlarmEntity.toModel() = ZonedAlarm(
        id = id.coerceAtLeast(ZonedAlarm.FIRST_ID),
        label = ZonedAlarm.cleanLabel(label),
        zoneId = ZonedAlarm.validZoneId(zoneId),
        hour = hour.coerceIn(0, 23),
        minute = minute.coerceIn(0, 59),
        daysOfWeekMask = ZonedAlarm.normalizeDays(daysOfWeekMask),
        soundMode = ZonedAlarm.validSoundMode(soundMode),
        vibrate = vibrate,
        enabled = enabled,
        nextTriggerAtMillis = nextTriggerAtMillis,
    )

    private fun ZonedAlarm.toEntity() = AlarmEntity(
        id = id.coerceAtLeast(ZonedAlarm.FIRST_ID),
        label = ZonedAlarm.cleanLabel(label),
        zoneId = ZonedAlarm.validZoneId(zoneId),
        hour = hour.coerceIn(0, 23),
        minute = minute.coerceIn(0, 59),
        daysOfWeekMask = ZonedAlarm.normalizeDays(daysOfWeekMask),
        soundMode = ZonedAlarm.validSoundMode(soundMode),
        vibrate = vibrate,
        enabled = enabled,
        nextTriggerAtMillis = nextTriggerAtMillis,
    )
}
