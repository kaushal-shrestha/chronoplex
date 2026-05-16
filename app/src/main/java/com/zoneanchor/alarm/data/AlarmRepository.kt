package com.zoneanchor.alarm.data

import com.zoneanchor.alarm.data.db.AlarmDao
import com.zoneanchor.alarm.data.db.AlarmEntity
import com.zoneanchor.alarm.domain.Alarm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlarmRepository(private val dao: AlarmDao) {
    fun observeAll(): Flow<List<Alarm>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getAllEnabled(): List<Alarm> = dao.getAllEnabled().map { it.toDomain() }

    suspend fun getById(id: Long): Alarm? = dao.getById(id)?.toDomain()

    suspend fun upsert(alarm: Alarm): Long {
        return if (alarm.id == 0L) {
            dao.upsert(AlarmEntity.fromDomain(alarm))
        } else {
            dao.update(AlarmEntity.fromDomain(alarm))
            alarm.id
        }
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) {
        val existing = dao.getById(id) ?: return
        dao.update(existing.copy(enabled = enabled))
    }

    suspend fun delete(id: Long) = dao.deleteById(id)
}
