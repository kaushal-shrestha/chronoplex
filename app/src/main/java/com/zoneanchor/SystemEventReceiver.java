package com.zoneanchor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class SystemEventReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        for (ZonedAlarm alarm : AlarmStore.loadAll(context)) {
            if (alarm.enabled) {
                AlarmScheduler.scheduleNext(context, alarm);
            }
        }
    }
}
