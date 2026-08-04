package com.zoneanchor.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.zoneanchor.app.ZoneAnchorApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId < 0) return
        when (intent.action) {
            ACTION_FIRE -> startRingService(context, alarmId)
            ACTION_CANCEL_SNOOZE -> cancelSnoozeAsync(context, alarmId)
        }
    }

    private fun startRingService(context: Context, alarmId: Long) {
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START
            putExtra(AlarmService.EXTRA_ALARM_ID, alarmId)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    private fun cancelSnoozeAsync(context: Context, alarmId: Long) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as ZoneAnchorApp
                val alarm = app.container.alarmRepo.getById(alarmId) ?: return@launch
                app.container.scheduler.cancelSnooze(alarm)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.zoneanchor.app.action.FIRE"
        const val ACTION_CANCEL_SNOOZE = "com.zoneanchor.app.action.CANCEL_SNOOZE"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
    }
}
