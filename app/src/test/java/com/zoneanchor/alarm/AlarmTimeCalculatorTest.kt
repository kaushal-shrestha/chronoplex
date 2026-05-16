package com.zoneanchor.alarm

import com.google.common.truth.Truth.assertThat
import com.zoneanchor.model.ZonedAlarm
import java.time.Instant
import java.time.ZoneId
import org.junit.Test

class AlarmTimeCalculatorTest {
    @Test
    fun mondayOnlyFromSaturdayReturnsNextMonday() {
        val next = AlarmTimeCalculator.nextTriggerMillis(
            zoneId = ZoneId.of("UTC"),
            hour = 17,
            minute = 0,
            daysOfWeekMask = 1,
            now = Instant.parse("2026-05-16T12:00:00Z"),
        )

        assertThat(Instant.ofEpochMilli(next)).isEqualTo(Instant.parse("2026-05-18T17:00:00Z"))
    }

    @Test
    fun zeroDaysFallsBackToAllDays() {
        val next = AlarmTimeCalculator.nextTriggerMillis(
            zoneId = ZoneId.of("UTC"),
            hour = 16,
            minute = 1,
            daysOfWeekMask = 0,
            now = Instant.parse("2026-05-16T16:00:00Z"),
        )

        assertThat(Instant.ofEpochMilli(next)).isEqualTo(Instant.parse("2026-05-16T16:01:00Z"))
    }

    @Test
    fun springForwardSkipsNonexistentWallTime() {
        val next = AlarmTimeCalculator.nextTriggerMillis(
            zoneId = ZoneId.of("America/New_York"),
            hour = 2,
            minute = 30,
            daysOfWeekMask = ZonedAlarm.ALL_DAYS,
            now = Instant.parse("2026-03-08T06:00:00Z"),
        )

        assertThat(Instant.ofEpochMilli(next)).isEqualTo(Instant.parse("2026-03-09T06:30:00Z"))
    }

    @Test
    fun fallBackChoosesFirstOccurrence() {
        val next = AlarmTimeCalculator.nextTriggerMillis(
            zoneId = ZoneId.of("America/New_York"),
            hour = 1,
            minute = 30,
            daysOfWeekMask = ZonedAlarm.ALL_DAYS,
            now = Instant.parse("2026-11-01T05:20:00Z"),
        )

        assertThat(Instant.ofEpochMilli(next)).isEqualTo(Instant.parse("2026-11-01T05:30:00Z"))
    }

    @Test
    fun currentMinuteIsSkipped() {
        val next = AlarmTimeCalculator.nextTriggerMillis(
            zoneId = ZoneId.of("UTC"),
            hour = 17,
            minute = 0,
            daysOfWeekMask = ZonedAlarm.ALL_DAYS,
            now = Instant.parse("2026-05-16T17:00:30Z"),
        )

        assertThat(Instant.ofEpochMilli(next)).isEqualTo(Instant.parse("2026-05-17T17:00:00Z"))
    }
}
