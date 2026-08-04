package com.chronoplex.app.data

import com.chronoplex.app.data.db.ClockDao
import com.chronoplex.app.data.db.ClockEntity
import com.chronoplex.app.data.db.ClockGroupEntity
import com.chronoplex.app.domain.Clock
import com.chronoplex.app.domain.Group
import com.chronoplex.app.domain.Validate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ClockRepository(private val dao: ClockDao) {
    fun observeAll(): Flow<List<Clock>> =
        dao.observeAll().map { list -> list.map { it.toDomain().clean() } }

    suspend fun getAll(): List<Clock> = dao.getAll().map { it.toDomain().clean() }

    suspend fun getById(id: Long): Clock? = dao.getById(id)?.toDomain()?.clean()

    suspend fun upsert(clock: Clock): Long {
        val cleaned = clock.clean()
        return if (cleaned.id == 0L) {
            dao.upsert(ClockEntity.fromDomain(cleaned))
        } else {
            dao.update(ClockEntity.fromDomain(cleaned))
            cleaned.id
        }
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    suspend fun deleteAll() = dao.deleteAll()

    // ----- Grouping -----

    fun observeGroups(): Flow<List<Group>> =
        dao.observeGroups().map { list -> list.map { it.toDomain() } }

    suspend fun createGroup(name: String): Long {
        val cleanName = Validate.label(name).ifBlank { "Group" }
        return dao.upsertGroup(ClockGroupEntity(name = cleanName, sortOrder = System.currentTimeMillis(), collapsed = false))
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

    /** Reassign sortOrder of [orderedIds] to 0, 1, 2, … in the given order. */
    suspend fun reorderItems(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> dao.setSortOrder(id, index.toLong()) }
    }

    suspend fun reorderGroups(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> dao.setGroupSortOrder(id, index.toLong()) }
    }

    private fun Clock.clean(): Clock = copy(
        label = Validate.label(label),
        zoneId = Validate.zoneId(zoneId),
    )
}
