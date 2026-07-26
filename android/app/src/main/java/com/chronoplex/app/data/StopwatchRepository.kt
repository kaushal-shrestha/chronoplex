package com.chronoplex.app.data

import com.chronoplex.app.data.db.StopwatchDao
import com.chronoplex.app.data.db.StopwatchEntity
import com.chronoplex.app.data.db.StopwatchGroupEntity
import com.chronoplex.app.data.db.StopwatchLapEntity
import com.chronoplex.app.domain.Group
import com.chronoplex.app.domain.Stopwatch
import com.chronoplex.app.domain.StopwatchLap
import com.chronoplex.app.domain.StopwatchState
import com.chronoplex.app.domain.Validate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StopwatchRepository(private val dao: StopwatchDao) {
    fun observeAll(): Flow<List<Stopwatch>> =
        dao.observeAll().map { list -> list.map { it.toDomain().clean() } }

    suspend fun getById(id: Long): Stopwatch? = dao.getById(id)?.toDomain()?.clean()

    suspend fun upsert(stopwatch: Stopwatch): Long {
        val cleaned = stopwatch.clean()
        return if (cleaned.id == 0L) {
            dao.upsert(StopwatchEntity.fromDomain(cleaned))
        } else {
            dao.update(StopwatchEntity.fromDomain(cleaned))
            cleaned.id
        }
    }

    suspend fun updateState(
        id: Long,
        state: StopwatchState,
        startedAtMillis: Long?,
        accumulatedMillis: Long,
    ) {
        val existing = dao.getById(id) ?: return
        dao.update(
            existing.copy(
                state = state.name,
                startedAtMillis = startedAtMillis,
                accumulatedMillis = accumulatedMillis,
            )
        )
    }

    suspend fun rename(id: Long, label: String) {
        val existing = dao.getById(id) ?: return
        dao.update(existing.copy(label = Validate.label(label)))
    }

    suspend fun delete(id: Long) {
        dao.deleteLaps(id)
        dao.deleteById(id)
    }

    suspend fun deleteAll() {
        dao.deleteAllLaps()
        dao.deleteAll()
    }

    fun observeLaps(stopwatchId: Long): Flow<List<StopwatchLap>> =
        dao.observeLaps(stopwatchId).map { list -> list.map { it.toDomain() } }

    suspend fun addLap(stopwatchId: Long, totalElapsedMillis: Long) {
        val next = dao.maxLapNumber(stopwatchId) + 1
        dao.insertLap(StopwatchLapEntity(stopwatchId, next, totalElapsedMillis))
    }

    suspend fun clearLaps(stopwatchId: Long) = dao.deleteLaps(stopwatchId)

    // ----- Grouping -----

    fun observeGroups(): Flow<List<Group>> =
        dao.observeGroups().map { list -> list.map { it.toDomain() } }

    suspend fun createGroup(name: String): Long {
        val cleanName = Validate.label(name).ifBlank { "Group" }
        return dao.upsertGroup(StopwatchGroupEntity(name = cleanName, sortOrder = System.currentTimeMillis(), collapsed = false))
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

    private fun Stopwatch.clean(): Stopwatch = copy(
        label = Validate.label(label),
        accumulatedMillis = accumulatedMillis.coerceAtLeast(0L),
    )
}
