package com.zoneanchor

import android.app.Application
import androidx.room.Room
import com.zoneanchor.alarm.AlarmScheduler
import com.zoneanchor.alarm.NotificationHelper
import com.zoneanchor.data.AppDatabase
import com.zoneanchor.data.alarms.AlarmRepository
import com.zoneanchor.data.clocks.ClockRepository
import com.zoneanchor.data.migration.LegacyPrefsMigration
import com.zoneanchor.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ZoneAnchorApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase by lazy {
        Room.databaseBuilder(applicationContext, AppDatabase::class.java, "zoneanchor.db").build()
    }
    val alarmRepository: AlarmRepository by lazy { AlarmRepository(database.alarmDao()) }
    val clockRepository: ClockRepository by lazy { ClockRepository(database.clockDao()) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(applicationContext) }
    val alarmScheduler: AlarmScheduler by lazy { AlarmScheduler(applicationContext, alarmRepository) }
    val legacyPrefsMigration: LegacyPrefsMigration by lazy {
        LegacyPrefsMigration(applicationContext, database, settingsRepository)
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)
        applicationScope.launch(Dispatchers.IO) {
            legacyPrefsMigration.runIfNeeded()
            clockRepository.seedDefaultsIfEmpty()
            alarmScheduler.rescheduleEnabledAlarms()
        }
    }
}
