package com.chronoplex.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ClockEntity::class,
        AlarmEntity::class,
        TimerEntity::class,
        StopwatchEntity::class,
        StopwatchLapEntity::class,
        ClockGroupEntity::class,
        AlarmGroupEntity::class,
        TimerGroupEntity::class,
        StopwatchGroupEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clockDao(): ClockDao
    abstract fun alarmDao(): AlarmDao
    abstract fun timerDao(): TimerDao
    abstract fun stopwatchDao(): StopwatchDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        // v1 → v2: AlarmEntity.snoozeUntilMillis for the active-snooze indicator.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN snoozeUntilMillis INTEGER")
            }
        }

        // v2 → v3: introduces the timers table.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS timers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        label TEXT NOT NULL,
                        durationMillis INTEGER NOT NULL,
                        state TEXT NOT NULL,
                        endsAtMillis INTEGER,
                        pausedRemainingMillis INTEGER,
                        finishMode TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        // v3 → v4: introduces stopwatches + stopwatch_laps.
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS stopwatches (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        label TEXT NOT NULL,
                        state TEXT NOT NULL,
                        startedAtMillis INTEGER,
                        accumulatedMillis INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS stopwatch_laps (
                        stopwatchId INTEGER NOT NULL,
                        lapNumber INTEGER NOT NULL,
                        totalElapsedMillis INTEGER NOT NULL,
                        PRIMARY KEY(stopwatchId, lapNumber)
                    )
                    """.trimIndent()
                )
            }
        }

        // v4 → v5: adds groupId to each entity table + four per-type group tables.
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE clocks ADD COLUMN groupId INTEGER")
                db.execSQL("ALTER TABLE alarms ADD COLUMN groupId INTEGER")
                db.execSQL("ALTER TABLE timers ADD COLUMN groupId INTEGER")
                db.execSQL("ALTER TABLE stopwatches ADD COLUMN groupId INTEGER")
                listOf("clock_groups", "alarm_groups", "timer_groups", "stopwatch_groups").forEach { table ->
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS $table (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            name TEXT NOT NULL,
                            sortOrder INTEGER NOT NULL,
                            collapsed INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                }
            }
        }

        // v5 → v6: adds richer alarm recurrence while preserving existing masks.
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN repeatType TEXT NOT NULL DEFAULT 'WEEKLY'")
                db.execSQL("ALTER TABLE alarms ADD COLUMN repeatInterval INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE alarms ADD COLUMN repeatStartDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE alarms ADD COLUMN monthlyDay INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE alarms ADD COLUMN monthlyOrdinal INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE alarms ADD COLUMN monthlyWeekday INTEGER NOT NULL DEFAULT 1")
                db.execSQL("UPDATE alarms SET repeatType = 'ONCE' WHERE daysMask = 0")
            }
        }

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "chronoplex.db",
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build().also { instance = it }
        }
    }
}
