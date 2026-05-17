package com.chronoplex.app.data

import com.chronoplex.app.data.db.AlarmDao
import com.chronoplex.app.data.db.AlarmEntity
import com.chronoplex.app.domain.Alarm
import com.chronoplex.app.domain.Validate
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

    /** Persist snooze state without re-routing through the rest of upsert's bookkeeping. */
    suspend fun setSnoozeUntil(id: Long, snoozeUntilMillis: Long?) {
        val existing = dao.getById(id) ?: return
        dao.update(existing.copy(snoozeUntilMillis = snoozeUntilMillis))
    }

    private fun Alarm.clean(): Alarm = copy(
        label = Validate.label(label),
        zoneId = Validate.zoneId(zoneId),
        hour = Validate.hour(hour),
        minute = Validate.minute(minute),
        daysMask = Validate.daysMask(daysMask),
    )
}
