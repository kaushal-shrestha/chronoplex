package com.chronoplex.app.data

import com.chronoplex.app.data.db.AlarmDao
import com.chronoplex.app.data.db.AlarmEntity
import com.chronoplex.app.data.db.AlarmGroupEntity
import com.chronoplex.app.domain.Alarm
import com.chronoplex.app.domain.AlarmRepeatType
import com.chronoplex.app.domain.DayMask
import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

        repository.assignToGroup(alarmId, work)
        repository.reorderGroups(listOf(travel, work))
        repository.setCollapsed(work, true)

        assertThat(repository.getById(alarmId)?.groupId).isEqualTo(work)
        assertThat(dao.groups.map { it.id }).containsExactly(travel, work).inOrder()
        assertThat(dao.groups.first { it.id == work }.collapsed).isTrue()

        repository.deleteGroup(work)

        assertThat(repository.getById(alarmId)?.groupId).isNull()
        assertThat(dao.groups.map { it.id }).containsExactly(travel)
    }

    private class FakeAlarmDao : AlarmDao {
        private var nextId = 1L
        private val rows = mutableListOf<AlarmEntity>()
        private val groupsState = MutableStateFlow<List<AlarmGroupEntity>>(emptyList())
        val groups: List<AlarmGroupEntity> get() = groupsState.value

        override fun observeAll(): Flow<List<AlarmEntity>> = MutableStateFlow(rows)
        override suspend fun getAllEnabled(): List<AlarmEntity> = rows.filter { it.enabled }
        override suspend fun getById(id: Long): AlarmEntity? = rows.firstOrNull { it.id == id }

        override suspend fun upsert(alarm: AlarmEntity): Long {
            val id = if (alarm.id == 0L) nextId++ else alarm.id
            rows.removeAll { it.id == id }
            rows.add(alarm.copy(id = id))
            return id
        }

        override suspend fun update(alarm: AlarmEntity) {
            rows.replace(alarm.id, alarm)
        }

        override suspend fun delete(alarm: AlarmEntity) {
            rows.removeAll { it.id == alarm.id }
        }

        override suspend fun deleteById(id: Long) {
            rows.removeAll { it.id == id }
        }

        override suspend fun deleteAll() {
            rows.clear()
        }

        override suspend fun assignToGroup(id: Long, groupId: Long?) {
            getById(id)?.let { rows.replace(id, it.copy(groupId = groupId)) }
        }

        override suspend fun unassignGroup(groupId: Long) {
            rows.toList().forEach { row ->
                if (row.groupId == groupId) rows.replace(row.id, row.copy(groupId = null))
            }
        }

        override fun observeGroups(): Flow<List<AlarmGroupEntity>> = groupsState
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
