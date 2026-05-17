package com.chronoplex.app.alarm

import com.google.common.truth.Truth.assertThat
import com.chronoplex.app.domain.Alarm
import com.chronoplex.app.domain.DayMask
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Exercises the [AlarmScheduler.nextTriggerMillis] companion. The function is pure
 * (zone math only — no Android dependencies), so this is a fast plain-JUnit test.
 */
class AlarmSchedulerNextTriggerTest {

    private val nyc = ZoneId.of("America/New_York")

    private fun nowAtNyc(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, nyc).toInstant().toEpochMilli()

    @Test fun `monday-only alarm from saturday afternoon picks the next monday`() {
        // Saturday 17 Jan 2026, 12:00 NYC
        val now = nowAtNyc(2026, 1, 17, 12, 0)
        val alarm = alarm(hour = 17, minute = 0, days = setOf(DayOfWeek.MONDAY))

        val next = AlarmScheduler.nextTriggerMillis(alarm, now)

        val expected = ZonedDateTime.of(2026, 1, 19, 17, 0, 0, 0, nyc).toInstant().toEpochMilli()
        assertThat(next).isEqualTo(expected)
        assertThat(ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(next!!), nyc).dayOfWeek)
            .isEqualTo(DayOfWeek.MONDAY)
    }

    @Test fun `daysMask zero is treated as one-shot, returning today if still in the future`() {
        val now = nowAtNyc(2026, 1, 17, 7, 0) // Saturday 07:00
        val alarm = alarm(hour = 8, minute = 0, days = emptySet()) // mask 0 → one-shot

        val next = AlarmScheduler.nextTriggerMillis(alarm, now)

        val expected = ZonedDateTime.of(2026, 1, 17, 8, 0, 0, 0, nyc).toInstant().toEpochMilli()
        assertThat(next).isEqualTo(expected)
    }

    @Test fun `daysMask zero one-shot rolls to tomorrow if time has passed today`() {
        val now = nowAtNyc(2026, 1, 17, 9, 0)
        val alarm = alarm(hour = 8, minute = 0, days = emptySet())

        val next = AlarmScheduler.nextTriggerMillis(alarm, now)

        val expected = ZonedDateTime.of(2026, 1, 18, 8, 0, 0, 0, nyc).toInstant().toEpochMilli()
        assertThat(next).isEqualTo(expected)
    }

    @Test fun `spring-forward sunday 02-30 in NYC is skipped to the next valid day`() {
        // DST 2026 in the US starts Sun 8 March at 02:00 — clocks jump straight to 03:00.
        // 02:30 doesn't exist that day. An alarm at 02:30, evaluated from 01:00 EST,
        // should fire NEXT day at 02:30 EDT (Mon 9 March), not at a shifted instant.
        val now = nowAtNyc(2026, 3, 8, 1, 0)
        val alarm = alarm(hour = 2, minute = 30, days = DayOfWeek.values().toSet())

        val next = AlarmScheduler.nextTriggerMillis(alarm, now)

        val expected = ZonedDateTime.of(2026, 3, 9, 2, 30, 0, 0, nyc).toInstant().toEpochMilli()
        assertThat(next).isEqualTo(expected)
    }

    @Test fun `fall-back sunday 01-30 in NYC fires at the first (EDT) occurrence`() {
        // DST 2026 in the US ends Sun 1 Nov at 02:00 — clocks fall back to 01:00.
        // 01:30 happens twice that day: once at EDT (UTC-4), then again at EST (UTC-5).
        // Evaluated just before 01:30, we want the first (EDT) occurrence.
        // 00:30 EDT == 04:30 UTC on Sun 1 Nov 2026.
        val sundayPreDst = ZonedDateTime.of(2026, 11, 1, 0, 30, 0, 0, nyc)
            .toInstant().toEpochMilli()
        val alarm = alarm(hour = 1, minute = 30, days = DayOfWeek.values().toSet())

        val next = AlarmScheduler.nextTriggerMillis(alarm, sundayPreDst)

        // The first 01:30 on Nov 1 is still EDT (UTC-4) = 05:30Z.
        val firstEdtInstant = java.time.OffsetDateTime
            .of(2026, 11, 1, 1, 30, 0, 0, java.time.ZoneOffset.ofHours(-4))
            .toInstant().toEpochMilli()
        assertThat(next).isEqualTo(firstEdtInstant)
    }

    @Test fun `alarm with current minute is skipped to the next allowed day`() {
        // If alarm time equals "now" to the second, isAfter() returns false → skip.
        val now = nowAtNyc(2026, 1, 17, 9, 0)
        val alarm = alarm(hour = 9, minute = 0, days = DayOfWeek.values().toSet())

        val next = AlarmScheduler.nextTriggerMillis(alarm, now)

        val tomorrow = ZonedDateTime.of(2026, 1, 18, 9, 0, 0, 0, nyc).toInstant().toEpochMilli()
        assertThat(next).isEqualTo(tomorrow)
    }

    @Test fun `invalid zone id falls back to system default and still returns a future time`() {
        val now = System.currentTimeMillis()
        val alarm = alarm(hour = 12, minute = 0, days = DayOfWeek.values().toSet())
            .copy(zoneId = "Mars/Olympus_Mons")

        val next = AlarmScheduler.nextTriggerMillis(alarm, now)

        assertThat(next).isNotNull()
        assertThat(next!!).isGreaterThan(now)
    }

    private fun alarm(
        hour: Int,
        minute: Int,
        days: Set<DayOfWeek>,
        zoneId: String = "America/New_York",
    ) = Alarm(
        id = 1,
        label = "test",
        zoneId = zoneId,
        hour = hour,
        minute = minute,
        daysMask = DayMask.toMask(days),
        soundEnabled = true,
        vibrationEnabled = true,
        enabled = true,
    )
}
