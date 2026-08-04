package com.zoneanchor.app.data

import com.zoneanchor.app.data.db.AlarmDao
import com.zoneanchor.app.data.db.AlarmEntity
import com.zoneanchor.app.data.db.AlarmGroupEntity
import com.zoneanchor.app.domain.Alarm
import com.zoneanchor.app.domain.Group
import com.zoneanchor.app.domain.Validate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlarmRepository(private val dao: AlarmDao) {
    fun observeAll(): Flow<List<Alarm>> =
        dao.observeAll().map { list -> list.map { it.toDomain().clean() } }

    suspend fun getAllEnabled(): List<Alarm> = dao.getAllEnabled().map { it.toDomain().clean() }

    suspend fun getById(id: Long): Alarm? = dao.getById(id)?.toDomain()?.clean()

    suspend fun getByClockId(clockId: Long): List<Alarm> =
        dao.getByClockId(clockId).map { it.toDomain().clean() }

    suspend fun upsert(alarm: Alarm): Long {
        val cleaned = alarm.clean()
        return if (cleaned.id == 0L) {
            dao.upsert(AlarmEntity.fromDomain(cleaned))
        } else {
            dao.update(AlarmEntity.fromDomain(cleaned))
            cleaned.id
        }
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) {
        val existing = dao.getById(id) ?: return
        dao.update(existing.copy(enabled = enabled))
    }

    suspend fun delete(id: Long) = dao.deleteById(id)
    suspend fun deleteAll() = dao.deleteAll()

    // ----- Grouping -----

    fun observeGroups(): Flow<List<Group>> =
        dao.observeGroups().map { list -> list.map { it.toDomain() } }

    suspend fun createGroup(name: String): Long {
        val cleanName = Validate.label(name).ifBlank { "Group" }
        return dao.upsertGroup(AlarmGroupEntity(name = cleanName, sortOrder = System.currentTimeMillis(), collapsed = false))
    }

    suspend fun renameGroup(id: Long, name: String) {
        val existing = dao.getGroupById(id) ?: return
        dao.updateGroup(existing.copy(name = Validate.label(name).ifBlank { existing.name }))
    }

    suspend fun setCollapsed(id: Long, collapsed: Boolean) {
        val existing = dao.getGroupById(id) ?: return
        dao.updateGroup(existing.copy(collapsed = collapsed))
    }

    suspend fun deleteGroup(id: Long) {
        dao.unassignGroup(id)
        dao.deleteGroupById(id)
    }

    suspend fun assignToGroup(id: Long, groupId: Long?) = dao.assignToGroup(id, groupId)

    suspend fun detachClock(clockId: Long) = dao.detachClock(clockId)

    suspend fun detachAllClocks() = dao.detachAllClocks()

    /** Alarms use (hour, minute, id) for natural sort; only groups can be reordered. */
    suspend fun reorderGroups(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> dao.setGroupSortOrder(id, index.toLong()) }
    }

    /** Persist snooze state without re-routing through the rest of upsert's bookkeeping. */
    suspend fun setSnoozeUntil(id: Long, snoozeUntilMillis: Long?) {
        val existing = dao.getById(id) ?: return
        dao.update(existing.copy(snoozeUntilMillis = snoozeUntilMillis))
    }

    private fun Alarm.clean(): Alarm = copy(
        label = Validate.label(label),
        zoneId = Validate.zoneId(zoneId),
        hour = Validate.hour(hour),
        minute = Validate.minute(minute),
        daysMask = Validate.daysMask(daysMask),
        repeatType = Validate.repeatType(repeatType, daysMask),
        repeatInterval = Validate.repeatInterval(repeatInterval),
        repeatStartDate = Validate.repeatStartDate(repeatStartDate),
        monthlyDay = Validate.monthlyDay(monthlyDay),
        monthlyOrdinal = Validate.monthlyOrdinal(monthlyOrdinal),
        monthlyWeekday = Validate.monthlyWeekday(monthlyWeekday),
    )
}
