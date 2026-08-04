package com.zoneanchor.app.data

import com.zoneanchor.app.data.db.ClockDao
import com.zoneanchor.app.data.db.ClockEntity
import com.zoneanchor.app.data.db.ClockGroupEntity
import com.zoneanchor.app.domain.Clock
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ClockRepositoryTest {
    private val dao = FakeClockDao()
    private val repository = ClockRepository(dao)

    @Test
    fun `upsert trims labels and falls back for invalid zones`(): Unit = runBlocking {
        val id = repository.upsert(Clock(label = "  Tokyo  ", zoneId = "Asia/Tokyo"))
        val invalidId = repository.upsert(Clock(label = "Broken", zoneId = "Mars/Olympus_Mons"))

        val clocks = repository.getAll()

        assertThat(clocks.first { it.id == id }.label).isEqualTo("Tokyo")
        assertThat(clocks.first { it.id == id }.zoneId).isEqualTo("Asia/Tokyo")
        assertThat(clocks.first { it.id == invalidId }.zoneId).isNotEqualTo("Mars/Olympus_Mons")
    }

    @Test
    fun `upsert with explicit backup id inserts missing clock`(): Unit = runBlocking {
        val returnedId = repository.upsert(Clock(id = 42L, label = "  Kathmandu  ", zoneId = "Asia/Kathmandu"))
        val saved = repository.getAll().single()

        assertThat(returnedId).isEqualTo(42L)
        assertThat(saved.id).isEqualTo(42L)
        assertThat(saved.label).isEqualTo("Kathmandu")
        assertThat(saved.zoneId).isEqualTo("Asia/Kathmandu")
    }

    @Test
    fun `reorder updates clock observation order`(): Unit = runBlocking {
        val first = repository.upsert(Clock(label = "First", zoneId = "UTC", sortOrder = 0))
        val second = repository.upsert(Clock(label = "Second", zoneId = "America/New_York", sortOrder = 1))
        val third = repository.upsert(Clock(label = "Third", zoneId = "Asia/Tokyo", sortOrder = 2))

        repository.reorderItems(listOf(third, first, second))

        assertThat(repository.observeAll().first().map { it.id }).containsExactly(third, first, second).inOrder()
    }

    @Test
    fun `deleting a group keeps clocks and clears assignments`(): Unit = runBlocking {
        val clockId = repository.upsert(Clock(label = "NYC", zoneId = "America/New_York"))
        val groupId = repository.createGroup("  Travel  ")

        repository.assignToGroup(clockId, groupId)
        repository.deleteGroup(groupId)

        assertThat(repository.getAll().single().groupId).isNull()
        assertThat(dao.groups).isEmpty()
    }

    @Test
    fun `existing clock update keeps id and delete paths remove rows`(): Unit = runBlocking {
        val id = repository.upsert(Clock(label = "NYC", zoneId = "America/New_York"))
        val returnedId = repository.upsert(Clock(id = id, label = "  New York  ", zoneId = "America/New_York"))
        val other = repository.upsert(Clock(label = "UTC", zoneId = "UTC"))

        assertThat(returnedId).isEqualTo(id)
        assertThat(repository.getAll().first { it.id == id }.label).isEqualTo("New York")

        repository.delete(other)

        assertThat(repository.getAll().map { it.id }).containsExactly(id)

        repository.deleteAll()

        assertThat(repository.getAll()).isEmpty()
    }

    @Test
    fun `group edits can rename collapse and reorder while ignoring missing groups`(): Unit = runBlocking {
        val home = repository.createGroup("  Home  ")
        val travel = repository.createGroup("Travel")

        repository.renameGroup(404L, "Missing")
        repository.setCollapsed(404L, true)
        repository.renameGroup(home, "")
        repository.renameGroup(travel, "  Trips  ")
        repository.setCollapsed(home, true)
        repository.reorderGroups(listOf(travel, home))

        assertThat(dao.groups.map { it.id }).containsExactly(travel, home).inOrder()
        assertThat(repository.observeGroups().first().map { it.id }).containsExactly(travel, home).inOrder()
        assertThat(dao.groups.first { it.id == home }.name).isEqualTo("Home")
        assertThat(dao.groups.first { it.id == travel }.name).isEqualTo("Trips")
        assertThat(dao.groups.first { it.id == home }.collapsed).isTrue()
    }

    private class FakeClockDao : ClockDao {
        private var nextId = 1L
        private val rowsState = MutableStateFlow<List<ClockEntity>>(emptyList())
        private val groupsState = MutableStateFlow<List<ClockGroupEntity>>(emptyList())
        val groups: List<ClockGroupEntity> get() = groupsState.value

        override fun observeAll(): Flow<List<ClockEntity>> = rowsState
        override suspend fun getAll(): List<ClockEntity> = rowsState.value

        override suspend fun upsert(clock: ClockEntity): Long {
            val id = if (clock.id == 0L) nextId++ else clock.id
            rowsState.value = (rowsState.value.filterNot { it.id == id } + clock.copy(id = id))
                .sortedBy { it.sortOrder }
            return id
        }

        override suspend fun update(clock: ClockEntity) {
            rowsState.value = rowsState.value.map { if (it.id == clock.id) clock else it }
                .sortedBy { it.sortOrder }
        }

        override suspend fun delete(clock: ClockEntity) {
            rowsState.value = rowsState.value.filterNot { it.id == clock.id }
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

        override fun observeGroups(): Flow<List<ClockGroupEntity>> = groupsState
        override suspend fun getAllGroups(): List<ClockGroupEntity> = groups
        override suspend fun getGroupById(id: Long): ClockGroupEntity? = groups.firstOrNull { it.id == id }

        override suspend fun upsertGroup(group: ClockGroupEntity): Long {
            val id = if (group.id == 0L) nextId++ else group.id
            groupsState.value = groups.filterNot { it.id == id } + group.copy(id = id)
            return id
        }

        override suspend fun updateGroup(group: ClockGroupEntity) {
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
