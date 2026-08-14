package com.chronoplex.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.chronoplex.app.ChronoplexApp
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
            ACTION_SNOOZE -> snoozeRingingAlarmAsync(context, alarmId)
            ACTION_DISMISS -> dismissRingingAlarmAsync(context, alarmId)
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

    private fun snoozeRingingAlarmAsync(context: Context, alarmId: Long) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as ChronoplexApp
                val alarm = app.container.alarmRepo.getById(alarmId) ?: return@launch
                app.container.scheduler.snooze(alarm, minutes = ACTION_SNOOZE_MINUTES)
                stopRingService(context)
            } finally {
                pending.finish()
            }
        }
    }

    private fun dismissRingingAlarmAsync(context: Context, alarmId: Long) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as ChronoplexApp
                val alarm = app.container.alarmRepo.getById(alarmId) ?: run {
                    stopRingService(context)
                    return@launch
                }
                app.container.alarmRepo.setSnoozeUntil(alarmId, null)
                if (alarm.isOneShot) {
                    app.container.alarmRepo.setEnabled(alarm.id, false)
                    app.container.scheduler.cancel(alarm.id)
                } else {
                    app.container.scheduler.schedule(alarm.copy(snoozeUntilMillis = null))
                }
                stopRingService(context)
            } finally {
                pending.finish()
            }
        }
    }

    private fun cancelSnoozeAsync(context: Context, alarmId: Long) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as ChronoplexApp
                val alarm = app.container.alarmRepo.getById(alarmId) ?: return@launch
                app.container.scheduler.cancelSnooze(alarm)
            } finally {
                pending.finish()
            }
        }
    }

    private fun stopRingService(context: Context) {
        context.stopService(Intent(context, AlarmService::class.java))
    }

    companion object {
        const val ACTION_FIRE = "com.chronoplex.app.action.FIRE"
        const val ACTION_SNOOZE = "com.chronoplex.app.action.SNOOZE"
        const val ACTION_DISMISS = "com.chronoplex.app.action.DISMISS"
        const val ACTION_CANCEL_SNOOZE = "com.chronoplex.app.action.CANCEL_SNOOZE"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val ACTION_SNOOZE_MINUTES = 5
    }
}
