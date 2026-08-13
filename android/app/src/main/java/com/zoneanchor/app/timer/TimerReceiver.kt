package com.zoneanchor.app.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zoneanchor.app.ZoneAnchorApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val timerId = intent.getLongExtra(EXTRA_TIMER_ID, -1L)
        if (timerId < 0) return
        val app = context.applicationContext as ZoneAnchorApp
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_FIRE -> app.container.timerScheduler.onFired(timerId)
                    ACTION_DISMISS -> {
                        val t = app.container.timerRepo.getById(timerId) ?: return@launch
                        app.container.timerScheduler.dismiss(t)
                    }
                    ACTION_ADD_MINUTE -> {
                        val t = app.container.timerRepo.getById(timerId) ?: return@launch
                        app.container.timerScheduler.addMinute(t)
                    }
                    ACTION_ADD_FIVE_MINUTES -> {
                        val t = app.container.timerRepo.getById(timerId) ?: return@launch
                        app.container.timerScheduler.addMinutes(t, minutes = 5)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.zoneanchor.app.timer.action.FIRE"
        const val ACTION_DISMISS = "com.zoneanchor.app.timer.action.DISMISS"
        const val ACTION_ADD_MINUTE = "com.zoneanchor.app.timer.action.ADD_MINUTE"
        const val ACTION_ADD_FIVE_MINUTES = "com.zoneanchor.app.timer.action.ADD_FIVE_MINUTES"
        const val EXTRA_TIMER_ID = "extra_timer_id"
    }
}
