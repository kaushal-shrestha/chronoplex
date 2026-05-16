package com.zoneanchor.alarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.zoneanchor.alarm.MainActivity
import com.zoneanchor.alarm.domain.Alarm
import com.zoneanchor.alarm.domain.DayMask
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(alarm: Alarm) {
        if (!alarm.enabled) {
            cancel(alarm.id)
            return
        }
        val triggerMillis = nextTriggerMillis(alarm) ?: return
        val pi = firePendingIntent(alarm.id)
        val showPi = showPendingIntent(alarm.id)
        val info = AlarmManager.AlarmClockInfo(triggerMillis, showPi)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // No permission yet — schedule a best-effort inexact alarm so it still rings near the time.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
        } else {
            alarmManager.setAlarmClock(info, pi)
        }
    }

    fun cancel(alarmId: Long) {
        alarmManager.cancel(firePendingIntent(alarmId))
    }

    fun snooze(alarm: Alarm, minutes: Int = 9) {
        val triggerMillis = System.currentTimeMillis() + minutes * 60_000L
        val pi = firePendingIntent(alarm.id)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
        } else {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent(alarm.id)),
                pi,
            )
        }
    }

    private fun firePendingIntent(alarmId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            // Make the intent unique per alarm so PendingIntent doesn't collapse.
            data = android.net.Uri.parse("zoneanchor://alarm/$alarmId")
        }
        return PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun showPendingIntent(alarmId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_ALARM_ID, alarmId)
        }
        return PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        /** Returns the next epoch-millis this alarm should fire at, or null if it never will. */
        fun nextTriggerMillis(alarm: Alarm, fromMillis: Long = System.currentTimeMillis()): Long? {
            val zone = runCatching { ZoneId.of(alarm.zoneId) }.getOrElse { ZoneId.systemDefault() }
            val now = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(fromMillis), zone)
            val time = LocalTime.of(alarm.hour, alarm.minute)

            if (alarm.isOneShot) {
                var candidate = now.with(time).withSecond(0).withNano(0)
                if (!candidate.isAfter(now)) candidate = candidate.plusDays(1)
                return candidate.toInstant().toEpochMilli()
            }

            // Look up to 8 days ahead for the next enabled day-of-week.
            for (offset in 0..8) {
                val candidate = now.plusDays(offset.toLong()).with(time).withSecond(0).withNano(0)
                if (!candidate.isAfter(now)) continue
                if (DayMask.contains(alarm.daysMask, candidate.dayOfWeek)) {
                    return candidate.toInstant().toEpochMilli()
                }
            }
            return null
        }
    }
}
