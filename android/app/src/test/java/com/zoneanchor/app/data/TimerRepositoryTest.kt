package com.zoneanchor.app.data

import com.google.common.truth.Truth.assertThat
import com.zoneanchor.app.data.db.TimerDao
import com.zoneanchor.app.data.db.TimerEntity
import com.zoneanchor.app.data.db.TimerGroupEntity
import com.zoneanchor.app.domain.Timer
import com.zoneanchor.app.domain.TimerFinishMode
import com.zoneanchor.app.domain.TimerState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test

class TimerRepositoryTest {
    private val dao = FakeTimerDao()
    private val repository = TimerRepository(dao)

    @Test fun `upsert cleans labels durations and preserves finish mode`(): Unit = runBlocking {
        val id = repository.upsert(
            Timer(
                label = "  ".plus("x".repeat(120)),
                durationMillis = -10L,
                state = TimerState.FINISHED,
                finishMode = TimerFinishMode.FULL_SCREEN,
            )
        )

        val saved = repository.getById(id)

        assertThat(saved?.label?.length).isEqualTo(80)
        assertThat(saved?.durationMillis).isEqualTo(0L)
        assertThat(saved?.state).isEqualTo(TimerState.FINISHED)
        assertThat(saved?.finishMode).isEqualTo(TimerFinishMode.FULL_SCREEN)
    }

    @Test fun `existing timer update keeps the same id and running query returns clean rows`(): Unit = runBlocking {
        val id = repository.upsert(Timer(label = "Tea", durationMillis = 180_000L))

        val returnedId = repository.upsert(
            Timer(
                id = id,
                label = "Coffee",
                durationMillis = 240_000L,
                state = TimerState.RUNNING,
                endsAtMillis = 1_000L,
                pausedRemainingMillis = null,
            )
        )

        assertThat(returnedId).isEqualTo(id)
        assertThat(repository.getAllRunning().map { it.id }).containsExactly(id)
        assertThat(repository.getById(id)?.label).isEqualTo("Coffee")
    }

    @Test fun `update state is ignored for missing rows and changes existing rows`(): Unit = runBlocking {
        repository.updateState(999L, TimerState.RUNNING, endsAtMillis = 1_000L, pausedRemainingMillis = null)

        val id = repository.upsert(Timer(label = "Tea", durationMillis = 180_000L))
        repository.updateState(id, TimerState.PAUSED, endsAtMillis = null, pausedRemainingMillis = 90_000L)

        val saved = repository.getById(id)

        assertThat(saved?.state).isEqualTo(TimerState.PAUSED)
        assertThat(saved?.endsAtMillis).isNull()
        assertThat(saved?.pausedRemainingMillis).isEqualTo(90_000L)
    }

    @Test fun `observe all maps corrupt enum rows back to safe defaults`(): Unit = runBlocking {
        dao.upsert(
            TimerEntity(
                id = 7,
                label = "Bad",
                durationMillis = 1_000L,
                state = "MISSING",
                endsAtMillis = null,
                pausedRemainingMillis = null,
                finishMode = "LOUD",
                sortOrder = 0,
            )
        )

        val observed = repository.observeAll().first().single()

        assertThat(observed.state).isEqualTo(TimerState.IDLE)
        assertThat(observed.finishMode).isEqualTo(TimerFinishMode.NOTIFICATION)
    }

    @Test fun `delete operations remove one timer or all timers`(): Unit = runBlocking {
        val first = repository.upsert(Timer(label = "A", durationMillis = 1_000L))
        val second = repository.upsert(Timer(label = "B", durationMillis = 2_000L))

        repository.delete(first)

        assertThat(repository.observeAll().first().map { it.id }).containsExactly(second)

        repository.deleteAll()

        assertThat(repository.observeAll().first()).isEmpty()
    }

    @Test fun `group lifecycle assigns renames collapses deletes and reorders`(): Unit = runBlocking {
        val timerId = repository.upsert(Timer(label = "Tea", durationMillis = 180_000L))
        val first = repository.createGroup("  Focus  ")
        val second = repository.createGroup("")

        repository.assignToGroup(timerId, first)
        repository.renameGroup(first, "")
        repository.renameGroup(second, " Kitchen ")
        repository.setCollapsed(first, true)
        repository.reorderGroups(listOf(second, first))

        assertThat(repository.getById(timerId)?.groupId).isEqualTo(first)
        assertThat(dao.groups.map { it.name }).containsExactly("Focus", "Kitchen")
        assertThat(dao.groups.map { it.id }).containsExactly(second, first).inOrder()
        assertThat(repository.observeGroups().first().map { it.id }).containsExactly(second, first).inOrder()
        assertThat(dao.groups.first { it.id == first }.collapsed).isTrue()

        repository.deleteGroup(first)

        assertThat(repository.getById(timerId)?.groupId).isNull()
        assertThat(dao.groups.map { it.id }).containsExactly(second)
    }

    @Test fun `missing group edits are ignored and item reorder updates sort order`(): Unit = runBlocking {
        val first = repository.upsert(Timer(label = "A", durationMillis = 1_000L, sortOrder = 0))
        val second = repository.upsert(Timer(label = "B", durationMillis = 1_000L, sortOrder = 1))

        repository.renameGroup(404L, "Missing")
        repository.setCollapsed(404L, true)
        repository.reorderItems(listOf(second, first))

        assertThat(repository.observeAll().first().map { it.id }).containsExactly(second, first).inOrder()
        assertThat(dao.groups).isEmpty()
    }

    private class FakeTimerDao : TimerDao {
        private var nextId = 1L
        private val rowsState = MutableStateFlow<List<TimerEntity>>(emptyList())
        private val groupsState = MutableStateFlow<List<TimerGroupEntity>>(emptyList())
        val groups: List<TimerGroupEntity> get() = groupsState.value

        override fun observeAll(): Flow<List<TimerEntity>> = rowsState
        override suspend fun getAllRunning(): List<TimerEntity> = rowsState.value.filter { it.state == TimerState.RUNNING.name }
        override suspend fun getById(id: Long): TimerEntity? = rowsState.value.firstOrNull { it.id == id }

        override suspend fun upsert(timer: TimerEntity): Long {
            val id = if (timer.id == 0L) nextId++ else timer.id
            rowsState.value = (rowsState.value.filterNot { it.id == id } + timer.copy(id = id))
                .sortedBy { it.sortOrder }
            return id
        }

        override suspend fun update(timer: TimerEntity) {
            rowsState.value = rowsState.value.map { if (it.id == timer.id) timer else it }
                .sortedBy { it.sortOrder }
        }

        override suspend fun deleteById(id: Long) {
            rowsState.value = rowsState.value.filterNot { it.id == id }
        }

        override suspend fun deleteAll() {
            rowsState.value = emptyList()
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

        override fun observeGroups(): Flow<List<TimerGroupEntity>> = groupsState
        override suspend fun getGroupById(id: Long): TimerGroupEntity? = groups.firstOrNull { it.id == id }

        override suspend fun upsertGroup(group: TimerGroupEntity): Long {
            val id = if (group.id == 0L) nextId++ else group.id
            groupsState.value = groups.filterNot { it.id == id } + group.copy(id = id)
            return id
        }

        override suspend fun updateGroup(group: TimerGroupEntity) {
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
