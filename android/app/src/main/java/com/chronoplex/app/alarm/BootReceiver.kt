package com.chronoplex.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.chronoplex.app.ChronoplexApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in HANDLED) return
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val app = context.applicationContext as ChronoplexApp
                val alarms = app.container.alarmRepo.getAllEnabled()
                alarms.forEach { app.container.scheduler.schedule(it) }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        // On API 31+ the system broadcasts this when the user grants/revokes the
        // exact-alarm permission. We re-arm so alarms upgrade from the inexact
        // fallback to setAlarmClock as soon as permission is granted.
        private const val ACTION_EXACT_ALARM_PERMISSION_CHANGED =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"

        private val HANDLED = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            ACTION_EXACT_ALARM_PERMISSION_CHANGED,
        )
    }
}
