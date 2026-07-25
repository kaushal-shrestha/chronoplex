package com.chronoplex.app.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.chronoplex.app.R
import com.chronoplex.app.ChronoplexApp
import com.chronoplex.app.domain.Alarm
import com.chronoplex.app.ui.theme.ChronoplexTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class AlarmActivity : ComponentActivity() {

    private val state = MutableStateFlow<Alarm?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            km.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Don't allow dismissal via back — force a deliberate dismiss/snooze.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        loadAlarm(alarmId)

        setContent {
            val app = LocalContext.current.applicationContext as ChronoplexApp
            val appearance by app.container.settings.appearance.collectAsState(initial = com.chronoplex.app.domain.AppearanceMode.SYSTEM)
            val palette by app.container.settings.palette.collectAsState(initial = com.chronoplex.app.domain.ThemePalette.Anchor)
            ChronoplexTheme(appearance = appearance, palette = palette) {
                val alarm by state.collectAsState()
                AlarmScreen(
                    alarm = alarm,
                    onDismiss = { dismiss() },
                    onSnooze = { minutes -> snooze(minutes) },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        loadAlarm(alarmId)
    }

    private fun loadAlarm(alarmId: Long) {
        if (alarmId < 0) { finish(); return }
        val app = applicationContext as ChronoplexApp
        lifecycleScope.launch {
            state.value = app.container.alarmRepo.getById(alarmId)
        }
    }

    private fun dismiss() {
        val app = applicationContext as ChronoplexApp
        val current = state.value ?: return finish()
        lifecycleScope.launch {
            // Reschedule next occurrence for recurring alarms; disable one-shots after firing.
            if (current.isOneShot) {
                app.container.alarmRepo.setEnabled(current.id, false)
                app.container.scheduler.cancel(current.id)
            } else {
                app.container.scheduler.schedule(current)
            }
            stopServiceAndFinish()
        }
    }

    private fun snooze(minutes: Int) {
        val app = applicationContext as ChronoplexApp
        val current = state.value ?: return finish()
        lifecycleScope.launch {
            app.container.scheduler.snooze(current, minutes = minutes)
            stopServiceAndFinish()
        }
    }

    private fun stopServiceAndFinish() {
        val stop = Intent(this, AlarmService::class.java).apply { action = AlarmService.ACTION_STOP }
        stopService(stop)
        finish()
    }

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
    }
}

@androidx.compose.runtime.Composable
private fun AlarmScreen(
    alarm: Alarm?,
    onDismiss: () -> Unit,
    onSnooze: (Int) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(24.dp))
                    val now = remember { mutableStateOf(ZonedDateTime.now()) }
                    LaunchedEffect(alarm?.zoneId) {
                        while (true) {
                            now.value = if (alarm != null) ZonedDateTime.now(ZoneId.of(alarm.zoneId)) else ZonedDateTime.now()
                            delay(1000)
                        }
                    }
                    val fmt = remember { DateTimeFormatter.ofPattern("h:mm a") }
                    Text(
                        fmt.format(now.value),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (alarm != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            alarm.zoneId,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (alarm.label.isNotBlank()) {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                alarm.label,
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.snooze_for),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(1, 5, 10).forEach { mins ->
                    OutlinedButton(
                        onClick = { onSnooze(mins) },
                        modifier = Modifier.weight(1f).height(56.dp),
                    ) { Text(stringResource(R.string.snooze_minutes, mins)) }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(64.dp).padding(vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) { Text(stringResource(R.string.dismiss)) }
        }
    }
}

