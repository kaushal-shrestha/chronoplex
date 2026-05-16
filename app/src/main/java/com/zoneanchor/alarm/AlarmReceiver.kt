package com.zoneanchor.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zoneanchor.ZoneAnchorApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        scope.launch {
            try {
                val app = context.applicationContext as ZoneAnchorApp
                val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, com.zoneanchor.model.ZonedAlarm.DEFAULT_ID)
                val alarm = app.alarmRepository.find(alarmId)
                if (alarm != null && alarm.enabled) {
                    NotificationHelper.showAlarm(context, alarm)
                    app.alarmScheduler.scheduleNext(alarm)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_FIRE_ALARM = "com.zoneanchor.action.FIRE_ALARM"
        const val EXTRA_ALARM_ID = "com.zoneanchor.extra.ALARM_ID"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
