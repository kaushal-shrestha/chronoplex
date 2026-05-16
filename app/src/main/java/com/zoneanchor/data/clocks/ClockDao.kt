package com.zoneanchor.data.clocks

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClockDao {
    @Query("SELECT * FROM clocks ORDER BY position, zoneId")
    fun observeAll(): Flow<List<ClockEntity>>

    @Query("SELECT * FROM clocks ORDER BY position, zoneId")
    suspend fun getAll(): List<ClockEntity>

    @Query("SELECT * FROM clocks WHERE zoneId = :zoneId")
    suspend fun findByZone(zoneId: String): ClockEntity?

    @Query("SELECT COUNT(*) FROM clocks")
    suspend fun count(): Int

    @Query("SELECT COALESCE(MAX(position), -1) FROM clocks")
    suspend fun maxPosition(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(clock: ClockEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(clocks: List<ClockEntity>)

    @Query("DELETE FROM clocks WHERE zoneId = :zoneId")
    suspend fun deleteByZone(zoneId: String)
}
