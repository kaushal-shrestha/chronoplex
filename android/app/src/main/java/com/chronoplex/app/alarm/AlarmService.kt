package com.chronoplex.app.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.chronoplex.app.R
import com.chronoplex.app.ChronoplexApp
import com.chronoplex.app.domain.Alarm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AlarmService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var ringtone: Ringtone? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentAlarmId: Long = -1L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
                if (alarmId < 0) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                currentAlarmId = alarmId
                startRinging(alarmId)
            }
            ACTION_STOP -> {
                stopRinging()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startRinging(alarmId: Long) {
        scope.launch {
            val app = applicationContext as ChronoplexApp
            val alarm = app.container.alarmRepo.getById(alarmId) ?: run { stopSelf(); return@launch }

            // Alarm has fired — no longer snoozed. Clear the row + the snoozed notification.
            if (alarm.isSnoozed()) {
                app.container.alarmRepo.setSnoozeUntil(alarmId, null)
            }
            SnoozeNotifier.cancel(applicationContext, alarmId)

            acquireWakeLock()
            val clockLabel = alarm.clockId
                ?.let { app.container.clockRepo.getById(it) }
                ?.label
                ?.takeIf { it.isNotBlank() }
            startForeground(NOTIFICATION_ID, buildNotification(alarm, clockLabel))

            if (alarm.soundEnabled) playRingtone()
            if (alarm.vibrationEnabled) startVibration()

            launchFullScreen(alarm.id)
        }
    }

    private fun stopRinging() {
        ringtone?.stop()
        ringtone = null
        stopVibration()
        releaseWakeLock()
        scope.coroutineContext[Job]?.cancel()
    }

    private fun playRingtone() {
        val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ringtone = RingtoneManager.getRingtone(this, uri)?.apply {
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) isLooping = true
            play()
        }
    }

    private fun startVibration() {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        val pattern = longArrayOf(0, 700, 500, 700, 500)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    private fun stopVibration() {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator?.cancel()
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "chronoplex:alarm-$currentAlarmId",
        ).apply {
            setReferenceCounted(false)
            acquire(10 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun ensureChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        val existing = nm.getNotificationChannel(CHANNEL_ID)
        if (existing == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.alarm_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = getString(R.string.alarm_channel_description)
                setBypassDnd(true)
                setSound(null, null) // we play sound ourselves
                enableVibration(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(alarm: Alarm, clockLabel: String?): android.app.Notification {
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmActivity.EXTRA_ALARM_ID, alarm.id)
        }
        val fullScreenPi = PendingIntent.getActivity(
            this,
            alarm.id.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val snoozePi = alarmActionPendingIntent(alarm.id, AlarmReceiver.ACTION_SNOOZE, "snooze")
        val dismissPi = alarmActionPendingIntent(alarm.id, AlarmReceiver.ACTION_DISMISS, "dismiss")
        val title = alarm.label.ifBlank { getString(R.string.notification_alarm_title) }
        val anchorLabel = clockLabel ?: alarm.zoneId
        val text = getString(R.string.notification_alarm_text, anchorLabel)
        val expandedText = if (clockLabel == null) {
            text
        } else {
            getString(R.string.notification_alarm_text_with_zone, clockLabel, alarm.zoneId)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPi, true)
            .setContentIntent(fullScreenPi)
            .addAction(0, getString(R.string.notification_snooze_5), snoozePi)
            .addAction(0, getString(R.string.dismiss), dismissPi)
            .build()
    }

    private fun alarmActionPendingIntent(alarmId: Long, action: String, path: String): PendingIntent {
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            this.action = action
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            data = android.net.Uri.parse("chronoplex://alarm-action/$path/$alarmId")
        }
        val requestOffset = when (action) {
            AlarmReceiver.ACTION_SNOOZE -> 10_000
            AlarmReceiver.ACTION_DISMISS -> 20_000
            else -> 30_000
        }
        return PendingIntent.getBroadcast(
            this,
            requestOffset + alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun launchFullScreen(alarmId: Long) {
        val intent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmActivity.EXTRA_ALARM_ID, alarmId)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRinging()
        scope.cancel()
    }

    companion object {
        const val ACTION_START = "com.chronoplex.app.service.START"
        const val ACTION_STOP = "com.chronoplex.app.service.STOP"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val CHANNEL_ID = "alarms"
        private const val NOTIFICATION_ID = 1001
    }
}
