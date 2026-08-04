package com.zoneanchor.app.data

import com.google.common.truth.Truth.assertThat
import com.zoneanchor.app.data.db.StopwatchDao
import com.zoneanchor.app.data.db.StopwatchEntity
import com.zoneanchor.app.data.db.StopwatchGroupEntity
import com.zoneanchor.app.data.db.StopwatchLapEntity
import com.zoneanchor.app.domain.Stopwatch
import com.zoneanchor.app.domain.StopwatchState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test

class StopwatchRepositoryTest {
    private val dao = FakeStopwatchDao()
    private val repository = StopwatchRepository(dao)

    @Test fun `upsert cleans labels and accumulated time`(): Unit = runBlocking {
        val id = repository.upsert(
            Stopwatch(
                label = "  ".plus("x".repeat(120)),
                state = StopwatchState.PAUSED,
                accumulatedMillis = -1_000L,
            )
        )

        val saved = repository.getById(id)

        assertThat(saved?.label?.length).isEqualTo(80)
        assertThat(saved?.accumulatedMillis).isEqualTo(0L)
        assertThat(saved?.state).isEqualTo(StopwatchState.PAUSED)
    }

    @Test fun `existing stopwatch update keeps the same id and corrupt state reads as idle`(): Unit = runBlocking {
        val id = repository.upsert(Stopwatch(label = "Run"))

        val returned = repository.upsert(
            Stopwatch(
                id = id,
                label = "Walk",
                state = StopwatchState.RUNNING,
                startedAtMillis = 1_000L,
                accumulatedMillis = 2_000L,
            )
        )
        dao.upsert(
            StopwatchEntity(
                id = 99,
                label = "Bad",
                state = "BROKEN",
                startedAtMillis = null,
                accumulatedMillis = 0,
                sortOrder = 9,
            )
        )

        assertThat(returned).isEqualTo(id)
        assertThat(repository.getById(id)?.label).isEqualTo("Walk")
        assertThat(repository.getById(99)?.state).isEqualTo(StopwatchState.IDLE)
    }

    @Test fun `update state rename and missing rows are handled safely`(): Unit = runBlocking {
        repository.updateState(404L, StopwatchState.RUNNING, startedAtMillis = 1_000L, accumulatedMillis = 0L)
        repository.rename(404L, "Missing")

        val id = repository.upsert(Stopwatch(label = "Run"))
        repository.updateState(id, StopwatchState.RUNNING, startedAtMillis = 1_000L, accumulatedMillis = 2_000L)
        repository.rename(id, "  Morning run  ")

        val saved = repository.getById(id)

        assertThat(saved?.state).isEqualTo(StopwatchState.RUNNING)
        assertThat(saved?.startedAtMillis).isEqualTo(1_000L)
        assertThat(saved?.accumulatedMillis).isEqualTo(2_000L)
        assertThat(saved?.label).isEqualTo("Morning run")
    }

    @Test fun `laps are numbered observed and cleared by stopwatch id`(): Unit = runBlocking {
        val stopwatchId = repository.upsert(Stopwatch(label = "Run"))
        val otherId = repository.upsert(Stopwatch(label = "Walk"))

        repository.addLap(stopwatchId, 10_000L)
        repository.addLap(stopwatchId, 25_000L)
        repository.addLap(otherId, 5_000L)

        assertThat(repository.observeLaps(stopwatchId).first()).containsExactly(
            com.zoneanchor.app.domain.StopwatchLap(stopwatchId, 1, 10_000L),
            com.zoneanchor.app.domain.StopwatchLap(stopwatchId, 2, 25_000L),
        ).inOrder()

        repository.clearLaps(stopwatchId)

        assertThat(repository.observeLaps(stopwatchId).first()).isEmpty()
        assertThat(repository.observeLaps(otherId).first()).hasSize(1)
    }

    @Test fun `delete removes laps for one stopwatch and deleteAll removes everything`(): Unit = runBlocking {
        val first = repository.upsert(Stopwatch(label = "A"))
        val second = repository.upsert(Stopwatch(label = "B"))
        repository.addLap(first, 1_000L)
        repository.addLap(second, 2_000L)

        repository.delete(first)

        assertThat(repository.observeAll().first().map { it.id }).containsExactly(second)
        assertThat(repository.observeLaps(first).first()).isEmpty()
        assertThat(repository.observeLaps(second).first()).hasSize(1)

        repository.deleteAll()

        assertThat(repository.observeAll().first()).isEmpty()
        assertThat(repository.observeLaps(second).first()).isEmpty()
    }

    @Test fun `group lifecycle assigns renames collapses deletes and reorders`(): Unit = runBlocking {
        val stopwatchId = repository.upsert(Stopwatch(label = "Run"))
        val first = repository.createGroup("  Fitness  ")
        val second = repository.createGroup("")

        repository.assignToGroup(stopwatchId, first)
        repository.renameGroup(first, "")
        repository.renameGroup(second, " Walks ")
        repository.setCollapsed(first, true)
        repository.reorderGroups(listOf(second, first))

        assertThat(repository.getById(stopwatchId)?.groupId).isEqualTo(first)
        assertThat(dao.groups.map { it.name }).containsExactly("Fitness", "Walks")
        assertThat(dao.groups.map { it.id }).containsExactly(second, first).inOrder()
        assertThat(repository.observeGroups().first().map { it.id }).containsExactly(second, first).inOrder()
        assertThat(dao.groups.first { it.id == first }.collapsed).isTrue()

        repository.deleteGroup(first)

        assertThat(repository.getById(stopwatchId)?.groupId).isNull()
        assertThat(dao.groups.map { it.id }).containsExactly(second)
    }

    @Test fun `missing group edits are ignored and item reorder updates sort order`(): Unit = runBlocking {
        val first = repository.upsert(Stopwatch(label = "A", sortOrder = 0))
        val second = repository.upsert(Stopwatch(label = "B", sortOrder = 1))

        repository.renameGroup(404L, "Missing")
        repository.setCollapsed(404L, true)
        repository.reorderItems(listOf(second, first))

        assertThat(repository.observeAll().first().map { it.id }).containsExactly(second, first).inOrder()
        assertThat(dao.groups).isEmpty()
    }

    private class FakeStopwatchDao : StopwatchDao {
        private var nextId = 1L
        private val rowsState = MutableStateFlow<List<StopwatchEntity>>(emptyList())
        private val lapsState = MutableStateFlow<List<StopwatchLapEntity>>(emptyList())
        private val groupsState = MutableStateFlow<List<StopwatchGroupEntity>>(emptyList())
        val groups: List<StopwatchGroupEntity> get() = groupsState.value

        override fun observeAll(): Flow<List<StopwatchEntity>> = rowsState
        override suspend fun getById(id: Long): StopwatchEntity? = rowsState.value.firstOrNull { it.id == id }

        override suspend fun upsert(stopwatch: StopwatchEntity): Long {
            val id = if (stopwatch.id == 0L) nextId++ else stopwatch.id
            rowsState.value = (rowsState.value.filterNot { it.id == id } + stopwatch.copy(id = id))
                .sortedBy { it.sortOrder }
            return id
        }

        override suspend fun update(stopwatch: StopwatchEntity) {
            rowsState.value = rowsState.value.map { if (it.id == stopwatch.id) stopwatch else it }
                .sortedBy { it.sortOrder }
        }

        override suspend fun deleteById(id: Long) {
            rowsState.value = rowsState.value.filterNot { it.id == id }
        }

        override suspend fun deleteAll() {
            rowsState.value = emptyList()
        }

        override suspend fun deleteAllLaps() {
            lapsState.value = emptyList()
        }

        override fun observeLaps(stopwatchId: Long): Flow<List<StopwatchLapEntity>> =
            MutableStateFlow(lapsState.value.filter { it.stopwatchId == stopwatchId }.sortedBy { it.lapNumber })

        override suspend fun maxLapNumber(stopwatchId: Long): Int =
            lapsState.value.filter { it.stopwatchId == stopwatchId }.maxOfOrNull { it.lapNumber } ?: 0

        override suspend fun insertLap(lap: StopwatchLapEntity) {
            lapsState.value = (lapsState.value.filterNot {
                it.stopwatchId == lap.stopwatchId && it.lapNumber == lap.lapNumber
            } + lap).sortedWith(compareBy({ it.stopwatchId }, { it.lapNumber }))
        }

        override suspend fun deleteLaps(stopwatchId: Long) {
            lapsState.value = lapsState.value.filterNot { it.stopwatchId == stopwatchId }
        }

        override suspend fun assignToGroup(id: Long, groupId: Long?) {
            rowsState.value = rowsState.value.map {
                if (it.id == id) it.copy(groupId = groupId) else it
            }
        }

        override suspend fun unassignGroup(groupId: Long) {
            rowsState.value = rowsState.value.map {
                if (it.groupId == groupId) it.copy(groupId = null) else it
            }
        }

        override fun observeGroups(): Flow<List<StopwatchGroupEntity>> = groupsState
        override suspend fun getGroupById(id: Long): StopwatchGroupEntity? = groups.firstOrNull { it.id == id }

        override suspend fun upsertGroup(group: StopwatchGroupEntity): Long {
            val id = if (group.id == 0L) nextId++ else group.id
            groupsState.value = groups.filterNot { it.id == id } + group.copy(id = id)
            return id
        }

        override suspend fun updateGroup(group: StopwatchGroupEntity) {
            groupsState.value = groups.map { if (it.id == group.id) group else it }
        }

        override suspend fun deleteGroupById(id: Long) {
            groupsState.value = groups.filterNot { it.id == id }
        }

        override suspend fun setSortOrder(id: Long, sortOrder: Long) {
            rowsState.value = rowsState.value.map {
                if (it.id == id) it.copy(sortOrder = sortOrder) else it
            }.sortedBy { it.sortOrder }
        }

        override suspend fun setGroupSortOrder(id: Long, sortOrder: Long) {
            groupsState.value = groups.map {
                if (it.id == id) it.copy(sortOrder = sortOrder) else it
            }.sortedBy { it.sortOrder }
        }
    }
}
