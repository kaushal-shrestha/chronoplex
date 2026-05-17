package com.chronoplex.app

import android.app.Application
import com.chronoplex.app.alarm.AlarmScheduler
import com.chronoplex.app.data.AlarmRepository
import com.chronoplex.app.data.ClockRepository
import com.chronoplex.app.data.SettingsRepository
import com.chronoplex.app.data.db.AppDatabase

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
    val settings = SettingsRepository(app)
    val scheduler = AlarmScheduler(app)
}
