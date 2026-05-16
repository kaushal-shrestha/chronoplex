package com.zoneanchor;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import java.time.Instant;
import java.time.ZoneId;

public final class AlarmScheduler {
    private AlarmScheduler() {
    }

    public static ScheduleResult scheduleNext(Context context, ZonedAlarm alarm) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager == null) {
            return ScheduleResult.failure("Alarm service is not available.", alarm);
        }

        if (!canScheduleExactAlarms(context)) {
            return ScheduleResult.failure("Exact alarm permission is needed for locked-time alerts.", alarm);
        }

        long nextTriggerAtMillis = AlarmTimeCalculator.nextTriggerMillis(
                ZoneId.of(alarm.zoneId),
                alarm.hour,
                alarm.minute,
                alarm.daysOfWeekMask,
                Instant.now()
        );
        ZonedAlarm scheduledAlarm = alarm.enabledWithNextTrigger(nextTriggerAtMillis);
        AlarmStore.save(context, scheduledAlarm);

        PendingIntent broadcastIntent = alarmBroadcastIntent(context, scheduledAlarm.id);
        PendingIntent showIntent = showAppIntent(context, scheduledAlarm.id);
        AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(nextTriggerAtMillis, showIntent);

        try {
            alarmManager.setAlarmClock(info, broadcastIntent);
            return ScheduleResult.success(scheduledAlarm);
        } catch (SecurityException exception) {
            return ScheduleResult.failure("Android blocked exact alarm scheduling.", scheduledAlarm);
        }
    }

    public static void cancel(Context context, int alarmId) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager != null) {
            alarmManager.cancel(alarmBroadcastIntent(context, alarmId));
        }
        AlarmStore.remove(context, alarmId);
    }

    public static boolean canScheduleExactAlarms(Context context) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return alarmManager.canScheduleExactAlarms();
        }
        return true;
    }

    public static Intent exactAlarmSettingsIntent(Context context) {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
        }
        return intent;
    }

    private static PendingIntent alarmBroadcastIntent(Context context, int alarmId) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(AlarmReceiver.ACTION_FIRE_ALARM);
        intent.putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId);
        return PendingIntent.getBroadcast(
                context,
                alarmId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static PendingIntent showAppIntent(Context context, int alarmId) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(
                context,
                alarmId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    public static final class ScheduleResult {
        public final boolean scheduled;
        public final String message;
        public final ZonedAlarm alarm;

        private ScheduleResult(boolean scheduled, String message, ZonedAlarm alarm) {
            this.scheduled = scheduled;
            this.message = message;
            this.alarm = alarm;
        }

        public static ScheduleResult success(ZonedAlarm alarm) {
            return new ScheduleResult(true, "Alarm scheduled.", alarm);
        }

        public static ScheduleResult failure(String message, ZonedAlarm alarm) {
            return new ScheduleResult(false, message, alarm);
        }
    }
}
