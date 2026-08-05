package com.zoneanchor.app.timer

import android.app.Notification
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
import androidx.core.app.NotificationManagerCompat
import com.zoneanchor.app.ZoneAnchorApp
import com.zoneanchor.app.MainActivity
import com.zoneanchor.app.R
import com.zoneanchor.app.domain.Timer
import com.zoneanchor.app.domain.TimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val activeTimerIds = linkedSetOf<Long>()
    private var foregroundTimerId: Long? = null
    private var ringtone: Ringtone? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val timerId = intent?.getLongExtra(EXTRA_TIMER_ID, -1L) ?: -1L
        when (intent?.action) {
            ACTION_START -> if (timerId >= 0L) startTimerAlert(timerId)
            ACTION_STOP -> if (timerId >= 0L) stopTimerAlert(timerId)
        }
        return START_STICKY
    }

    private fun startTimerAlert(timerId: Long) {
        activeTimerIds.add(timerId)
        if (foregroundTimerId == null) foregroundTimerId = timerId

        postNotification(timerId = timerId, timer = null)
        startAlerting()

        scope.launch {
            val timer = withContext(Dispatchers.IO) {
                (applicationContext as ZoneAnchorApp).container.timerRepo.getById(timerId)
            }
            if (timer == null || timer.state != TimerState.FINISHED) {
                stopTimerAlert(timerId)
                return@launch
            }
            postNotification(timerId = timerId, timer = timer)
        }
    }

    private fun stopTimerAlert(timerId: Long) {
        val wasForeground = foregroundTimerId == timerId
        activeTimerIds.remove(timerId)

        if (activeTimerIds.isEmpty()) {
            stopAlerting()
            foregroundTimerId = null
            stopForegroundRemovingNotification()
            NotificationManagerCompat.from(this).cancel(notificationId(timerId))
            stopSelf()
            return
        }

        if (wasForeground) {
            val nextTimerId = activeTimerIds.first()
            foregroundTimerId = nextTimerId
            postNotification(timerId = nextTimerId, timer = null)
            scope.launch {
                val timer = withContext(Dispatchers.IO) {
                    (applicationContext as ZoneAnchorApp).container.timerRepo.getById(nextTimerId)
                }
                if (timer == null || timer.state != TimerState.FINISHED) {
                    stopTimerAlert(nextTimerId)
                    return@launch
                }
                postNotification(timerId = nextTimerId, timer = timer)
            }
        }
        NotificationManagerCompat.from(this).cancel(notificationId(timerId))
    }

    private fun postNotification(timerId: Long, timer: Timer?) {
        val notification = buildNotification(timerId, timer)
        if (foregroundTimerId == timerId) {
            startForeground(notificationId(timerId), notification)
        } else {
            NotificationManagerCompat.from(this).notify(notificationId(timerId), notification)
        }
    }

    private fun buildNotification(timerId: Long, timer: Timer?): Notification {
        val title = timer?.label?.ifBlank { getString(R.string.timer_finished_default_title) }
            ?: getString(R.string.timer_finished_default_title)
        val text = getString(R.string.timer_finished_body)

        val contentPi = PendingIntent.getActivity(
            this,
            timerId.toInt(),
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setTicker(title)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setLocalOnly(true)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setSilent(false)
            .setContentIntent(contentPi)
            .addAction(0, getString(R.string.add_minute), action(timerId, TimerReceiver.ACTION_ADD_MINUTE, 1))
            .addAction(0, getString(R.string.stop), action(timerId, TimerReceiver.ACTION_DISMISS, 2))
            .build()
    }

    private fun action(timerId: Long, action: String, salt: Int): PendingIntent {
        val intent = Intent(this, TimerReceiver::class.java).apply {
            this.action = action
            putExtra(TimerReceiver.EXTRA_TIMER_ID, timerId)
            data = android.net.Uri.parse("zoneanchor://timer/$timerId/$action")
        }
        return PendingIntent.getBroadcast(
            this,
            (2_000_000 + timerId * 10 + salt).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun startAlerting() {
        acquireWakeLock()
        if (ringtone == null) playRingtone()
        startVibration()
    }

    private fun stopAlerting() {
        ringtone?.stop()
        ringtone = null
        stopVibration()
        releaseWakeLock()
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
        val vibrator = vibrator()
        val pattern = longArrayOf(0, 600, 400, 600, 400)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    private fun stopVibration() {
        vibrator()?.cancel()
    }

    private fun vibrator(): Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private fun acquireWakeLock() {
        val existing = wakeLock
        if (existing?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "zoneanchor:timer-alert",
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
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.timer_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = getString(R.string.timer_channel_description)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
                setSound(null, null)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 400, 600)
            }
        )
    }

    private fun stopForegroundRemovingNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlerting()
        scope.cancel()
    }

    companion object {
        const val ACTION_START = "com.zoneanchor.app.timer.service.START"
        const val ACTION_STOP = "com.zoneanchor.app.timer.service.STOP"
        const val EXTRA_TIMER_ID = "extra_timer_id"
        private const val CHANNEL_ID = "timer_alerts_v3"

        fun start(context: Context, timerId: Long) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TIMER_ID, timerId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context, timerId: Long) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_STOP
                putExtra(EXTRA_TIMER_ID, timerId)
            }
            runCatching { context.startService(intent) }
            NotificationManagerCompat.from(context).cancel(notificationId(timerId))
        }

        fun notificationId(timerId: Long): Int = 3000 + timerId.toInt()
    }
}
