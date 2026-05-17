package com.zoneanchor.alarm.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.zoneanchor.alarm.R
import com.zoneanchor.alarm.domain.Alarm
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Manages the low-priority "Snoozed until X" notification that stays in the shade
 * while an alarm is snoozed. Lets the user cancel a snooze without waiting for the
 * fire time, and provides outward signal that there's a snooze in flight.
 */
object SnoozeNotifier {
    private const val CHANNEL_ID = "alarm_snoozed"
    private val timeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    fun show(context: Context, alarm: Alarm, triggerMillis: Long) {
        ensureChannel(context)

        val cancelIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_CANCEL_SNOOZE
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            data = android.net.Uri.parse("zoneanchor://snooze-cancel/${alarm.id}")
        }
        val cancelPi = PendingIntent.getBroadcast(
            context,
            (5000 + alarm.id).toInt(),
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val zone = runCatching { ZoneId.of(alarm.zoneId) }.getOrElse { ZoneId.systemDefault() }
        val triggerZoned = ZonedDateTime.ofInstant(Instant.ofEpochMilli(triggerMillis), zone)
        val title = alarm.label.ifBlank { context.getString(R.string.app_name) }
        val text = context.getString(R.string.snoozed_until, timeFmt.format(triggerZoned))

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .addAction(0, context.getString(R.string.cancel_snooze), cancelPi)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId(alarm.id), notification)
    }

    fun cancel(context: Context, alarmId: Long) {
        NotificationManagerCompat.from(context).cancel(notificationId(alarmId))
    }

    private fun notificationId(alarmId: Long): Int = 2000 + alarmId.toInt()

    private fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.snoozed_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.snoozed_channel_description)
                setSound(null, null)
                enableVibration(false)
            },
        )
    }
}
