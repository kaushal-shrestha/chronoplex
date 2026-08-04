package com.zoneanchor.app.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId

class ValidateTest {

    @Test fun `label trims whitespace and caps length`() {
        assertThat(Validate.label("   morning  ")).isEqualTo("morning")
        assertThat(Validate.label(null)).isEmpty()
        val long = "x".repeat(500)
        assertThat(Validate.label(long).length).isEqualTo(80)
    }

    @Test fun `zoneId passes known zones unchanged`() {
        assertThat(Validate.zoneId("America/New_York")).isEqualTo("America/New_York")
        assertThat(Validate.zoneId("UTC")).isEqualTo("UTC")
    }

    @Test fun `zoneId falls back to system default on garbage input`() {
        val fallback = ZoneId.systemDefault().id
        assertThat(Validate.zoneId("Mars/Olympus_Mons")).isEqualTo(fallback)
        assertThat(Validate.zoneId(null)).isEqualTo(fallback)
        assertThat(Validate.zoneId("")).isEqualTo(fallback)
    }

    @Test fun `hour and minute are clamped`() {
        assertThat(Validate.hour(-1)).isEqualTo(0)
        assertThat(Validate.hour(99)).isEqualTo(23)
        assertThat(Validate.minute(-5)).isEqualTo(0)
        assertThat(Validate.minute(60)).isEqualTo(59)
    }

    @Test fun `daysMask strips garbage high bits but preserves zero (one-shot)`() {
        assertThat(Validate.daysMask(0)).isEqualTo(0)
        assertThat(Validate.daysMask(0b11111111)).isEqualTo(0b01111111) // bit 7 stripped
        assertThat(Validate.daysMask(DayMask.WEEKDAYS)).isEqualTo(DayMask.WEEKDAYS)
    }

    @Test fun `repeat type defaults from sanitized day mask`() {
        assertThat(Validate.repeatType(null, 0)).isEqualTo(AlarmRepeatType.ONCE)
        assertThat(Validate.repeatType(null, DayMask.WEEKDAYS)).isEqualTo(AlarmRepeatType.WEEKLY)
        assertThat(Validate.repeatType(AlarmRepeatType.MONTHLY_DAY, 0)).isEqualTo(AlarmRepeatType.MONTHLY_DAY)
        assertThat(Validate.repeatType(null, 1 shl 12)).isEqualTo(AlarmRepeatType.ONCE)
    }

    @Test fun `repeat interval and date fields are clamped or dropped`() {
        assertThat(Validate.repeatInterval(-10)).isEqualTo(1)
        assertThat(Validate.repeatInterval(1)).isEqualTo(1)
        assertThat(Validate.repeatInterval(500)).isEqualTo(99)

        assertThat(Validate.repeatStartDate("2026-08-03")).isEqualTo("2026-08-03")
        assertThat(Validate.repeatStartDate("08/03/2026")).isEmpty()
        assertThat(Validate.repeatStartDate(null)).isEmpty()
    }

    @Test fun `monthly recurrence fields are clamped to valid controls`() {
        assertThat(Validate.monthlyDay(-1)).isEqualTo(1)
        assertThat(Validate.monthlyDay(15)).isEqualTo(15)
        assertThat(Validate.monthlyDay(80)).isEqualTo(31)

        assertThat(Validate.monthlyOrdinal(-1)).isEqualTo(-1)
        assertThat(Validate.monthlyOrdinal(0)).isEqualTo(1)
        assertThat(Validate.monthlyOrdinal(3)).isEqualTo(3)
        assertThat(Validate.monthlyOrdinal(99)).isEqualTo(4)

        assertThat(Validate.monthlyWeekday(-1)).isEqualTo(DayOfWeek.MONDAY.value)
        assertThat(Validate.monthlyWeekday(DayOfWeek.THURSDAY.value)).isEqualTo(DayOfWeek.THURSDAY.value)
        assertThat(Validate.monthlyWeekday(99)).isEqualTo(DayOfWeek.SUNDAY.value)
    }
}
