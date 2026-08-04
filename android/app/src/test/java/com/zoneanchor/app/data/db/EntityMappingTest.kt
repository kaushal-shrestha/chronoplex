package com.zoneanchor.app.data.db

import com.google.common.truth.Truth.assertThat
import com.zoneanchor.app.domain.Alarm
import com.zoneanchor.app.domain.AlarmRepeatType
import com.zoneanchor.app.domain.Clock
import com.zoneanchor.app.domain.DayMask
import com.zoneanchor.app.domain.Group
import com.zoneanchor.app.domain.Stopwatch
import com.zoneanchor.app.domain.StopwatchLap
import com.zoneanchor.app.domain.StopwatchState
import com.zoneanchor.app.domain.Timer
import com.zoneanchor.app.domain.TimerFinishMode
import com.zoneanchor.app.domain.TimerState
import org.junit.Test

class EntityMappingTest {

    @Test fun `clock entity round trips domain fields`() {
        val clock = Clock(id = 7, label = "Kathmandu", zoneId = "Asia/Kathmandu", sortOrder = 4, groupId = 2)
        val entity = ClockEntity.fromDomain(clock)
        val defaulted = ClockEntity(label = "UTC", zoneId = "UTC", sortOrder = 1)

        assertThat(entity).isEqualTo(
            ClockEntity(id = 7, label = "Kathmandu", zoneId = "Asia/Kathmandu", sortOrder = 4, groupId = 2)
        )
        assertThat(entity.toDomain()).isEqualTo(clock)
        assertThat(defaulted.id).isEqualTo(0)
        assertThat(defaulted.groupId).isNull()
    }

    @Test fun `alarm entity stores effective repeat and recovers invalid repeat names`() {
        val weeklyWithNoDays = Alarm(
            id = 3,
            label = "Check in",
            zoneId = "America/New_York",
            hour = 16,
            minute = 0,
            daysMask = 0,
            soundEnabled = false,
            vibrationEnabled = true,
            enabled = true,
            repeatType = AlarmRepeatType.WEEKLY,
            repeatInterval = 2,
            repeatStartDate = "2026-08-03",
            monthlyDay = 31,
            monthlyOrdinal = -1,
            monthlyWeekday = 5,
            groupId = 11,
            clockId = 12,
            snoozeUntilMillis = 9_000L,
        )

        val entity = AlarmEntity.fromDomain(weeklyWithNoDays)

        assertThat(entity.repeatType).isEqualTo(AlarmRepeatType.ONCE.name)
        assertThat(entity.toDomain()).isEqualTo(weeklyWithNoDays.copy(repeatType = AlarmRepeatType.ONCE))

        assertThat(baseAlarmEntity(daysMask = 0, repeatType = "NOT_REAL").toDomain().repeatType)
            .isEqualTo(AlarmRepeatType.ONCE)
        assertThat(baseAlarmEntity(daysMask = DayMask.WEEKENDS, repeatType = "NOT_REAL").toDomain().repeatType)
            .isEqualTo(AlarmRepeatType.WEEKLY)
    }

    @Test fun `timer entity round trips and defaults corrupt enum strings`() {
        val timer = Timer(
            id = 4,
            label = "Tea",
            durationMillis = 180_000L,
            state = TimerState.RUNNING,
            endsAtMillis = 300_000L,
            pausedRemainingMillis = null,
            finishMode = TimerFinishMode.FULL_SCREEN,
            sortOrder = 8,
            groupId = 2,
        )
        val entity = TimerEntity.fromDomain(timer)

        assertThat(entity.toDomain()).isEqualTo(timer)
        assertThat(entity.finishMode).isEqualTo(TimerFinishMode.FULL_SCREEN.name)

        val corrupt = entity.copy(state = "BROKEN", finishMode = "LOUD")

        assertThat(corrupt.toDomain().state).isEqualTo(TimerState.IDLE)
        assertThat(corrupt.toDomain().finishMode).isEqualTo(TimerFinishMode.NOTIFICATION)
    }

    @Test fun `stopwatch and lap entities round trip and default corrupt state`() {
        val stopwatch = Stopwatch(
            id = 5,
            label = "Run",
            state = StopwatchState.RUNNING,
            startedAtMillis = 1_000L,
            accumulatedMillis = 2_000L,
            sortOrder = 3,
            groupId = 9,
        )
        val lap = StopwatchLap(stopwatchId = 5, lapNumber = 2, totalElapsedMillis = 30_000L)

        assertThat(StopwatchEntity.fromDomain(stopwatch).toDomain()).isEqualTo(stopwatch)
        assertThat(StopwatchEntity.fromDomain(stopwatch).copy(state = "WHAT").toDomain().state)
            .isEqualTo(StopwatchState.IDLE)
        assertThat(StopwatchLapEntity.fromDomain(lap).toDomain()).isEqualTo(lap)
    }

    @Test fun `group entities round trip shared group domain shape`() {
        val group = Group(id = 8, name = "Travel", sortOrder = 2, collapsed = true)

        assertThat(ClockGroupEntity.fromDomain(group).toDomain()).isEqualTo(group)
        assertThat(AlarmGroupEntity.fromDomain(group).toDomain()).isEqualTo(group)
        assertThat(TimerGroupEntity.fromDomain(group).toDomain()).isEqualTo(group)
        assertThat(StopwatchGroupEntity.fromDomain(group).toDomain()).isEqualTo(group)
    }

    private fun baseAlarmEntity(daysMask: Int, repeatType: String) = AlarmEntity(
        id = 1,
        label = "Alarm",
        zoneId = "UTC",
        hour = 9,
        minute = 30,
        daysMask = daysMask,
        soundEnabled = true,
        vibrationEnabled = true,
        enabled = true,
        repeatType = repeatType,
    )
}
