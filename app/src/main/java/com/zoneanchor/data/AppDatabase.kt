package com.zoneanchor.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.zoneanchor.data.alarms.AlarmDao
import com.zoneanchor.data.alarms.AlarmEntity
import com.zoneanchor.data.clocks.ClockDao
import com.zoneanchor.data.clocks.ClockEntity

@Database(
    entities = [AlarmEntity::class, ClockEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun clockDao(): ClockDao
}
