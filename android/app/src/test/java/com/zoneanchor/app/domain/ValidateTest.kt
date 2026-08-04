package com.zoneanchor.app.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
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
}
