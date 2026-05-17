package com.zoneanchor.alarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.zoneanchor.alarm.MainActivity
import com.zoneanchor.alarm.domain.Alarm
import com.zoneanchor.alarm.domain.DayMask
import java.time.Instant
import java.time.LocalDateTime
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
        /**
         * Returns the next epoch-millis this alarm should fire at, or null if it never will.
         *
         * Walks forward up to 8 days from `fromMillis` in the alarm's zone. Each candidate
         * is validated against the zone's DST rules:
         *   - Spring-forward: if the wall-clock time doesn't exist that day (e.g., 02:30 on
         *     the second Sunday of March in America/New_York), the day is skipped — the
         *     alarm is anchored to a wall-clock time, not an instant.
         *   - Fall-back: when the wall-clock time exists twice, the earlier (still-DST)
         *     occurrence is chosen.
         */
        fun nextTriggerMillis(alarm: Alarm, fromMillis: Long = System.currentTimeMillis()): Long? {
            val zone = runCatching { ZoneId.of(alarm.zoneId) }.getOrElse { ZoneId.systemDefault() }
            val nowInstant = Instant.ofEpochMilli(fromMillis)
            val zoneNow = nowInstant.atZone(zone)
            val oneShot = alarm.isOneShot

            for (offset in 0..8) {
                val local = zoneNow.toLocalDate().plusDays(offset.toLong())
                    .atTime(alarm.hour, alarm.minute)
                val candidate = candidateOrNull(local, zone) ?: continue
                if (!candidate.toInstant().isAfter(nowInstant)) continue
                if (!oneShot && !DayMask.contains(alarm.daysMask, candidate.dayOfWeek)) continue
                return candidate.toInstant().toEpochMilli()
            }
            return null
        }

        private fun candidateOrNull(local: LocalDateTime, zoneId: ZoneId): ZonedDateTime? {
            val offsets = zoneId.rules.getValidOffsets(local)
            if (offsets.isEmpty()) return null
            val candidate = ZonedDateTime.ofLocal(local, zoneId, offsets.first())
            return if (candidate.hour == local.hour && candidate.minute == local.minute) candidate else null
        }
    }
}
