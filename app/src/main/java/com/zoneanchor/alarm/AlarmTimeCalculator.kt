package com.zoneanchor.alarm

import com.zoneanchor.model.ZonedAlarm
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

object AlarmTimeCalculator {
    fun nextTriggerMillis(
        zoneId: ZoneId,
        hour: Int,
        minute: Int,
        daysOfWeekMask: Int = ZonedAlarm.ALL_DAYS,
        now: Instant = Instant.now(),
    ): Long {
        val zoneNow = now.atZone(zoneId)
        val selectedDays = ZonedAlarm.normalizeDays(daysOfWeekMask)
        for (dayOffset in 0..7) {
            val local = zoneNow.toLocalDate().plusDays(dayOffset.toLong()).atTime(hour, minute)
            val candidate = candidateOrNull(local, zoneId) ?: continue
            if (isSelected(candidate.dayOfWeek, selectedDays) && candidate.toInstant().isAfter(now)) {
                return candidate.toInstant().toEpochMilli()
            }
        }
        return zoneNow.plusDays(1).toInstant().toEpochMilli()
    }

    private fun candidateOrNull(local: LocalDateTime, zoneId: ZoneId): ZonedDateTime? {
        val offsets = zoneId.rules.getValidOffsets(local)
        if (offsets.isEmpty()) return null
        val candidate = ZonedDateTime.ofLocal(local, zoneId, offsets.first())
        // Spring-forward gaps can shift wall time; locked alarms skip nonexistent local times.
        return if (candidate.hour == local.hour && candidate.minute == local.minute) candidate else null
    }

    private fun isSelected(day: DayOfWeek, daysOfWeekMask: Int): Boolean {
        return (daysOfWeekMask and (1 shl (day.value - 1))) != 0
    }
}
