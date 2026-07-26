package com.chronoplex.app.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.chronoplex.app.ChronoplexApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val timerId = intent.getLongExtra(EXTRA_TIMER_ID, -1L)
        if (timerId < 0) return
        val app = context.applicationContext as ChronoplexApp
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
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.chronoplex.app.timer.action.FIRE"
        const val ACTION_DISMISS = "com.chronoplex.app.timer.action.DISMISS"
        const val ACTION_ADD_MINUTE = "com.chronoplex.app.timer.action.ADD_MINUTE"
        const val EXTRA_TIMER_ID = "extra_timer_id"
    }
}
