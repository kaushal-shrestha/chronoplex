package com.chronoplex.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClockDao {
    @Query("SELECT * FROM clocks ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<ClockEntity>>

    @Query("SELECT * FROM clocks ORDER BY sortOrder ASC")
    suspend fun getAll(): List<ClockEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(clock: ClockEntity): Long

    @Update
    suspend fun update(clock: ClockEntity)

    @Delete
    suspend fun delete(clock: ClockEntity)

    @Query("DELETE FROM clocks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM clocks")
    suspend fun deleteAll()

    @Query("UPDATE clocks SET groupId = :groupId WHERE id = :id")
    suspend fun assignToGroup(id: Long, groupId: Long?)

    @Query("UPDATE clocks SET groupId = NULL WHERE groupId = :groupId")
    suspend fun unassignGroup(groupId: Long)

    @Query("SELECT * FROM clock_groups ORDER BY sortOrder ASC")
    fun observeGroups(): Flow<List<ClockGroupEntity>>

    @Query("SELECT * FROM clock_groups WHERE id = :id")
    suspend fun getGroupById(id: Long): ClockGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroup(group: ClockGroupEntity): Long

    @Update
    suspend fun updateGroup(group: ClockGroupEntity)

    @Query("DELETE FROM clock_groups WHERE id = :id")
    suspend fun deleteGroupById(id: Long)

    @Query("UPDATE clocks SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun setSortOrder(id: Long, sortOrder: Long)

    @Query("UPDATE clock_groups SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun setGroupSortOrder(id: Long, sortOrder: Long)
}

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour, minute, id")
    fun observeAll(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE enabled = 1")
    suspend fun getAllEnabled(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getById(id: Long): AlarmEntity?

    @Query("SELECT * FROM alarms WHERE clockId = :clockId ORDER BY hour, minute, id")
    suspend fun getByClockId(clockId: Long): List<AlarmEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(alarm: AlarmEntity): Long

    @Update
    suspend fun update(alarm: AlarmEntity)

    @Delete
    suspend fun delete(alarm: AlarmEntity)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM alarms")
    suspend fun deleteAll()

    @Query("UPDATE alarms SET groupId = :groupId WHERE id = :id")
    suspend fun assignToGroup(id: Long, groupId: Long?)

    @Query("UPDATE alarms SET clockId = NULL WHERE clockId = :clockId")
    suspend fun detachClock(clockId: Long)

    @Query("UPDATE alarms SET clockId = NULL")
    suspend fun detachAllClocks()

    @Query("UPDATE alarms SET groupId = NULL WHERE groupId = :groupId")
    suspend fun unassignGroup(groupId: Long)

    @Query("SELECT * FROM alarm_groups ORDER BY sortOrder ASC")
    fun observeGroups(): Flow<List<AlarmGroupEntity>>

    @Query("SELECT * FROM alarm_groups WHERE id = :id")
    suspend fun getGroupById(id: Long): AlarmGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroup(group: AlarmGroupEntity): Long

    @Update
    suspend fun updateGroup(group: AlarmGroupEntity)

    @Query("DELETE FROM alarm_groups WHERE id = :id")
    suspend fun deleteGroupById(id: Long)

    // Alarms have a synthetic ordering (hour, minute, id) in observeAll(); for explicit
    // reorder we'd need to add a sortOrder column. For now reorder of alarms is unsupported.

    @Query("UPDATE alarm_groups SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun setGroupSortOrder(id: Long, sortOrder: Long)
}

@Dao
interface TimerDao {
    @Query("SELECT * FROM timers ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<TimerEntity>>

    @Query("SELECT * FROM timers WHERE state = 'RUNNING'")
    suspend fun getAllRunning(): List<TimerEntity>

    @Query("SELECT * FROM timers WHERE id = :id")
    suspend fun getById(id: Long): TimerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(timer: TimerEntity): Long

    @Update
    suspend fun update(timer: TimerEntity)

    @Query("DELETE FROM timers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM timers")
    suspend fun deleteAll()

    @Query("UPDATE timers SET groupId = :groupId WHERE id = :id")
    suspend fun assignToGroup(id: Long, groupId: Long?)

    @Query("UPDATE timers SET groupId = NULL WHERE groupId = :groupId")
    suspend fun unassignGroup(groupId: Long)

    @Query("SELECT * FROM timer_groups ORDER BY sortOrder ASC")
    fun observeGroups(): Flow<List<TimerGroupEntity>>

    @Query("SELECT * FROM timer_groups WHERE id = :id")
    suspend fun getGroupById(id: Long): TimerGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroup(group: TimerGroupEntity): Long

    @Update
    suspend fun updateGroup(group: TimerGroupEntity)

    @Query("DELETE FROM timer_groups WHERE id = :id")
    suspend fun deleteGroupById(id: Long)

    @Query("UPDATE timers SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun setSortOrder(id: Long, sortOrder: Long)

    @Query("UPDATE timer_groups SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun setGroupSortOrder(id: Long, sortOrder: Long)
}

@Dao
interface StopwatchDao {
    @Query("SELECT * FROM stopwatches ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<StopwatchEntity>>

    @Query("SELECT * FROM stopwatches WHERE id = :id")
    suspend fun getById(id: Long): StopwatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stopwatch: StopwatchEntity): Long

    @Update
    suspend fun update(stopwatch: StopwatchEntity)

    @Query("DELETE FROM stopwatches WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM stopwatches")
    suspend fun deleteAll()

    @Query("DELETE FROM stopwatch_laps")
    suspend fun deleteAllLaps()

    @Query("SELECT * FROM stopwatch_laps WHERE stopwatchId = :stopwatchId ORDER BY lapNumber ASC")
    fun observeLaps(stopwatchId: Long): Flow<List<StopwatchLapEntity>>

    @Query("SELECT COALESCE(MAX(lapNumber), 0) FROM stopwatch_laps WHERE stopwatchId = :stopwatchId")
    suspend fun maxLapNumber(stopwatchId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLap(lap: StopwatchLapEntity)

    @Query("DELETE FROM stopwatch_laps WHERE stopwatchId = :stopwatchId")
    suspend fun deleteLaps(stopwatchId: Long)

    @Query("UPDATE stopwatches SET groupId = :groupId WHERE id = :id")
    suspend fun assignToGroup(id: Long, groupId: Long?)

    @Query("UPDATE stopwatches SET groupId = NULL WHERE groupId = :groupId")
    suspend fun unassignGroup(groupId: Long)

    @Query("SELECT * FROM stopwatch_groups ORDER BY sortOrder ASC")
    fun observeGroups(): Flow<List<StopwatchGroupEntity>>

    @Query("SELECT * FROM stopwatch_groups WHERE id = :id")
    suspend fun getGroupById(id: Long): StopwatchGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroup(group: StopwatchGroupEntity): Long

    @Update
    suspend fun updateGroup(group: StopwatchGroupEntity)

    @Query("DELETE FROM stopwatch_groups WHERE id = :id")
    suspend fun deleteGroupById(id: Long)

    @Query("UPDATE stopwatches SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun setSortOrder(id: Long, sortOrder: Long)

    @Query("UPDATE stopwatch_groups SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun setGroupSortOrder(id: Long, sortOrder: Long)
}
