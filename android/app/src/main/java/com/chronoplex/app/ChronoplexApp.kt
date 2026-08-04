package com.chronoplex.app

import android.app.Application
import com.chronoplex.app.alarm.AlarmScheduler
import com.chronoplex.app.data.AlarmRepository
import com.chronoplex.app.data.BackupRepository
import com.chronoplex.app.data.ClockRepository
import com.chronoplex.app.data.SettingsRepository
import com.chronoplex.app.data.StopwatchRepository
import com.chronoplex.app.data.TimerRepository
import com.chronoplex.app.data.db.AppDatabase
import com.chronoplex.app.timer.TimerScheduler

class ChronoplexApp : Application() {
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
