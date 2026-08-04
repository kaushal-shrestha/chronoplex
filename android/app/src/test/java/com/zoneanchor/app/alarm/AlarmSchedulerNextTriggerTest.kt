package com.zoneanchor.app.alarm

import com.google.common.truth.Truth.assertThat
import com.zoneanchor.app.domain.Alarm
import com.zoneanchor.app.domain.AlarmRepeatType
import com.zoneanchor.app.domain.DayMask
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

    @Test fun `every other sunday uses the configured start week as its anchor`() {
        val now = nowAtNyc(2026, 8, 10, 9, 0)
        val alarm = alarm(hour = 8, minute = 0, days = setOf(DayOfWeek.SUNDAY))
            .copy(repeatInterval = 2, repeatStartDate = "2026-08-03")

        val next = AlarmScheduler.nextTriggerMillis(alarm, now)

        val expected = ZonedDateTime.of(2026, 8, 23, 8, 0, 0, 0, nyc).toInstant().toEpochMilli()
        assertThat(next).isEqualTo(expected)
    }

    @Test fun `monthly day repeat honors a six month interval from the anchor month`() {
        val now = nowAtNyc(2026, 9, 1, 9, 0)
        val alarm = alarm(hour = 9, minute = 30, days = DayOfWeek.values().toSet())
            .copy(
                repeatType = AlarmRepeatType.MONTHLY_DAY,
                repeatInterval = 6,
                repeatStartDate = "2026-08-03",
                monthlyDay = 3,
            )

        val next = AlarmScheduler.nextTriggerMillis(alarm, now)

        val expected = ZonedDateTime.of(2027, 2, 3, 9, 30, 0, 0, nyc).toInstant().toEpochMilli()
        assertThat(next).isEqualTo(expected)
    }

    @Test fun `first sunday monthly skips an anchor month whose first sunday is before the start date`() {
        val now = nowAtNyc(2026, 8, 4, 9, 0)
        val alarm = alarm(hour = 10, minute = 0, days = DayOfWeek.values().toSet())
            .copy(
                repeatType = AlarmRepeatType.MONTHLY_WEEKDAY,
                repeatStartDate = "2026-08-03",
                monthlyOrdinal = 1,
                monthlyWeekday = DayOfWeek.SUNDAY.value,
            )

        val next = AlarmScheduler.nextTriggerMillis(alarm, now)

        val expected = ZonedDateTime.of(2026, 9, 6, 10, 0, 0, 0, nyc).toInstant().toEpochMilli()
        assertThat(next).isEqualTo(expected)
    }

    @Test fun `monthly day repeat skips months without that date`() {
        val now = nowAtNyc(2026, 1, 31, 9, 0)
        val alarm = alarm(hour = 8, minute = 0, days = DayOfWeek.values().toSet())
            .copy(
                repeatType = AlarmRepeatType.MONTHLY_DAY,
                repeatStartDate = "2026-01-01",
                monthlyDay = 31,
            )

        val next = AlarmScheduler.nextTriggerMillis(alarm, now)

        val expected = ZonedDateTime.of(2026, 3, 31, 8, 0, 0, 0, nyc).toInstant().toEpochMilli()
        assertThat(next).isEqualTo(expected)
    }

    @Test fun `weekly alarm with only garbage high bits has no valid trigger`() {
        val now = nowAtNyc(2026, 1, 17, 9, 0)
        val alarm = alarm(hour = 8, minute = 0, days = emptySet())
            .copy(daysMask = 1 shl 12, repeatType = AlarmRepeatType.WEEKLY)

        val next = AlarmScheduler.nextTriggerMillis(alarm, now)

        assertThat(next).isNull()
    }

    @Test fun `weekly repeat waits for a future start date before using selected days`() {
        val now = nowAtNyc(2026, 8, 1, 9, 0)
        val alarm = alarm(hour = 11, minute = 15, days = setOf(DayOfWeek.FRIDAY))
            .copy(repeatStartDate = "2026-08-12")

        val next = AlarmScheduler.nextTriggerMillis(alarm, now)

        val expected = ZonedDateTime.of(2026, 8, 14, 11, 15, 0, 0, nyc).toInstant().toEpochMilli()
        assertThat(next).isEqualTo(expected)
    }

    @Test fun `weekly repeat uses today as anchor when start date is invalid`() {
        val now = nowAtNyc(2026, 8, 3, 9, 0)
        val alarm = alarm(hour = 8, minute = 0, days = setOf(DayOfWeek.SUNDAY))
            .copy(repeatInterval = 2, repeatStartDate = "not-a-date")

        val next = AlarmScheduler.nextTriggerMillis(alarm, now)

        val expected = ZonedDateTime.of(2026, 8, 9, 8, 0, 0, 0, nyc).toInstant().toEpochMilli()
        assertThat(next).isEqualTo(expected)
    }

    @Test fun `monthly day repeat advances when this month has already passed`() {
        val now = nowAtNyc(2026, 1, 15, 10, 0)
        val alarm = alarm(hour = 9, minute = 0, days = DayOfWeek.values().toSet())
            .copy(
                repeatType = AlarmRepeatType.MONTHLY_DAY,
                repeatStartDate = "2026-01-01",
                monthlyDay = 15,
            )

        val next = AlarmScheduler.nextTriggerMillis(alarm, now)

        val expected = ZonedDateTime.of(2026, 2, 15, 9, 0, 0, 0, nyc).toInstant().toEpochMilli()
        assertThat(next).isEqualTo(expected)
    }

    @Test fun `monthly day repeat clamps impossible saved days before scheduling`() {
        val now = nowAtNyc(2026, 1, 1, 9, 0)
        val alarm = alarm(hour = 7, minute = 45, days = DayOfWeek.values().toSet())
            .copy(
                repeatType = AlarmRepeatType.MONTHLY_DAY,
                repeatStartDate = "2026-01-01",
                monthlyDay = 80,
            )

        val next = AlarmScheduler.nextTriggerMillis(alarm, now)

        val expected = ZonedDateTime.of(2026, 1, 31, 7, 45, 0, 0, nyc).toInstant().toEpochMilli()
        assertThat(next).isEqualTo(expected)
    }

    @Test fun `monthly weekday repeat supports the last selected weekday`() {
        val now = nowAtNyc(2026, 7, 1, 9, 0)
        val alarm = alarm(hour = 6, minute = 30, days = DayOfWeek.values().toSet())
            .copy(
                repeatType = AlarmRepeatType.MONTHLY_WEEKDAY,
                repeatStartDate = "2026-07-01",
                monthlyOrdinal = -1,
                monthlyWeekday = DayOfWeek.FRIDAY.value,
            )

        val next = AlarmScheduler.nextTriggerMillis(alarm, now)

        val expected = ZonedDateTime.of(2026, 7, 31, 6, 30, 0, 0, nyc).toInstant().toEpochMilli()
        assertThat(next).isEqualTo(expected)
    }

    @Test fun `monthly weekday repeat clamps corrupt ordinal and weekday values`() {
        val now = nowAtNyc(2026, 7, 1, 9, 0)
        val alarm = alarm(hour = 6, minute = 30, days = DayOfWeek.values().toSet())
            .copy(
                repeatType = AlarmRepeatType.MONTHLY_WEEKDAY,
                repeatStartDate = "2026-07-01",
                monthlyOrdinal = 99,
                monthlyWeekday = 99,
            )

        val next = AlarmScheduler.nextTriggerMillis(alarm, now)

        val expected = ZonedDateTime.of(2026, 7, 26, 6, 30, 0, 0, nyc).toInstant().toEpochMilli()
        assertThat(next).isEqualTo(expected)
    }

    @Test fun `spring-forward gap on the only selected day waits for the next selected week`() {
        val now = nowAtNyc(2026, 3, 8, 1, 0)
        val alarm = alarm(hour = 2, minute = 30, days = setOf(DayOfWeek.SUNDAY))

        val next = AlarmScheduler.nextTriggerMillis(alarm, now)

        val expected = ZonedDateTime.of(2026, 3, 15, 2, 30, 0, 0, nyc).toInstant().toEpochMilli()
        assertThat(next).isEqualTo(expected)
    }

    @Test fun `fall-back day does not use the second repeated occurrence after the first has passed`() {
        val now = ZonedDateTime.of(2026, 11, 1, 1, 45, 0, 0, nyc).toInstant().toEpochMilli()
        val alarm = alarm(hour = 1, minute = 30, days = DayOfWeek.values().toSet())

        val next = AlarmScheduler.nextTriggerMillis(alarm, now)

        val expected = ZonedDateTime.of(2026, 11, 2, 1, 30, 0, 0, nyc).toInstant().toEpochMilli()
        assertThat(next).isEqualTo(expected)
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
