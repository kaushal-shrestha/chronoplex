package com.chronoplex.app.data

import com.chronoplex.app.data.db.ClockDao
import com.chronoplex.app.data.db.ClockEntity
import com.chronoplex.app.domain.Clock
import com.chronoplex.app.domain.Validate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ClockRepository(private val dao: ClockDao) {
    fun observeAll(): Flow<List<Clock>> =
        dao.observeAll().map { list -> list.map { it.toDomain().clean() } }

    suspend fun getAll(): List<Clock> = dao.getAll().map { it.toDomain().clean() }

    suspend fun upsert(clock: Clock): Long {
        val cleaned = clock.clean()
        return if (cleaned.id == 0L) {
            dao.upsert(ClockEntity.fromDomain(cleaned))
        } else {
            dao.update(ClockEntity.fromDomain(cleaned))
            cleaned.id
        }
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    suspend fun deleteAll() = dao.deleteAll()

    private fun Clock.clean(): Clock = copy(
        label = Validate.label(label),
        zoneId = Validate.zoneId(zoneId),
    )
}
