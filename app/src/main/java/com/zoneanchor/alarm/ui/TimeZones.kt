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

    /**
     * Result of a picker query. When the query is empty we surface a small "pinned"
     * section (device default + a couple of broadly-useful zones) above the full list.
     */
    data class Results(val pinned: List<ZoneOption>, val rest: List<ZoneOption>)

    fun query(query: String, restrictTo: Set<String>? = null): Results {
        val base = if (restrictTo != null) cache.filter { it.id in restrictTo } else cache
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            val matches = base.filter {
                it.id.lowercase().contains(q) || it.city.lowercase().contains(q)
            }
            return Results(pinned = emptyList(), rest = matches)
        }
        if (restrictTo != null) return Results(pinned = emptyList(), rest = base)

        val pinnedIds = linkedSetOf(
            ZoneId.systemDefault().id,
            "America/New_York",
            "UTC",
        )
        val byId = base.associateBy { it.id }
        val pinned = pinnedIds.mapNotNull { byId[it] }
        val pinnedSet = pinned.map { it.id }.toSet()
        val rest = base.filterNot { it.id in pinnedSet }
        return Results(pinned = pinned, rest = rest)
    }

    fun filter(query: String, restrictTo: Set<String>? = null): List<ZoneOption> {
        val r = query(query, restrictTo)
        return r.pinned + r.rest
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
