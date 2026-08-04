package com.zoneanchor.app.domain

import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek
import org.junit.Test

class DomainModelsTest {

    @Test fun `day mask converts days both ways and ignores garbage bits`() {
        val mask = DayMask.toMask(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY))

        assertThat(mask).isEqualTo(0b1010001)
        assertThat(DayMask.toDays(mask)).containsExactly(
            DayOfWeek.MONDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SUNDAY,
        )
        assertThat(DayMask.contains(mask, DayOfWeek.FRIDAY)).isTrue()
        assertThat(DayMask.contains(mask, DayOfWeek.SATURDAY)).isFalse()
        assertThat(DayMask.sanitize(mask or (1 shl 12))).isEqualTo(mask)
        assertThat(DayMask.WEEKDAYS).isEqualTo(
            DayMask.toMask(
                setOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY,
                )
            )
        )
        assertThat(DayMask.WEEKENDS).isEqualTo(DayMask.toMask(setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)))
        assertThat(DayMask.EVERY_DAY).isEqualTo(DayMask.toMask(DayOfWeek.values().toSet()))
    }

    @Test fun `alarm derived repeat and snooze properties reflect anchored state`() {
        val oneShot = Alarm(
            id = 1,
            label = "Flight check-in",
            zoneId = "America/New_York",
            hour = 16,
            minute = 0,
            daysMask = 0,
            repeatType = AlarmRepeatType.WEEKLY,
            snoozeUntilMillis = 2_000L,
        )
        val weekly = oneShot.copy(
            daysMask = DayMask.toMask(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)),
            repeatType = AlarmRepeatType.WEEKLY,
            snoozeUntilMillis = null,
        )

        assertThat(oneShot.effectiveRepeatType).isEqualTo(AlarmRepeatType.ONCE)
        assertThat(oneShot.isOneShot).isTrue()
        assertThat(oneShot.daysOfWeek).isEmpty()
        assertThat(oneShot.isSnoozed(nowMillis = 1_999L)).isTrue()
        assertThat(oneShot.isSnoozed(nowMillis = 2_000L)).isFalse()
        assertThat(oneShot.copy(snoozeUntilMillis = Long.MAX_VALUE).isSnoozed()).isTrue()

        assertThat(weekly.effectiveRepeatType).isEqualTo(AlarmRepeatType.WEEKLY)
        assertThat(weekly.isOneShot).isFalse()
        assertThat(weekly.daysOfWeek).containsExactly(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
        assertThat(weekly.isSnoozed(nowMillis = 1L)).isFalse()
    }

    @Test fun `timer remaining time is derived from state`() {
        assertThat(Timer(label = "Idle", durationMillis = 90_000L).remainingMillis(nowMillis = 10_000L))
            .isEqualTo(90_000L)
        assertThat(
            Timer(
                label = "Running",
                durationMillis = 90_000L,
                state = TimerState.RUNNING,
                endsAtMillis = 40_000L,
            ).remainingMillis(nowMillis = 10_000L)
        ).isEqualTo(30_000L)
        assertThat(
            Timer(
                label = "Overdue",
                durationMillis = 90_000L,
                state = TimerState.RUNNING,
                endsAtMillis = 5_000L,
            ).remainingMillis(nowMillis = 10_000L)
        ).isEqualTo(0L)
        assertThat(
            Timer(
                label = "Missing end",
                durationMillis = 90_000L,
                state = TimerState.RUNNING,
                endsAtMillis = null,
            ).remainingMillis(nowMillis = 10_000L)
        ).isEqualTo(0L)
        assertThat(
            Timer(
                label = "Paused",
                durationMillis = 90_000L,
                state = TimerState.PAUSED,
                pausedRemainingMillis = 12_345L,
            ).remainingMillis(nowMillis = 10_000L)
        ).isEqualTo(12_345L)
        assertThat(
            Timer(
                label = "Paused fallback",
                durationMillis = 90_000L,
                state = TimerState.PAUSED,
                pausedRemainingMillis = null,
            ).remainingMillis(nowMillis = 10_000L)
        ).isEqualTo(90_000L)
        assertThat(
            Timer(
                label = "Done",
                durationMillis = 90_000L,
                state = TimerState.FINISHED,
            ).remainingMillis(nowMillis = 10_000L)
        ).isEqualTo(0L)
    }

    @Test fun `stopwatch elapsed time is derived from state`() {
        assertThat(Stopwatch(label = "Idle").elapsedMillis(nowMillis = 10_000L)).isEqualTo(0L)
        assertThat(
            Stopwatch(
                label = "Paused",
                state = StopwatchState.PAUSED,
                accumulatedMillis = 12_000L,
            ).elapsedMillis(nowMillis = 20_000L)
        ).isEqualTo(12_000L)
        assertThat(
            Stopwatch(
                label = "Running",
                state = StopwatchState.RUNNING,
                startedAtMillis = 10_000L,
                accumulatedMillis = 5_000L,
            ).elapsedMillis(nowMillis = 20_000L)
        ).isEqualTo(15_000L)
        assertThat(
            Stopwatch(
                label = "Future start",
                state = StopwatchState.RUNNING,
                startedAtMillis = 30_000L,
                accumulatedMillis = 5_000L,
            ).elapsedMillis(nowMillis = 20_000L)
        ).isEqualTo(5_000L)
        assertThat(
            Stopwatch(
                label = "Missing start",
                state = StopwatchState.RUNNING,
                startedAtMillis = null,
                accumulatedMillis = 5_000L,
            ).elapsedMillis(nowMillis = 20_000L)
        ).isEqualTo(5_000L)
    }

    @Test fun `theme palettes expose stable display metadata`() {
        assertThat(Group(name = "Travel").name).isEqualTo("Travel")
        assertThat(Group(name = "Travel").collapsed).isFalse()
        assertThat(AppearanceMode.values()).asList().containsExactly(
            AppearanceMode.SYSTEM,
            AppearanceMode.LIGHT,
            AppearanceMode.DARK,
        ).inOrder()
        assertThat(ThemePalette.values().map { it.displayName }).containsExactly(
            "Anchor",
            "Daybreak",
            "Harbor",
            "Grove",
            "Ember",
            "Twilight",
            "Sunrise",
            "Forest",
            "Slate",
            "Plum",
        ).inOrder()
        assertThat(ThemePalette.Anchor.description).contains("Material You")
    }
}
