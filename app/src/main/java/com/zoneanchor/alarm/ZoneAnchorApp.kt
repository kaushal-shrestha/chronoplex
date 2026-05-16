package com.zoneanchor.alarm

import android.app.Application
import com.zoneanchor.alarm.alarm.AlarmScheduler
import com.zoneanchor.alarm.data.AlarmRepository
import com.zoneanchor.alarm.data.ClockRepository
import com.zoneanchor.alarm.data.SettingsRepository
import com.zoneanchor.alarm.data.db.AppDatabase

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
    val settings = SettingsRepository(app)
    val scheduler = AlarmScheduler(app)
}
