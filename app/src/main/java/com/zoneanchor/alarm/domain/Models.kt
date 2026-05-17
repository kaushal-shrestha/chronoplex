package com.zoneanchor.alarm.domain

import java.time.DayOfWeek
import java.time.ZoneId

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
}
