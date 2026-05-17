package com.chronoplex.app.data

import com.chronoplex.app.data.db.TimerDao
import com.chronoplex.app.data.db.TimerEntity
import com.chronoplex.app.data.db.TimerGroupEntity
import com.chronoplex.app.domain.Group
import com.chronoplex.app.domain.Timer
import com.chronoplex.app.domain.TimerState
import com.chronoplex.app.domain.Validate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TimerRepository(private val dao: TimerDao) {
    fun observeAll(): Flow<List<Timer>> =
        dao.observeAll().map { list -> list.map { it.toDomain().clean() } }

    suspend fun getAllRunning(): List<Timer> = dao.getAllRunning().map { it.toDomain().clean() }

    suspend fun getById(id: Long): Timer? = dao.getById(id)?.toDomain()?.clean()

    suspend fun upsert(timer: Timer): Long {
        val cleaned = timer.clean()
        return if (cleaned.id == 0L) {
            dao.upsert(TimerEntity.fromDomain(cleaned))
        } else {
            dao.update(TimerEntity.fromDomain(cleaned))
            cleaned.id
        }
    }

    suspend fun updateState(
        id: Long,
        state: TimerState,
        endsAtMillis: Long?,
        pausedRemainingMillis: Long?,
    ) {
        val existing = dao.getById(id) ?: return
        dao.update(
            existing.copy(
                state = state.name,
                endsAtMillis = endsAtMillis,
                pausedRemainingMillis = pausedRemainingMillis,
            )
        )
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    // ----- Grouping -----

    fun observeGroups(): Flow<List<Group>> =
        dao.observeGroups().map { list -> list.map { it.toDomain() } }

    suspend fun createGroup(name: String): Long {
        val cleanName = Validate.label(name).ifBlank { "Group" }
        return dao.upsertGroup(TimerGroupEntity(name = cleanName, sortOrder = System.currentTimeMillis(), collapsed = false))
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

    suspend fun reorderItems(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> dao.setSortOrder(id, index.toLong()) }
    }

    suspend fun reorderGroups(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> dao.setGroupSortOrder(id, index.toLong()) }
    }

    private fun Timer.clean(): Timer = copy(
        label = Validate.label(label),
        durationMillis = durationMillis.coerceAtLeast(0L),
    )
}
