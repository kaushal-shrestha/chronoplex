package com.zoneanchor.alarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zoneanchor.alarm.ZoneAnchorApp
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
                val app = context.applicationContext as ZoneAnchorApp
                val alarms = app.container.alarmRepo.getAllEnabled()
                alarms.forEach { app.container.scheduler.schedule(it) }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val HANDLED = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
        )
    }
}
