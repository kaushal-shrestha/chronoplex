package com.zoneanchor.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.zoneanchor.app.MainActivity
import com.zoneanchor.app.domain.Alarm
import com.zoneanchor.app.domain.AlarmRepeatType
import com.zoneanchor.app.domain.DayMask
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

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

    suspend fun snooze(alarm: Alarm, minutes: Int = 9) {
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
        // Persist state + surface a low-priority notification with a Cancel action.
        val app = context.applicationContext as com.zoneanchor.app.ZoneAnchorApp
        app.container.alarmRepo.setSnoozeUntil(alarm.id, triggerMillis)
        SnoozeNotifier.show(context, alarm.copy(snoozeUntilMillis = triggerMillis), triggerMillis)
    }

    /** Called when the user taps Cancel on the snoozed notification. Restores the regular schedule. */
    suspend fun cancelSnooze(alarm: Alarm) {
        val app = context.applicationContext as com.zoneanchor.app.ZoneAnchorApp
        app.container.alarmRepo.setSnoozeUntil(alarm.id, null)
        SnoozeNotifier.cancel(context, alarm.id)
        schedule(alarm.copy(snoozeUntilMillis = null))
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
            return when (alarm.effectiveRepeatType) {
                AlarmRepeatType.ONCE -> nextOneShot(alarm, zone, zoneNow, nowInstant)
                AlarmRepeatType.WEEKLY -> nextWeekly(alarm, zone, zoneNow, nowInstant)
                AlarmRepeatType.MONTHLY_DAY -> nextMonthlyDay(alarm, zone, zoneNow, nowInstant)
                AlarmRepeatType.MONTHLY_WEEKDAY -> nextMonthlyWeekday(alarm, zone, zoneNow, nowInstant)
            }
        }

        private fun nextOneShot(
            alarm: Alarm,
            zone: ZoneId,
            zoneNow: ZonedDateTime,
            nowInstant: Instant,
        ): Long? {
            for (offset in 0..2) {
                val local = zoneNow.toLocalDate().plusDays(offset.toLong())
                    .atTime(alarm.hour, alarm.minute)
                val candidate = candidateOrNull(local, zone) ?: continue
                if (!candidate.toInstant().isAfter(nowInstant)) continue
                return candidate.toInstant().toEpochMilli()
            }
            return null
        }

        private fun nextWeekly(
            alarm: Alarm,
            zone: ZoneId,
            zoneNow: ZonedDateTime,
            nowInstant: Instant,
        ): Long? {
            val selectedDays = DayMask.sanitize(alarm.daysMask)
            if (selectedDays == 0) return null
            val interval = alarm.repeatInterval.coerceIn(1, 99)
            val anchor = startDateOrToday(alarm, zoneNow)
            val horizonDays = 8 + interval * 7
            for (offset in 0..horizonDays) {
                val date = zoneNow.toLocalDate().plusDays(offset.toLong())
                if (date.isBefore(anchor)) continue
                if (!isOnWeeklyInterval(date, anchor, interval)) continue
                val local = date.atTime(alarm.hour, alarm.minute)
                val candidate = candidateOrNull(local, zone) ?: continue
                if (!candidate.toInstant().isAfter(nowInstant)) continue
                if (!DayMask.contains(selectedDays, candidate.dayOfWeek)) continue
                return candidate.toInstant().toEpochMilli()
            }
            return null
        }

        private fun nextMonthlyDay(
            alarm: Alarm,
            zone: ZoneId,
            zoneNow: ZonedDateTime,
            nowInstant: Instant,
        ): Long? {
            val anchor = startDateOrToday(alarm, zoneNow)
            val interval = alarm.repeatInterval.coerceIn(1, 99)
            val day = alarm.monthlyDay.coerceIn(1, 31)
            val firstMonth = if (zoneNow.toLocalDate().isBefore(anchor)) YearMonth.from(anchor)
                             else YearMonth.from(zoneNow.toLocalDate())
            for (offset in 0..2400) {
                val month = firstMonth.plusMonths(offset.toLong())
                if (!isOnMonthlyInterval(month, YearMonth.from(anchor), interval)) continue
                if (day > month.lengthOfMonth()) continue
                val date = month.atDay(day)
                if (date.isBefore(anchor)) continue
                val candidate = candidateOrNull(date.atTime(alarm.hour, alarm.minute), zone) ?: continue
                if (candidate.toInstant().isAfter(nowInstant)) return candidate.toInstant().toEpochMilli()
            }
            return null
        }

        private fun nextMonthlyWeekday(
            alarm: Alarm,
            zone: ZoneId,
            zoneNow: ZonedDateTime,
            nowInstant: Instant,
        ): Long? {
            val anchor = startDateOrToday(alarm, zoneNow)
            val interval = alarm.repeatInterval.coerceIn(1, 99)
            val weekday = DayOfWeek.of(alarm.monthlyWeekday.coerceIn(1, 7))
            val ordinal = if (alarm.monthlyOrdinal == -1) -1 else alarm.monthlyOrdinal.coerceIn(1, 4)
            val firstMonth = if (zoneNow.toLocalDate().isBefore(anchor)) YearMonth.from(anchor)
                             else YearMonth.from(zoneNow.toLocalDate())
            for (offset in 0..2400) {
                val month = firstMonth.plusMonths(offset.toLong())
                if (!isOnMonthlyInterval(month, YearMonth.from(anchor), interval)) continue
                val date = month.atDay(1).with(TemporalAdjusters.dayOfWeekInMonth(ordinal, weekday))
                if (YearMonth.from(date) != month || date.isBefore(anchor)) continue
                val candidate = candidateOrNull(date.atTime(alarm.hour, alarm.minute), zone) ?: continue
                if (candidate.toInstant().isAfter(nowInstant)) return candidate.toInstant().toEpochMilli()
            }
            return null
        }

        private fun candidateOrNull(local: LocalDateTime, zoneId: ZoneId): ZonedDateTime? {
            val offsets = zoneId.rules.getValidOffsets(local)
            if (offsets.isEmpty()) return null
            val candidate = ZonedDateTime.ofLocal(local, zoneId, offsets.first())
            return if (candidate.hour == local.hour && candidate.minute == local.minute) candidate else null
        }

        private fun startDateOrToday(alarm: Alarm, zoneNow: ZonedDateTime): LocalDate =
            runCatching { LocalDate.parse(alarm.repeatStartDate) }.getOrElse { zoneNow.toLocalDate() }

        private fun isOnWeeklyInterval(date: LocalDate, anchor: LocalDate, interval: Int): Boolean {
            val anchorWeek = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val dateWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val weeks = ChronoUnit.WEEKS.between(anchorWeek, dateWeek)
            return weeks >= 0 && weeks % interval == 0L
        }

        private fun isOnMonthlyInterval(month: YearMonth, anchor: YearMonth, interval: Int): Boolean {
            val months = ChronoUnit.MONTHS.between(anchor, month)
            return months >= 0 && months % interval == 0L
        }
    }
}
