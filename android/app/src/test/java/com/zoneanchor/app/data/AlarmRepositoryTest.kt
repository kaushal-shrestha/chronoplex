package com.zoneanchor.app.data

import com.zoneanchor.app.data.db.AlarmDao
import com.zoneanchor.app.data.db.AlarmEntity
import com.zoneanchor.app.data.db.AlarmGroupEntity
import com.zoneanchor.app.domain.Alarm
import com.zoneanchor.app.domain.AlarmRepeatType
import com.zoneanchor.app.domain.DayMask
import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test

class AlarmRepositoryTest {
    private val dao = FakeAlarmDao()
    private val repository = AlarmRepository(dao)

    @Test
    fun `upsert and read preserve advanced recurrence fields`(): Unit = runBlocking {
        val id = repository.upsert(
            Alarm(
                label = "  Quarterly sync  ",
                zoneId = "America/New_York",
                hour = 9,
                minute = 30,
                daysMask = DayMask.toMask(setOf(DayOfWeek.MONDAY)),
                soundEnabled = false,
                vibrationEnabled = false,
                enabled = true,
                repeatType = AlarmRepeatType.MONTHLY_DAY,
                repeatInterval = 6,
                repeatStartDate = "2026-08-03",
                monthlyDay = 3,
                monthlyOrdinal = 4,
                monthlyWeekday = DayOfWeek.SUNDAY.value,
            )
        )

        val saved = repository.getById(id)

        assertThat(saved?.label).isEqualTo("Quarterly sync")
        assertThat(saved?.repeatType).isEqualTo(AlarmRepeatType.MONTHLY_DAY)
        assertThat(saved?.repeatInterval).isEqualTo(6)
        assertThat(saved?.repeatStartDate).isEqualTo("2026-08-03")
        assertThat(saved?.monthlyDay).isEqualTo(3)
        assertThat(saved?.monthlyOrdinal).isEqualTo(4)
        assertThat(saved?.monthlyWeekday).isEqualTo(DayOfWeek.SUNDAY.value)
        assertThat(saved?.soundEnabled).isFalse()
        assertThat(saved?.vibrationEnabled).isFalse()

        val returnedId = repository.upsert(saved!!.copy(label = "Updated sync"))

        assertThat(returnedId).isEqualTo(id)
        assertThat(repository.getById(id)?.label).isEqualTo("Updated sync")
    }

    @Test
    fun `upsert with explicit backup id inserts missing alarm`(): Unit = runBlocking {
        val returnedId = repository.upsert(
            Alarm(
                id = 77L,
                label = "  Backup alarm  ",
                zoneId = "America/New_York",
                hour = 16,
                minute = 0,
                daysMask = DayMask.EVERY_DAY,
            )
        )
        val saved = repository.getById(77L)

        assertThat(returnedId).isEqualTo(77L)
        assertThat(saved?.id).isEqualTo(77L)
        assertThat(saved?.label).isEqualTo("Backup alarm")
        assertThat(saved?.hour).isEqualTo(16)
    }

    @Test
    fun `repository sanitizes corrupt alarm rows on read`(): Unit = runBlocking {
        dao.upsert(
            AlarmEntity(
                id = 1,
                label = "x".repeat(100),
                zoneId = "Mars/Olympus_Mons",
                hour = 99,
                minute = -2,
                daysMask = 0b11111111,
                soundEnabled = true,
                vibrationEnabled = true,
                enabled = true,
                repeatType = "NOPE",
                repeatInterval = 500,
                repeatStartDate = "not-a-date",
                monthlyDay = 80,
                monthlyOrdinal = 99,
                monthlyWeekday = -1,
            )
        )

        val saved = repository.getById(1)

        assertThat(saved?.label?.length).isEqualTo(80)
        assertThat(saved?.hour).isEqualTo(23)
        assertThat(saved?.minute).isEqualTo(0)
        assertThat(saved?.daysMask).isEqualTo(DayMask.EVERY_DAY)
        assertThat(saved?.repeatType).isEqualTo(AlarmRepeatType.WEEKLY)
        assertThat(saved?.repeatInterval).isEqualTo(99)
        assertThat(saved?.repeatStartDate).isEmpty()
        assertThat(saved?.monthlyDay).isEqualTo(31)
        assertThat(saved?.monthlyOrdinal).isEqualTo(4)
        assertThat(saved?.monthlyWeekday).isEqualTo(DayOfWeek.MONDAY.value)
    }

    @Test
    fun `group lifecycle assigns unassigns and reorders`(): Unit = runBlocking {
        val alarmId = repository.upsert(
            Alarm(label = "Wake", zoneId = "UTC", hour = 7, minute = 0, daysMask = DayMask.EVERY_DAY)
        )
        val work = repository.createGroup("  Work  ")
        val travel = repository.createGroup("Travel")
        val fallback = repository.createGroup("")

        repository.assignToGroup(alarmId, work)
        repository.reorderGroups(listOf(travel, work))
        repository.setCollapsed(work, true)

        assertThat(repository.getById(alarmId)?.groupId).isEqualTo(work)
        assertThat(dao.groups.map { it.id }).containsExactly(travel, work, fallback).inOrder()
        assertThat(dao.groups.first { it.id == fallback }.name).isEqualTo("Group")
        assertThat(repository.observeGroups().first().map { it.id }).containsExactly(travel, work, fallback).inOrder()
        assertThat(dao.groups.first { it.id == work }.collapsed).isTrue()

        repository.deleteGroup(work)

        assertThat(repository.getById(alarmId)?.groupId).isNull()
        assertThat(dao.groups.map { it.id }).containsExactly(travel, fallback)
    }

    @Test
    fun `clock attachment can be queried and detached`(): Unit = runBlocking {
        val clockId = 42L
        val attached = repository.upsert(
            Alarm(
                label = "Attached",
                zoneId = "UTC",
                hour = 8,
                minute = 0,
                daysMask = DayMask.EVERY_DAY,
                clockId = clockId,
            )
        )
        repository.upsert(
            Alarm(
                label = "Detached",
                zoneId = "UTC",
                hour = 9,
                minute = 0,
                daysMask = DayMask.EVERY_DAY,
            )
        )

        assertThat(repository.getByClockId(clockId).map { it.id }).containsExactly(attached)

        repository.detachClock(clockId)

        assertThat(repository.getByClockId(clockId)).isEmpty()
        assertThat(repository.getById(attached)?.clockId).isNull()
    }

    @Test
    fun `enabled query setEnabled snooze and delete paths are safe`(): Unit = runBlocking {
        repository.setEnabled(404L, false)
        repository.setSnoozeUntil(404L, 1_000L)

        val enabled = repository.upsert(
            Alarm(label = "Enabled", zoneId = "UTC", hour = 8, minute = 0, daysMask = DayMask.EVERY_DAY)
        )
        val disabled = repository.upsert(
            Alarm(
                label = "Disabled",
                zoneId = "UTC",
                hour = 9,
                minute = 0,
                daysMask = DayMask.EVERY_DAY,
                enabled = false,
            )
        )

        assertThat(repository.getAllEnabled().map { it.id }).containsExactly(enabled)

        repository.setEnabled(enabled, false)
        repository.setSnoozeUntil(enabled, 9_000L)

        assertThat(repository.getById(enabled)?.enabled).isFalse()
        assertThat(repository.getById(enabled)?.snoozeUntilMillis).isEqualTo(9_000L)
        assertThat(repository.getAllEnabled()).isEmpty()

        repository.delete(disabled)

        assertThat(repository.getById(disabled)).isNull()

        repository.deleteAll()

        assertThat(dao.rows).isEmpty()
    }

    @Test
    fun `group edits ignore missing groups and blank rename keeps old name`(): Unit = runBlocking {
        val groupId = repository.createGroup("  Home  ")

        repository.renameGroup(404L, "Missing")
        repository.setCollapsed(404L, true)
        repository.renameGroup(groupId, "")

        assertThat(dao.groups.single().name).isEqualTo("Home")
        assertThat(dao.groups.single().collapsed).isFalse()
    }

    @Test
    fun `detachAllClocks clears every alarm clock attachment`(): Unit = runBlocking {
        val first = repository.upsert(
            Alarm(label = "A", zoneId = "UTC", hour = 8, minute = 0, daysMask = DayMask.EVERY_DAY, clockId = 1)
        )
        val second = repository.upsert(
            Alarm(label = "B", zoneId = "UTC", hour = 9, minute = 0, daysMask = DayMask.EVERY_DAY, clockId = 2)
        )

        repository.detachAllClocks()

        assertThat(repository.getById(first)?.clockId).isNull()
        assertThat(repository.getById(second)?.clockId).isNull()
        assertThat(repository.observeAll().first().map { it.id }).containsExactly(first, second)
    }

    private class FakeAlarmDao : AlarmDao {
        private var nextId = 1L
        private val rowStore = mutableListOf<AlarmEntity>()
        private val groupsState = MutableStateFlow<List<AlarmGroupEntity>>(emptyList())
        val rows: List<AlarmEntity> get() = rowStore.toList()
        val groups: List<AlarmGroupEntity> get() = groupsState.value

        override fun observeAll(): Flow<List<AlarmEntity>> = MutableStateFlow(rowStore)
        override suspend fun getAllEnabled(): List<AlarmEntity> = rowStore.filter { it.enabled }
        override suspend fun getAll(): List<AlarmEntity> =
            rowStore.sortedWith(compareBy({ it.hour }, { it.minute }, { it.id }))

        override suspend fun getById(id: Long): AlarmEntity? = rowStore.firstOrNull { it.id == id }
        override suspend fun getByClockId(clockId: Long): List<AlarmEntity> =
            rowStore.filter { it.clockId == clockId }.sortedWith(compareBy({ it.hour }, { it.minute }, { it.id }))

        override suspend fun upsert(alarm: AlarmEntity): Long {
            val id = if (alarm.id == 0L) nextId++ else alarm.id
            rowStore.removeAll { it.id == id }
            rowStore.add(alarm.copy(id = id))
            return id
        }

        override suspend fun update(alarm: AlarmEntity) {
            rowStore.replace(alarm.id, alarm)
        }

        override suspend fun delete(alarm: AlarmEntity) {
            rowStore.removeAll { it.id == alarm.id }
        }

        override suspend fun deleteById(id: Long) {
            rowStore.removeAll { it.id == id }
        }

        override suspend fun deleteAll() {
            rowStore.clear()
        }

        override suspend fun assignToGroup(id: Long, groupId: Long?) {
            getById(id)?.let { rowStore.replace(id, it.copy(groupId = groupId)) }
        }

        override suspend fun detachClock(clockId: Long) {
            rowStore.toList().forEach { row ->
                if (row.clockId == clockId) rowStore.replace(row.id, row.copy(clockId = null))
            }
        }

        override suspend fun detachAllClocks() {
            rowStore.toList().forEach { row ->
                if (row.clockId != null) rowStore.replace(row.id, row.copy(clockId = null))
            }
        }

        override suspend fun unassignGroup(groupId: Long) {
            rowStore.toList().forEach { row ->
                if (row.groupId == groupId) rowStore.replace(row.id, row.copy(groupId = null))
            }
        }

        override fun observeGroups(): Flow<List<AlarmGroupEntity>> = groupsState
        override suspend fun getAllGroups(): List<AlarmGroupEntity> = groups
        override suspend fun getGroupById(id: Long): AlarmGroupEntity? = groups.firstOrNull { it.id == id }

        override suspend fun upsertGroup(group: AlarmGroupEntity): Long {
            val id = if (group.id == 0L) nextId++ else group.id
            groupsState.value = groups.filterNot { it.id == id } + group.copy(id = id)
            return id
        }

        override suspend fun updateGroup(group: AlarmGroupEntity) {
            groupsState.value = groups.map { if (it.id == group.id) group else it }
        }

        override suspend fun deleteGroupById(id: Long) {
            groupsState.value = groups.filterNot { it.id == id }
        }

        override suspend fun setGroupSortOrder(id: Long, sortOrder: Long) {
            groupsState.value = groups.map {
                if (it.id == id) it.copy(sortOrder = sortOrder) else it
            }.sortedBy { it.sortOrder }
        }

        private fun MutableList<AlarmEntity>.replace(id: Long, alarm: AlarmEntity) {
            val index = indexOfFirst { it.id == id }
            if (index >= 0) this[index] = alarm
        }
    }
}
