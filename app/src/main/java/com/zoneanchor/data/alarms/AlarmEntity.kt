package com.zoneanchor.data.alarms

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey val id: Int,
    val label: String,
    val zoneId: String,
    val hour: Int,
    val minute: Int,
    val daysOfWeekMask: Int,
    val soundMode: String,
    val vibrate: Boolean,
    val enabled: Boolean,
    val nextTriggerAtMillis: Long,
)
