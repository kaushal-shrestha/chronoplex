package com.zoneanchor.app

import android.app.Application
import com.zoneanchor.app.alarm.AlarmScheduler
import com.zoneanchor.app.data.AlarmRepository
import com.zoneanchor.app.data.BackupRepository
import com.zoneanchor.app.data.ClockRepository
import com.zoneanchor.app.data.SettingsRepository
import com.zoneanchor.app.data.StopwatchRepository
import com.zoneanchor.app.data.TimerRepository
import com.zoneanchor.app.data.db.AppDatabase
import com.zoneanchor.app.timer.TimerScheduler

class ZoneAnchorApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(app: Application) {
    private val db = AppDatabase.get(app)
    val clockRepo = ClockRepository(db.clockDao())
    val alarmRepo = AlarmRepository(db.alarmDao())
    val timerRepo = TimerRepository(db.timerDao())
    val stopwatchRepo = StopwatchRepository(db.stopwatchDao())
    val settings = SettingsRepository(app)
    val scheduler = AlarmScheduler(app)
    val timerScheduler = TimerScheduler(app)
    val backupRepo = BackupRepository(db, clockRepo, alarmRepo, timerRepo, stopwatchRepo, settings)
}
