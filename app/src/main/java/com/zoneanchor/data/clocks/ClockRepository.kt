package com.zoneanchor.data.clocks

import com.zoneanchor.model.ClockEntry
import com.zoneanchor.model.ZonedAlarm
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ClockRepository(private val dao: ClockDao) {
    val clocks: Flow<List<ClockEntry>> = dao.observeAll().map { rows -> rows.map { it.toModel() } }

    suspend fun allOnce(): List<ClockEntry> = dao.getAll().map { it.toModel() }

    suspend fun find(zoneId: String): ClockEntry? = dao.findByZone(zoneId)?.toModel()

    suspend fun addOrUpdate(oldZoneId: String?, newZoneId: String, label: String) {
        val cleanZone = validZoneIdOrNull(newZoneId) ?: return
        val cleanLabel = label.trim()
        val oldPosition = oldZoneId?.let { dao.findByZone(it)?.position }
        if (oldZoneId != null && oldZoneId != cleanZone) {
            dao.deleteByZone(oldZoneId)
        }
        val position = dao.findByZone(cleanZone)?.position ?: oldPosition ?: (dao.maxPosition() + 1)
        dao.upsert(ClockEntity(cleanZone, cleanLabel, position))
    }

    suspend fun delete(zoneId: String) = dao.deleteByZone(zoneId)

    suspend fun seedDefaultsIfEmpty() {
        if (dao.count() > 0) return
        dao.upsertAll(defaultClocks().mapIndexed { index, entry -> entry.toEntity(index) })
    }

    suspend fun upsertImported(entries: List<ClockEntry>) {
        dao.upsertAll(entries.distinctBy { it.zoneId }.mapIndexed { index, entry -> entry.toEntity(index) })
    }

    private fun defaultClocks(): List<ClockEntry> {
        val localZone = ZoneId.systemDefault().id
        val zones = buildList {
            if (ZonedAlarm.DEFAULT_ZONE_ID != localZone) add(ZonedAlarm.DEFAULT_ZONE_ID)
            if ("UTC" != localZone) add("UTC")
        }.ifEmpty { listOf("Europe/London") }
        return zones.mapIndexed { index, zone -> ClockEntry(zone, position = index) }
    }

    private fun validZoneIdOrNull(value: String): String? {
        return try {
            ZoneId.of(value).id
        } catch (_: Exception) {
            null
        }
    }

    private fun ClockEntity.toModel() = ClockEntry(zoneId, label.trim(), position)

    private fun ClockEntry.toEntity(index: Int = position) =
        ClockEntity(ZonedAlarm.validZoneId(zoneId), label.trim(), index)
}
