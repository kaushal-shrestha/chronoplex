package com.chronoplex.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chronoplex.app.domain.Alarm
import com.chronoplex.app.domain.Clock

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
