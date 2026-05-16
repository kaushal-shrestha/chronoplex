package com.zoneanchor.alarm.domain

import java.time.DayOfWeek

data class Clock(
    val id: Long = 0,
    val label: String,
    val zoneId: String,
    val sortOrder: Long = System.currentTimeMillis(),
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
) {
    val daysOfWeek: Set<DayOfWeek> get() = DayMask.toDays(daysMask)
    val isOneShot: Boolean get() = daysMask == 0
}

enum class AppearanceMode { SYSTEM, LIGHT, DARK }

enum class ThemePalette(val displayName: String) {
    Anchor("Anchor (default)"),
    Sunrise("Sunrise"),
    Forest("Forest"),
    Slate("Slate"),
    Plum("Plum"),
}

object DayMask {
    fun toMask(days: Set<DayOfWeek>): Int =
        days.fold(0) { acc, d -> acc or (1 shl (d.value - 1)) }

    fun toDays(mask: Int): Set<DayOfWeek> =
        DayOfWeek.values().filter { (mask shr (it.value - 1)) and 1 == 1 }.toSet()

    fun contains(mask: Int, day: DayOfWeek): Boolean =
        (mask shr (day.value - 1)) and 1 == 1

    val WEEKDAYS = toMask(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY))
    val WEEKENDS = toMask(setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
    val EVERY_DAY = toMask(DayOfWeek.values().toSet())
}
