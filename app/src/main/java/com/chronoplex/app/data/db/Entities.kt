package com.chronoplex.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chronoplex.app.domain.Alarm
import com.chronoplex.app.domain.Clock
import com.chronoplex.app.domain.Stopwatch
import com.chronoplex.app.domain.StopwatchLap
import com.chronoplex.app.domain.StopwatchState
import com.chronoplex.app.domain.Timer
import com.chronoplex.app.domain.TimerFinishMode
import com.chronoplex.app.domain.TimerState

@Entity(tableName = "clocks")
data class ClockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val zoneId: String,
    val sortOrder: Long,
) {
    fun toDomain() = Clock(id = id, label = label, zoneId = zoneId, sortOrder = sortOrder)

    companion object {
        fun fromDomain(c: Clock) = ClockEntity(
            id = c.id,
            label = c.label,
            zoneId = c.zoneId,
            sortOrder = c.sortOrder,
        )
    }
}

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val zoneId: String,
    val hour: Int,
    val minute: Int,
    val daysMask: Int,
    val soundEnabled: Boolean,
    val vibrationEnabled: Boolean,
    val enabled: Boolean,
    val snoozeUntilMillis: Long? = null,
) {
    fun toDomain() = Alarm(
        id = id,
        label = label,
        zoneId = zoneId,
        hour = hour,
        minute = minute,
        daysMask = daysMask,
        soundEnabled = soundEnabled,
        vibrationEnabled = vibrationEnabled,
        enabled = enabled,
        snoozeUntilMillis = snoozeUntilMillis,
    )

    companion object {
        fun fromDomain(a: Alarm) = AlarmEntity(
            id = a.id,
            label = a.label,
            zoneId = a.zoneId,
            hour = a.hour,
            minute = a.minute,
            daysMask = a.daysMask,
            soundEnabled = a.soundEnabled,
            vibrationEnabled = a.vibrationEnabled,
            enabled = a.enabled,
            snoozeUntilMillis = a.snoozeUntilMillis,
        )
    }
}

@Entity(tableName = "timers")
data class TimerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val durationMillis: Long,
    val state: String,
    val endsAtMillis: Long?,
    val pausedRemainingMillis: Long?,
    val finishMode: String,
    val sortOrder: Long,
) {
    fun toDomain() = Timer(
        id = id,
        label = label,
        durationMillis = durationMillis,
        state = runCatching { TimerState.valueOf(state) }.getOrElse { TimerState.IDLE },
        endsAtMillis = endsAtMillis,
        pausedRemainingMillis = pausedRemainingMillis,
        finishMode = runCatching { TimerFinishMode.valueOf(finishMode) }.getOrElse { TimerFinishMode.NOTIFICATION },
        sortOrder = sortOrder,
    )

    companion object {
        fun fromDomain(t: Timer) = TimerEntity(
            id = t.id,
            label = t.label,
            durationMillis = t.durationMillis,
            state = t.state.name,
            endsAtMillis = t.endsAtMillis,
            pausedRemainingMillis = t.pausedRemainingMillis,
            finishMode = t.finishMode.name,
            sortOrder = t.sortOrder,
        )
    }
}

@Entity(tableName = "stopwatches")
data class StopwatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val state: String,
    val startedAtMillis: Long?,
    val accumulatedMillis: Long,
    val sortOrder: Long,
) {
    fun toDomain() = Stopwatch(
        id = id,
        label = label,
        state = runCatching { StopwatchState.valueOf(state) }.getOrElse { StopwatchState.IDLE },
        startedAtMillis = startedAtMillis,
        accumulatedMillis = accumulatedMillis,
        sortOrder = sortOrder,
    )

    companion object {
        fun fromDomain(s: Stopwatch) = StopwatchEntity(
            id = s.id,
            label = s.label,
            state = s.state.name,
            startedAtMillis = s.startedAtMillis,
            accumulatedMillis = s.accumulatedMillis,
            sortOrder = s.sortOrder,
        )
    }
}

@Entity(tableName = "stopwatch_laps", primaryKeys = ["stopwatchId", "lapNumber"])
data class StopwatchLapEntity(
    val stopwatchId: Long,
    val lapNumber: Int,
    val totalElapsedMillis: Long,
) {
    fun toDomain() = StopwatchLap(stopwatchId, lapNumber, totalElapsedMillis)

    companion object {
        fun fromDomain(l: StopwatchLap) = StopwatchLapEntity(l.stopwatchId, l.lapNumber, l.totalElapsedMillis)
    }
}
