package com.zoneanchor.app.timer

import android.content.Context
import com.zoneanchor.app.domain.Timer

/**
 * Bridges the timer state machine to the foreground alert service. Timer audio is
 * played by [TimerService], not by the notification channel, so it keeps looping
 * until the user explicitly stops or extends the finished timer.
 */
object TimerNotifier {
    fun showFinishedNotification(context: Context, timer: Timer) {
        TimerService.start(context, timer.id)
    }

    fun cancel(context: Context, timerId: Long) {
        TimerService.stop(context, timerId)
    }
}
