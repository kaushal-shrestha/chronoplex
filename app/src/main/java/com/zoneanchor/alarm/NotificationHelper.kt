package com.zoneanchor.alarm

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.zoneanchor.R
import com.zoneanchor.model.ZonedAlarm
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object NotificationHelper {
    const val CHANNEL_DEFAULT_VIBRATE = "locked_alarm_alerts_default_vibrate"
    const val CHANNEL_DEFAULT_QUIET = "locked_alarm_alerts_default_quiet"
    const val CHANNEL_SILENT_VIBRATE = "locked_alarm_alerts_silent_vibrate"
    const val CHANNEL_SILENT_QUIET = "locked_alarm_alerts_silent_quiet"
    private const val REQUEST_NOTIFICATIONS = 401
    private val alarmFormat = DateTimeFormatter.ofPattern("h:mm a z, EEE MMM d", Locale.getDefault())

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        createChannel(manager, CHANNEL_DEFAULT_VIBRATE, "Alarm sound and vibration", true, true)
        createChannel(manager, CHANNEL_DEFAULT_QUIET, "Alarm sound", true, false)
        createChannel(manager, CHANNEL_SILENT_VIBRATE, "Silent alarm with vibration", false, true)
        createChannel(manager, CHANNEL_SILENT_QUIET, "Silent alarm", false, false)
    }

    fun requestNotificationPermissionIfNeeded(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            activity.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        }
    }

    fun canPostNotifications(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun showAlarm(context: Context, alarm: ZonedAlarm) {
        ensureChannels(context)
        if (!canPostNotifications(context)) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val lockedTime = alarmFormat.format(Instant.now().atZone(ZoneId.of(alarm.zoneId)))
        val title = alarm.label.ifBlank { "Locked alarm" }
        val text = "It is $lockedTime in ${alarm.zoneId}."
        val builder = Notification.Builder(context, channelId(alarm))
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(ringIntent(context, alarm.id))
            .setFullScreenIntent(ringIntent(context, alarm.id), true)
            .addAction(action(context, alarm.id, AlarmActionReceiver.ACTION_SNOOZE, "Snooze"))
            .addAction(action(context, alarm.id, AlarmActionReceiver.ACTION_DISMISS, "Dismiss"))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setSound(if (alarm.soundMode == ZonedAlarm.SOUND_SILENT) null else defaultAlarmUri())
            builder.setVibrate(if (alarm.vibrate) longArrayOf(0L, 500L, 250L, 500L) else longArrayOf(0L))
        }
        manager.notify(alarm.id, builder.build())
    }

    fun cancel(context: Context, alarmId: Int) {
        context.getSystemService(NotificationManager::class.java)?.cancel(alarmId)
    }

    fun channelId(alarm: ZonedAlarm): String {
        return if (alarm.soundMode == ZonedAlarm.SOUND_SILENT) {
            if (alarm.vibrate) CHANNEL_SILENT_VIBRATE else CHANNEL_SILENT_QUIET
        } else {
            if (alarm.vibrate) CHANNEL_DEFAULT_VIBRATE else CHANNEL_DEFAULT_QUIET
        }
    }

    private fun createChannel(
        manager: NotificationManager,
        id: String,
        name: String,
        sound: Boolean,
        vibrate: Boolean,
    ) {
        val channel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH)
        channel.description = "Time-zone locked ZoneAnchor Alarm alerts."
        channel.enableVibration(vibrate)
        if (vibrate) channel.vibrationPattern = longArrayOf(0L, 500L, 250L, 500L)
        channel.setSound(if (sound) defaultAlarmUri() else null, if (sound) alarmAudioAttributes() else null)
        manager.createNotificationChannel(channel)
    }

    private fun action(context: Context, alarmId: Int, action: String, title: String): Notification.Action {
        val intent = Intent(context, AlarmActionReceiver::class.java)
            .setAction(action)
            .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        val pending = PendingIntent.getBroadcast(
            context,
            alarmId + action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Action.Builder(R.drawable.ic_alarm, title, pending).build()
    }

    private fun ringIntent(context: Context, alarmId: Int): PendingIntent {
        val intent = Intent(context, AlarmRingActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        return PendingIntent.getActivity(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun defaultAlarmUri() =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    private fun alarmAudioAttributes() = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
}
