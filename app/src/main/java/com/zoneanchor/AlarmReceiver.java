package com.zoneanchor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class AlarmReceiver extends BroadcastReceiver {
    public static final String ACTION_FIRE_ALARM = "com.zoneanchor.action.FIRE_ALARM";
    public static final String EXTRA_ALARM_ID = "com.zoneanchor.extra.ALARM_ID";

    @Override
    public void onReceive(Context context, Intent intent) {
        int alarmId = intent.getIntExtra(EXTRA_ALARM_ID, ZonedAlarm.DEFAULT_ID);
        ZonedAlarm alarm = AlarmStore.load(context, alarmId);
        if (alarm == null || !alarm.enabled) {
            return;
        }

        NotificationHelper.showAlarm(context, alarm);
        AlarmScheduler.scheduleNext(context, alarm);
    }
}
