package com.zoneanchor.data.clocks

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clocks")
data class ClockEntity(
    @PrimaryKey val zoneId: String,
    val label: String,
    val position: Int,
)
