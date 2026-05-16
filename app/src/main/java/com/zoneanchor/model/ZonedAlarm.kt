package com.zoneanchor.model

import java.time.ZoneId

data class ZonedAlarm(
    val id: Int = DEFAULT_ID,
    val label: String = "",
    val zoneId: String = DEFAULT_ZONE_ID,
    val hour: Int = DEFAULT_HOUR,
    val minute: Int = DEFAULT_MINUTE,
    val daysOfWeekMask: Int = ALL_DAYS,
    val soundMode: String = SOUND_DEFAULT,
    val vibrate: Boolean = true,
    val enabled: Boolean = false,
    val nextTriggerAtMillis: Long = 0L,
) {
    val displayName: String get() = label.ifBlank { zoneId }
    val normalizedDaysMask: Int get() = normalizeDays(daysOfWeekMask)

    fun enabledWithNextTrigger(nextTrigger: Long) = copy(
        label = cleanLabel(label),
        zoneId = validZoneId(zoneId),
        daysOfWeekMask = normalizedDaysMask,
        soundMode = validSoundMode(soundMode),
        enabled = true,
        nextTriggerAtMillis = nextTrigger,
    )

    fun disabled() = copy(enabled = false, nextTriggerAtMillis = 0L)

    companion object {
        const val FIRST_ID = 400
        const val DEFAULT_ID = FIRST_ID
        const val DEFAULT_ZONE_ID = "America/New_York"
        const val DEFAULT_HOUR = 16
        const val DEFAULT_MINUTE = 0
        const val ALL_DAYS = 0b1111111
        const val SOUND_DEFAULT = "default"
        const val SOUND_SILENT = "silent"

        fun cleanLabel(value: String?) = value?.trim().orEmpty()

        fun normalizeDays(mask: Int): Int {
            val normalized = mask and ALL_DAYS
            return if (normalized == 0) ALL_DAYS else normalized
        }

        fun validSoundMode(value: String?) =
            if (value == SOUND_SILENT) SOUND_SILENT else SOUND_DEFAULT

        fun validZoneId(value: String?): String {
            return try {
                ZoneId.of(value ?: DEFAULT_ZONE_ID).id
            } catch (_: Exception) {
                DEFAULT_ZONE_ID
            }
        }
    }
}
