package com.zoneanchor.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.zoneanchor.MainActivity
import com.zoneanchor.data.alarms.AlarmRepository
import com.zoneanchor.model.ZonedAlarm
import java.time.Instant
import java.time.ZoneId

class AlarmScheduler(
    private val context: Context,
    private val repository: AlarmRepository,
) {
    suspend fun scheduleNext(alarm: ZonedAlarm): ScheduleResult {
        val manager = context.getSystemService(AlarmManager::class.java)
            ?: return ScheduleResult(false, "Alarm service is not available.", alarm)
        if (!canScheduleExactAlarms()) {
            return ScheduleResult(false, "Exact alarm permission is needed for locked-time alerts.", alarm)
        }
        val nextTrigger = AlarmTimeCalculator.nextTriggerMillis(
            zoneId = ZoneId.of(ZonedAlarm.validZoneId(alarm.zoneId)),
            hour = alarm.hour,
            minute = alarm.minute,
            daysOfWeekMask = alarm.daysOfWeekMask,
            now = Instant.now(),
        )
        val scheduled = alarm.enabledWithNextTrigger(nextTrigger)
        repository.upsert(scheduled)
        return setAlarmClock(manager, scheduled.id, nextTrigger, "Alarm scheduled.", scheduled)
    }

    suspend fun scheduleOneOff(alarm: ZonedAlarm, triggerAt: Instant): ScheduleResult {
        val manager = context.getSystemService(AlarmManager::class.java)
            ?: return ScheduleResult(false, "Alarm service is not available.", alarm)
        if (!canScheduleExactAlarms()) {
            return ScheduleResult(false, "Exact alarm permission is needed for snooze.", alarm)
        }
        return setAlarmClock(manager, alarm.id, triggerAt.toEpochMilli(), "Snoozed for 9 minutes.", alarm)
    }

    suspend fun cancel(alarmId: Int) {
        cancelSystemOnly(alarmId)
        repository.delete(alarmId)
        NotificationHelper.cancel(context, alarmId)
    }

    fun cancelSystemOnly(alarmId: Int) {
        context.getSystemService(AlarmManager::class.java)?.cancel(alarmBroadcastIntent(alarmId))
    }

    suspend fun rescheduleEnabledAlarms() {
        repository.enabledOnce().forEach { scheduleNext(it) }
    }

    fun canScheduleExactAlarms(): Boolean {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
    }

    fun exactAlarmSettingsIntent(): Intent {
        val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
        } else {
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        }
        return Intent(action).setData(Uri.parse("package:${context.packageName}"))
    }

    private fun setAlarmClock(
        manager: AlarmManager,
        alarmId: Int,
        triggerAtMillis: Long,
        message: String,
        alarm: ZonedAlarm,
    ): ScheduleResult {
        return try {
            val info = AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent(alarmId))
            manager.setAlarmClock(info, alarmBroadcastIntent(alarmId))
            ScheduleResult(true, message, alarm.copy(nextTriggerAtMillis = triggerAtMillis, enabled = true))
        } catch (_: SecurityException) {
            ScheduleResult(false, "Android blocked exact alarm scheduling.", alarm)
        }
    }

    private fun alarmBroadcastIntent(alarmId: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_FIRE_ALARM)
            .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        return PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun showIntent(alarmId: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    data class ScheduleResult(
        val scheduled: Boolean,
        val message: String,
        val alarm: ZonedAlarm,
    )
}
