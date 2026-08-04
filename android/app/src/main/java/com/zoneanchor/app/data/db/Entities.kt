package com.zoneanchor.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zoneanchor.app.domain.Alarm
import com.zoneanchor.app.domain.AlarmRepeatType
import com.zoneanchor.app.domain.Clock
import com.zoneanchor.app.domain.Group
import com.zoneanchor.app.domain.Stopwatch
import com.zoneanchor.app.domain.StopwatchLap
import com.zoneanchor.app.domain.StopwatchState
import com.zoneanchor.app.domain.Timer
import com.zoneanchor.app.domain.TimerFinishMode
import com.zoneanchor.app.domain.TimerState

@Entity(tableName = "clocks")
data class ClockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val zoneId: String,
    val sortOrder: Long,
    val groupId: Long? = null,
) {
    fun toDomain() = Clock(id = id, label = label, zoneId = zoneId, sortOrder = sortOrder, groupId = groupId)

    companion object {
        fun fromDomain(c: Clock) = ClockEntity(
            id = c.id,
            label = c.label,
            zoneId = c.zoneId,
            sortOrder = c.sortOrder,
            groupId = c.groupId,
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
    val groupId: Long? = null,
    val clockId: Long? = null,
    val repeatType: String = AlarmRepeatType.WEEKLY.name,
    val repeatInterval: Int = 1,
    val repeatStartDate: String = "",
    val monthlyDay: Int = 1,
    val monthlyOrdinal: Int = 1,
    val monthlyWeekday: Int = java.time.DayOfWeek.MONDAY.value,
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
        groupId = groupId,
        clockId = clockId,
        repeatType = runCatching { AlarmRepeatType.valueOf(repeatType) }
            .getOrElse { if (daysMask == 0) AlarmRepeatType.ONCE else AlarmRepeatType.WEEKLY },
        repeatInterval = repeatInterval,
        repeatStartDate = repeatStartDate,
        monthlyDay = monthlyDay,
        monthlyOrdinal = monthlyOrdinal,
        monthlyWeekday = monthlyWeekday,
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
            groupId = a.groupId,
            clockId = a.clockId,
            repeatType = a.effectiveRepeatType.name,
            repeatInterval = a.repeatInterval,
            repeatStartDate = a.repeatStartDate,
            monthlyDay = a.monthlyDay,
            monthlyOrdinal = a.monthlyOrdinal,
            monthlyWeekday = a.monthlyWeekday,
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
    val groupId: Long? = null,
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
        groupId = groupId,
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
            groupId = t.groupId,
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
    val groupId: Long? = null,
) {
    fun toDomain() = Stopwatch(
        id = id,
        label = label,
        state = runCatching { StopwatchState.valueOf(state) }.getOrElse { StopwatchState.IDLE },
        startedAtMillis = startedAtMillis,
        accumulatedMillis = accumulatedMillis,
        sortOrder = sortOrder,
        groupId = groupId,
    )

    companion object {
        fun fromDomain(s: Stopwatch) = StopwatchEntity(
            id = s.id,
            label = s.label,
            state = s.state.name,
            startedAtMillis = s.startedAtMillis,
            accumulatedMillis = s.accumulatedMillis,
            sortOrder = s.sortOrder,
            groupId = s.groupId,
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

// Per-entity-type group tables. All share the same shape but stay in separate
// tables so foreign keys and queries are unambiguous.
@Entity(tableName = "clock_groups")
data class ClockGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Long,
    val collapsed: Boolean,
) {
    fun toDomain() = Group(id, name, sortOrder, collapsed)
    companion object {
        fun fromDomain(g: Group) = ClockGroupEntity(g.id, g.name, g.sortOrder, g.collapsed)
    }
}

@Entity(tableName = "alarm_groups")
data class AlarmGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Long,
    val collapsed: Boolean,
) {
    fun toDomain() = Group(id, name, sortOrder, collapsed)
    companion object {
        fun fromDomain(g: Group) = AlarmGroupEntity(g.id, g.name, g.sortOrder, g.collapsed)
    }
}

@Entity(tableName = "timer_groups")
data class TimerGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Long,
    val collapsed: Boolean,
) {
    fun toDomain() = Group(id, name, sortOrder, collapsed)
    companion object {
        fun fromDomain(g: Group) = TimerGroupEntity(g.id, g.name, g.sortOrder, g.collapsed)
    }
}

@Entity(tableName = "stopwatch_groups")
data class StopwatchGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Long,
    val collapsed: Boolean,
) {
    fun toDomain() = Group(id, name, sortOrder, collapsed)
    companion object {
        fun fromDomain(g: Group) = StopwatchGroupEntity(g.id, g.name, g.sortOrder, g.collapsed)
    }
}
