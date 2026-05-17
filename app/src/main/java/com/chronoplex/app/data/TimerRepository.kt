package com.chronoplex.app.data

import com.chronoplex.app.data.db.TimerDao
import com.chronoplex.app.data.db.TimerEntity
import com.chronoplex.app.domain.Timer
import com.chronoplex.app.domain.TimerState
import com.chronoplex.app.domain.Validate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TimerRepository(private val dao: TimerDao) {
    fun observeAll(): Flow<List<Timer>> =
        dao.observeAll().map { list -> list.map { it.toDomain().clean() } }

    suspend fun getAllRunning(): List<Timer> = dao.getAllRunning().map { it.toDomain().clean() }

    suspend fun getById(id: Long): Timer? = dao.getById(id)?.toDomain()?.clean()

    suspend fun upsert(timer: Timer): Long {
        val cleaned = timer.clean()
        return if (cleaned.id == 0L) {
            dao.upsert(TimerEntity.fromDomain(cleaned))
        } else {
            dao.update(TimerEntity.fromDomain(cleaned))
            cleaned.id
        }
    }

    suspend fun updateState(
        id: Long,
        state: TimerState,
        endsAtMillis: Long?,
        pausedRemainingMillis: Long?,
    ) {
        val existing = dao.getById(id) ?: return
        dao.update(
            existing.copy(
                state = state.name,
                endsAtMillis = endsAtMillis,
                pausedRemainingMillis = pausedRemainingMillis,
            )
        )
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    private fun Timer.clean(): Timer = copy(
        label = Validate.label(label),
        durationMillis = durationMillis.coerceAtLeast(0L),
    )
}
