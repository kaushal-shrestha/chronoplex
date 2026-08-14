package com.zoneanchor.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/** A user-defined collection that items of one entity type can belong to. */
data class Group(
    val id: Long = 0,
    val name: String,
    val sortOrder: Long = System.currentTimeMillis(),
    val collapsed: Boolean = false,
)

data class Clock(
    val id: Long = 0,
    val label: String,
    val zoneId: String,
    val sortOrder: Long = System.currentTimeMillis(),
    val groupId: Long? = null,
)

data class Alarm(
    val id: Long = 0,
    val label: String,
    val zoneId: String,
    val hour: Int,
    val minute: Int,
    val daysMask: Int,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val enabled: Boolean = true,
    /** When this alarm is currently snoozed, the epoch millis at which it will next ring. */
    val snoozeUntilMillis: Long? = null,
    val groupId: Long? = null,
    val clockId: Long? = null,
    val repeatType: AlarmRepeatType = AlarmRepeatType.WEEKLY,
    val repeatInterval: Int = 1,
    /** Local ISO date (yyyy-MM-dd) used as the interval anchor for advanced repeats. */
    val repeatStartDate: String = "",
    val monthlyDay: Int = 1,
    /** 1..4 for first through fourth, -1 for last. */
    val monthlyOrdinal: Int = 1,
    val monthlyWeekday: Int = DayOfWeek.MONDAY.value,
) {
    val daysOfWeek: Set<DayOfWeek> get() = DayMask.toDays(daysMask)
    val effectiveRepeatType: AlarmRepeatType get() =
        if (repeatType == AlarmRepeatType.WEEKLY && daysMask == 0) AlarmRepeatType.ONCE else repeatType
    val isOneShot: Boolean get() = effectiveRepeatType == AlarmRepeatType.ONCE
    fun isSnoozed(nowMillis: Long = System.currentTimeMillis()): Boolean =
        (snoozeUntilMillis ?: 0L) > nowMillis
}

enum class AlarmRepeatType { ONCE, WEEKLY, MONTHLY_DAY, MONTHLY_WEEKDAY }

enum class AppearanceMode { SYSTEM, LIGHT, DARK }

enum class TimerState { IDLE, RUNNING, PAUSED, FINISHED }

enum class TimerFinishMode { NOTIFICATION, FULL_SCREEN }

data class Timer(
    val id: Long = 0,
    val label: String = "",
    val durationMillis: Long,
    val state: TimerState = TimerState.IDLE,
    /** When [state] is RUNNING, the absolute epoch-millis at which this timer fires. */
    val endsAtMillis: Long? = null,
    /** When [state] is PAUSED, the millis remaining at the moment of pause. */
    val pausedRemainingMillis: Long? = null,
    val finishMode: TimerFinishMode = TimerFinishMode.NOTIFICATION,
    val sortOrder: Long = System.currentTimeMillis(),
    val groupId: Long? = null,
) {
    /** Remaining millis at [nowMillis] given the current state. */
    fun remainingMillis(nowMillis: Long = System.currentTimeMillis()): Long = when (state) {
        TimerState.IDLE -> durationMillis
        TimerState.RUNNING -> ((endsAtMillis ?: nowMillis) - nowMillis).coerceAtLeast(0L)
        TimerState.PAUSED -> pausedRemainingMillis ?: durationMillis
        TimerState.FINISHED -> 0L
    }

    /** Apply editable fields without disturbing runtime state such as running countdowns. */
    fun withEditableFields(label: String, durationMillis: Long, groupId: Long?): Timer = copy(
        label = label,
        durationMillis = durationMillis,
        groupId = groupId,
    )
}

enum class StopwatchState { IDLE, RUNNING, PAUSED }

data class Stopwatch(
    val id: Long = 0,
    val label: String = "",
    val state: StopwatchState = StopwatchState.IDLE,
    /** When RUNNING, the absolute epoch-millis when the current segment started. */
    val startedAtMillis: Long? = null,
    /** Sum of completed segments before the current one. */
    val accumulatedMillis: Long = 0L,
    val sortOrder: Long = System.currentTimeMillis(),
    val groupId: Long? = null,
) {
    /** Total elapsed millis at [nowMillis]. */
    fun elapsedMillis(nowMillis: Long = System.currentTimeMillis()): Long = when (state) {
        StopwatchState.IDLE -> 0L
        StopwatchState.PAUSED -> accumulatedMillis
        StopwatchState.RUNNING -> accumulatedMillis +
            ((startedAtMillis?.let { nowMillis - it } ?: 0L).coerceAtLeast(0L))
    }
}

data class StopwatchLap(
    val stopwatchId: Long,
    val lapNumber: Int,
    val totalElapsedMillis: Long,
)

enum class ThemePalette(val displayName: String, val description: String) {
    Anchor("Anchor", "Material You on Android 12+, deep navy fallback elsewhere"),
    Daybreak("Daybreak", "Clean paper, deep ink, and calm teal"),
    Harbor("Harbor", "Soft blue-gray with a crisp marine accent"),
    Grove("Grove", "A green workspace with warm ivory surfaces"),
    Ember("Ember", "Warm rose accents on a quiet neutral base"),
    Twilight("Twilight", "Cool slate tones with a bright evening accent"),
    Sunrise("Sunrise", "Warm orange tones for early risers"),
    Forest("Forest", "Deep evergreen with a calming spread"),
    Slate("Slate", "Cool grey neutrals for focus"),
    Plum("Plum", "Deep purple accents on light"),
}

object DayMask {
    const val ALL_BITS = 0b1111111

    fun toMask(days: Set<DayOfWeek>): Int =
        days.fold(0) { acc, d -> acc or (1 shl (d.value - 1)) }

    fun toDays(mask: Int): Set<DayOfWeek> =
        DayOfWeek.values().filter { (mask shr (it.value - 1)) and 1 == 1 }.toSet()

    fun contains(mask: Int, day: DayOfWeek): Boolean =
        (mask shr (day.value - 1)) and 1 == 1

    /** Strip any garbage high bits without collapsing mask==0 (which represents one-shot in our model). */
    fun sanitize(mask: Int): Int = mask and ALL_BITS

    val WEEKDAYS = toMask(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY))
    val WEEKENDS = toMask(setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
    val EVERY_DAY = toMask(DayOfWeek.values().toSet())
}

/**
 * Normalize untrusted persisted/input fields before they reach the rest of the app.
 * Applied at the repository boundary so a corrupt row can never poison the UI.
 */
object Validate {
    private const val MAX_LABEL_LEN = 80

    fun label(s: String?): String = s?.trim().orEmpty().take(MAX_LABEL_LEN)

    fun zoneId(s: String?): String =
        runCatching { ZoneId.of(s ?: ZoneId.systemDefault().id).id }
            .getOrElse { ZoneId.systemDefault().id }

    fun hour(h: Int): Int = h.coerceIn(0, 23)
    fun minute(m: Int): Int = m.coerceIn(0, 59)
    fun daysMask(mask: Int): Int = DayMask.sanitize(mask)
    fun repeatType(type: AlarmRepeatType?, daysMask: Int): AlarmRepeatType =
        type ?: if (DayMask.sanitize(daysMask) == 0) AlarmRepeatType.ONCE else AlarmRepeatType.WEEKLY
    fun repeatInterval(interval: Int): Int = interval.coerceIn(1, 99)
    fun repeatStartDate(value: String?): String =
        value?.takeIf { runCatching { LocalDate.parse(it) }.isSuccess }.orEmpty()
    fun monthlyDay(day: Int): Int = day.coerceIn(1, 31)
    fun monthlyOrdinal(ordinal: Int): Int = if (ordinal == -1) -1 else ordinal.coerceIn(1, 4)
    fun monthlyWeekday(value: Int): Int = value.coerceIn(1, 7)
}
