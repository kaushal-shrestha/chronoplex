package com.zoneanchor.alarm.ui

import java.time.Instant
import java.time.ZoneId

data class ZoneOption(
    val id: String,
    val city: String,
    val region: String,
    val offsetMinutes: Int,
) {
    val offsetLabel: String
        get() {
            val sign = if (offsetMinutes >= 0) "+" else "-"
            val abs = kotlin.math.abs(offsetMinutes)
            val h = abs / 60
            val m = abs % 60
            return "UTC$sign%02d:%02d".format(h, m)
        }
}

object TimeZones {
    private val cache: List<ZoneOption> by lazy { loadAll() }

    fun all(): List<ZoneOption> = cache

    fun filter(query: String, restrictTo: Set<String>? = null): List<ZoneOption> {
        val base = if (restrictTo != null) cache.filter { it.id in restrictTo } else cache
        if (query.isBlank()) return base
        val q = query.trim().lowercase()
        return base.filter { it.id.lowercase().contains(q) || it.city.lowercase().contains(q) }
    }

    private fun loadAll(): List<ZoneOption> {
        val now = Instant.now()
        return ZoneId.getAvailableZoneIds()
            // Drop legacy aliases (no slash) like "EST", "GMT+1", etc. Keep only canonical region/city ids.
            .filter { it.contains('/') && !it.startsWith("Etc/") && !it.startsWith("SystemV/") }
            .map { id ->
                val zone = ZoneId.of(id)
                val offset = zone.rules.getOffset(now).totalSeconds / 60
                val parts = id.split('/')
                val region = parts.first()
                val city = parts.drop(1).joinToString(" / ").replace('_', ' ')
                ZoneOption(id = id, city = city, region = region, offsetMinutes = offset)
            }
            .sortedWith(compareBy({ it.offsetMinutes }, { it.city }))
    }
}
