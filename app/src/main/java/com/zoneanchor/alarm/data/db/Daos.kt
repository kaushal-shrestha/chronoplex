package com.zoneanchor.alarm.data.db

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
}

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour, minute, id")
    fun observeAll(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE enabled = 1")
    suspend fun getAllEnabled(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getById(id: Long): AlarmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(alarm: AlarmEntity): Long

    @Update
    suspend fun update(alarm: AlarmEntity)

    @Delete
    suspend fun delete(alarm: AlarmEntity)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteById(id: Long)
}
