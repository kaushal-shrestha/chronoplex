package com.zoneanchor.app.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.zoneanchor.app.ZoneAnchorApp
import com.zoneanchor.app.domain.Timer
import com.zoneanchor.app.domain.TimerState

/**
 * Drives Timer lifecycle. Keeps the database row in sync with the OS-level
 * AlarmManager trigger, and dispatches the [TimerNotifier] alert path via
 * [TimerReceiver] when a timer fires.
 *
 * State machine:
 *   IDLE ──start──▶ RUNNING ──fires──▶ FINISHED ──dismiss──▶ IDLE
 *                    │   ▲
 *                    │   └────resume─────┐
 *                    ▼                   │
 *                  PAUSED ───────────────┘
 *
 *   reset(): any → IDLE
 *   addMinute(): RUNNING or FINISHED → RUNNING (+60s)
 */
class TimerScheduler(private val context: Context) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** Start an IDLE/PAUSED/FINISHED timer. */
    suspend fun start(timer: Timer) {
        val durationLeft = when (timer.state) {
            TimerState.PAUSED -> timer.pausedRemainingMillis ?: timer.durationMillis
            TimerState.FINISHED, TimerState.IDLE, TimerState.RUNNING -> timer.durationMillis
        }
        scheduleAt(timer.id, System.currentTimeMillis() + durationLeft)
        repo().updateState(
            id = timer.id,
            state = TimerState.RUNNING,
            endsAtMillis = System.currentTimeMillis() + durationLeft,
            pausedRemainingMillis = null,
        )
    }

    suspend fun pause(timer: Timer) {
        val remaining = timer.endsAtMillis?.let { (it - System.currentTimeMillis()).coerceAtLeast(0L) }
            ?: timer.durationMillis
        cancelTrigger(timer.id)
        repo().updateState(
            id = timer.id,
            state = TimerState.PAUSED,
            endsAtMillis = null,
            pausedRemainingMillis = remaining,
        )
    }

    suspend fun reset(timer: Timer) {
        cancelTrigger(timer.id)
        TimerNotifier.cancel(context, timer.id)
        repo().updateState(
            id = timer.id,
            state = TimerState.IDLE,
            endsAtMillis = null,
            pausedRemainingMillis = null,
        )
    }

    /** Extend a running or finished timer by 60 seconds. */
    suspend fun addMinute(timer: Timer) {
        val newEnd = when (timer.state) {
            TimerState.RUNNING -> (timer.endsAtMillis ?: System.currentTimeMillis()) + 60_000L
            TimerState.FINISHED -> System.currentTimeMillis() + 60_000L
            TimerState.PAUSED, TimerState.IDLE -> return
        }
        scheduleAt(timer.id, newEnd)
        TimerNotifier.cancel(context, timer.id)
        repo().updateState(
            id = timer.id,
            state = TimerState.RUNNING,
            endsAtMillis = newEnd,
            pausedRemainingMillis = null,
        )
    }

    /** Called from [TimerReceiver] when the OS fires the trigger. */
    suspend fun onFired(timerId: Long) {
        val app = context.applicationContext as ZoneAnchorApp
        val timer = app.container.timerRepo.getById(timerId) ?: return
        app.container.timerRepo.updateState(
            id = timer.id,
            state = TimerState.FINISHED,
            endsAtMillis = null,
            pausedRemainingMillis = null,
        )
        TimerNotifier.showFinishedNotification(context, timer.copy(state = TimerState.FINISHED))
    }

    /** User dismissed a FINISHED timer. */
    suspend fun dismiss(timer: Timer) {
        TimerNotifier.cancel(context, timer.id)
        repo().updateState(
            id = timer.id,
            state = TimerState.IDLE,
            endsAtMillis = null,
            pausedRemainingMillis = null,
        )
    }

    private fun scheduleAt(timerId: Long, triggerAtMillis: Long) {
        val pi = firePendingIntent(timerId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    private fun cancelTrigger(timerId: Long) {
        alarmManager.cancel(firePendingIntent(timerId))
    }

    private fun firePendingIntent(timerId: Long): PendingIntent {
        // Offset request codes to avoid colliding with alarm PendingIntents.
        val requestCode = (1_000_000 + timerId).toInt()
        val intent = Intent(context, TimerReceiver::class.java).apply {
            action = TimerReceiver.ACTION_FIRE
            putExtra(TimerReceiver.EXTRA_TIMER_ID, timerId)
            data = android.net.Uri.parse("zoneanchor://timer/$timerId")
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun repo() = (context.applicationContext as ZoneAnchorApp).container.timerRepo
}
