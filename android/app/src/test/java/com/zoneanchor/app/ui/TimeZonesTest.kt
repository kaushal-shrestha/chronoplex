package com.zoneanchor.app.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TimeZonesTest {

    @Test fun `offset label formats positive negative and zero offsets`() {
        assertThat(ZoneOption("UTC", "UTC", "UTC", 0).offsetLabel).isEqualTo("UTC+00:00")
        assertThat(ZoneOption("Asia/Kathmandu", "Kathmandu", "Asia", 345).offsetLabel).isEqualTo("UTC+05:45")
        assertThat(ZoneOption("America/New_York", "New York", "America", -300).offsetLabel).isEqualTo("UTC-05:00")
    }

    @Test fun `blank unrestricted query returns pinned zones above the rest`() {
        val result = TimeZones.query("")

        assertThat(result.pinned.map { it.id }).containsAtLeast("America/New_York", "UTC")
        assertThat(result.rest.map { it.id }).doesNotContain("America/New_York")
        assertThat(result.rest.map { it.id }).doesNotContain("UTC")
    }

    @Test fun `non blank query matches id and city labels without pinned zones`() {
        val byId = TimeZones.query("America/New_York")
        val byCity = TimeZones.query("new york")

        assertThat(byId.pinned).isEmpty()
        assertThat(byId.rest.map { it.id }).contains("America/New_York")
        assertThat(byCity.pinned).isEmpty()
        assertThat(byCity.rest.map { it.id }).contains("America/New_York")
    }

    @Test fun `restricted query only returns allowed ids and never pins`() {
        val allowed = setOf("UTC", "Asia/Tokyo")
        val blank = TimeZones.query("", restrictTo = allowed)
        val tokyo = TimeZones.query("Tokyo", restrictTo = allowed)

        assertThat(blank.pinned).isEmpty()
        assertThat(blank.rest.map { it.id }).containsExactlyElementsIn(allowed)
        assertThat(tokyo.pinned).isEmpty()
        assertThat(tokyo.rest.map { it.id }).containsExactly("Asia/Tokyo")
    }

    @Test fun `filter concatenates pinned and rest results`() {
        val result = TimeZones.query("")
        val filtered = TimeZones.filter("")

        assertThat(filtered).containsExactlyElementsIn(result.pinned + result.rest).inOrder()
    }

    @Test fun `available zones keep canonical anchors and omit legacy aliases`() {
        val ids = TimeZones.all().map { it.id }

        assertThat(ids).contains("UTC")
        assertThat(ids).contains("GMT")
        assertThat(ids).contains("America/New_York")
        assertThat(ids).doesNotContain("EST")
        assertThat(ids).doesNotContain("Etc/GMT")
        assertThat(ids).doesNotContain("SystemV/EST5")
    }
}
