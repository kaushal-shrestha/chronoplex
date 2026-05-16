package com.zoneanchor.alarm.data

import com.zoneanchor.alarm.data.db.ClockDao
import com.zoneanchor.alarm.data.db.ClockEntity
import com.zoneanchor.alarm.domain.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ClockRepository(private val dao: ClockDao) {
    fun observeAll(): Flow<List<Clock>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getAll(): List<Clock> = dao.getAll().map { it.toDomain() }

    suspend fun upsert(clock: Clock): Long {
        return if (clock.id == 0L) {
            dao.upsert(ClockEntity.fromDomain(clock))
        } else {
            dao.update(ClockEntity.fromDomain(clock))
            clock.id
        }
    }

    suspend fun delete(id: Long) = dao.deleteById(id)
}
