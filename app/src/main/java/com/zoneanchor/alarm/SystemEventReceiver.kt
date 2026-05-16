package com.zoneanchor.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zoneanchor.ZoneAnchorApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

open class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        scope.launch {
            try {
                (context.applicationContext as ZoneAnchorApp).alarmScheduler.rescheduleEnabledAlarms()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
