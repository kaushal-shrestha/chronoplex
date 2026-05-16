package com.zoneanchor;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class NotificationHelper {
    private static final String CHANNEL_DEFAULT_VIBRATE = "locked_alarm_alerts_default_vibrate";
    private static final String CHANNEL_DEFAULT_QUIET = "locked_alarm_alerts_default_quiet";
    private static final String CHANNEL_SILENT_VIBRATE = "locked_alarm_alerts_silent_vibrate";
    private static final String CHANNEL_SILENT_QUIET = "locked_alarm_alerts_silent_quiet";
    private static final int NOTIFICATION_ID = 400;
    private static final int REQUEST_NOTIFICATIONS = 401;
    private static final DateTimeFormatter ALARM_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a z, EEE MMM d", Locale.getDefault());

    private NotificationHelper() {
    }

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }

        createChannel(
                manager,
                CHANNEL_DEFAULT_VIBRATE,
                "Alarm sound and vibration",
                ZonedAlarm.SOUND_DEFAULT,
                true
        );
        createChannel(
                manager,
                CHANNEL_DEFAULT_QUIET,
                "Alarm sound",
                ZonedAlarm.SOUND_DEFAULT,
                false
        );
        createChannel(
                manager,
                CHANNEL_SILENT_VIBRATE,
                "Silent alarm with vibration",
                ZonedAlarm.SOUND_SILENT,
                true
        );
        createChannel(
                manager,
                CHANNEL_SILENT_QUIET,
                "Silent alarm",
                ZonedAlarm.SOUND_SILENT,
                false
        );
    }

    private static void createChannel(
            NotificationManager manager,
            String channelId,
            String channelName,
            String soundMode,
            boolean vibrate
    ) {
        NotificationChannel channel = new NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Time-zone locked ZoneAnchor Alarm alerts.");
        channel.enableVibration(vibrate);
        if (vibrate) {
            channel.setVibrationPattern(new long[]{0L, 500L, 250L, 500L});
        }
        if (ZonedAlarm.SOUND_SILENT.equals(soundMode)) {
            channel.setSound(null, null);
        } else {
            channel.setSound(defaultAlarmUri(), alarmAudioAttributes());
        }
        manager.createNotificationChannel(channel);
    }

    public static void requestNotificationPermissionIfNeeded(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        }
    }

    @SuppressWarnings("deprecation")
    public static void showAlarm(Context context, ZonedAlarm alarm) {
        ensureChannel(context);

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }

        String lockedTime = ALARM_FORMAT.format(Instant.now().atZone(ZoneId.of(alarm.zoneId)));
        String title = alarm.label.isEmpty() ? "Locked alarm" : alarm.label;
        String text = "It is " + lockedTime + " in " + alarm.zoneId + ".";

        Notification.Builder builder = new Notification.Builder(context, channelId(alarm))
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(openAppIntent(context));

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            if (ZonedAlarm.SOUND_SILENT.equals(alarm.soundMode)) {
                builder.setSound(null);
            } else {
                builder.setSound(defaultAlarmUri());
            }
            builder.setVibrate(alarm.vibrate ? new long[]{0L, 500L, 250L, 500L} : new long[]{0L});
        }

        Notification notification = builder.build();

        manager.notify(alarm.id, notification);
    }

    private static String channelId(ZonedAlarm alarm) {
        if (ZonedAlarm.SOUND_SILENT.equals(alarm.soundMode)) {
            return alarm.vibrate ? CHANNEL_SILENT_VIBRATE : CHANNEL_SILENT_QUIET;
        }
        return alarm.vibrate ? CHANNEL_DEFAULT_VIBRATE : CHANNEL_DEFAULT_QUIET;
    }

    private static Uri defaultAlarmUri() {
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (uri == null) {
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        return uri;
    }

    private static AudioAttributes alarmAudioAttributes() {
        return new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
    }

    private static PendingIntent openAppIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
