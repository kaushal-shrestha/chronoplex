package com.zoneanchor.alarm.data

import com.zoneanchor.alarm.data.db.AlarmDao
import com.zoneanchor.alarm.data.db.AlarmEntity
import com.zoneanchor.alarm.domain.Alarm
import com.zoneanchor.alarm.domain.Validate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlarmRepository(private val dao: AlarmDao) {
    fun observeAll(): Flow<List<Alarm>> =
        dao.observeAll().map { list -> list.map { it.toDomain().clean() } }

    suspend fun getAllEnabled(): List<Alarm> = dao.getAllEnabled().map { it.toDomain().clean() }

    suspend fun getById(id: Long): Alarm? = dao.getById(id)?.toDomain()?.clean()

    suspend fun upsert(alarm: Alarm): Long {
        val cleaned = alarm.clean()
        return if (cleaned.id == 0L) {
            dao.upsert(AlarmEntity.fromDomain(cleaned))
        } else {
            dao.update(AlarmEntity.fromDomain(cleaned))
            cleaned.id
        }
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) {
        val existing = dao.getById(id) ?: return
        dao.update(existing.copy(enabled = enabled))
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    private fun Alarm.clean(): Alarm = copy(
        label = Validate.label(label),
        zoneId = Validate.zoneId(zoneId),
        hour = Validate.hour(hour),
        minute = Validate.minute(minute),
        daysMask = Validate.daysMask(daysMask),
    )
}
