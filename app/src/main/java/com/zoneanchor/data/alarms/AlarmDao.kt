package com.zoneanchor.data.alarms

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour, minute, id")
    fun observeAll(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms ORDER BY hour, minute, id")
    suspend fun getAll(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE enabled = 1 ORDER BY id")
    suspend fun getEnabled(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun findById(id: Int): AlarmEntity?

    @Query("SELECT COUNT(*) FROM alarms")
    suspend fun count(): Int

    @Query("SELECT MAX(id) FROM alarms")
    suspend fun maxId(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(alarm: AlarmEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(alarms: List<AlarmEntity>)

    @Delete
    suspend fun delete(alarm: AlarmEntity)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteById(id: Int)
}
