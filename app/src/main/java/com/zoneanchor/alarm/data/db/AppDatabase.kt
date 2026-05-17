package com.zoneanchor.alarm.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ClockEntity::class, AlarmEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clockDao(): ClockDao
    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        // v1 → v2: adds AlarmEntity.snoozeUntilMillis for the active-snooze indicator.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN snoozeUntilMillis INTEGER")
            }
        }

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "zoneanchor.db",
            )
                .addMigrations(MIGRATION_1_2)
                .build().also { instance = it }
        }
    }
}
