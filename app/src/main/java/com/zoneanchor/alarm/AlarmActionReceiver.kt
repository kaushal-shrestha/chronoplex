package com.zoneanchor.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zoneanchor.ZoneAnchorApp
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        scope.launch {
            try {
                val app = context.applicationContext as ZoneAnchorApp
                val alarmId = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1)
                val alarm = app.alarmRepository.find(alarmId) ?: return@launch
                NotificationHelper.cancel(context, alarmId)
                when (intent.action) {
                    ACTION_SNOOZE -> {
                        app.alarmScheduler.cancelSystemOnly(alarmId)
                        app.alarmScheduler.scheduleOneOff(alarm, Instant.now().plusSeconds(9 * 60L))
                    }
                    ACTION_DISMISS -> app.alarmScheduler.scheduleNext(alarm)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_SNOOZE = "com.zoneanchor.action.SNOOZE"
        const val ACTION_DISMISS = "com.zoneanchor.action.DISMISS"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
