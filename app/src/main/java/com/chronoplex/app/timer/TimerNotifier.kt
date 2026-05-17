package com.chronoplex.app.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.chronoplex.app.R
import com.chronoplex.app.domain.Timer

/**
 * Posts the "timer finished" notification and (optionally) hooks up a full-screen
 * intent to [TimerExpiredActivity] for users who prefer the alarm-style takeover.
 */
object TimerNotifier {
    private const val CHANNEL_ID = "timer_finished"

    fun showFinishedNotification(context: Context, timer: Timer, fullScreen: Boolean = false) {
        ensureChannel(context)

        val title = timer.label.ifBlank { context.getString(R.string.timer_finished_default_title) }
        val text = context.getString(R.string.timer_finished_body)

        val dismissPi = action(context, timer.id, TimerReceiver.ACTION_DISMISS, 1)
        val addMinutePi = action(context, timer.id, TimerReceiver.ACTION_ADD_MINUTE, 2)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(0, context.getString(R.string.add_minute), addMinutePi)
            .addAction(0, context.getString(R.string.stop), dismissPi)
            .setDeleteIntent(dismissPi)

        if (fullScreen) {
            val fsIntent = Intent(context, TimerExpiredActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(TimerExpiredActivity.EXTRA_TIMER_ID, timer.id)
            }
            val fsPi = PendingIntent.getActivity(
                context,
                (3_000_000 + timer.id).toInt(),
                fsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.setFullScreenIntent(fsPi, true).setContentIntent(fsPi)
        }

        NotificationManagerCompat.from(context).notify(notificationId(timer.id), builder.build())

        if (fullScreen) {
            // Launch the activity directly too — some launchers gate full-screen intents.
            context.startActivity(
                Intent(context, TimerExpiredActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(TimerExpiredActivity.EXTRA_TIMER_ID, timer.id)
                }
            )
        }
    }

    fun cancel(context: Context, timerId: Long) {
        NotificationManagerCompat.from(context).cancel(notificationId(timerId))
    }

    private fun action(
        context: Context,
        timerId: Long,
        action: String,
        salt: Int,
    ): PendingIntent {
        val intent = Intent(context, TimerReceiver::class.java).apply {
            this.action = action
            putExtra(TimerReceiver.EXTRA_TIMER_ID, timerId)
            data = android.net.Uri.parse("chronoplex://timer/$timerId/$action")
        }
        return PendingIntent.getBroadcast(
            context,
            (2_000_000 + timerId * 10 + salt).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notificationId(timerId: Long): Int = 3000 + timerId.toInt()

    private fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.timer_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.timer_channel_description)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 400, 600)
                setSound(sound, attrs)
                setBypassDnd(true)
            }
        )
    }
}
