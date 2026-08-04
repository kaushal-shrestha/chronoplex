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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class AlarmActivity : ComponentActivity() {

    private val state = MutableStateFlow<Alarm?>(null)
    private val clockLabelState = MutableStateFlow<String?>(null)

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
                val clockLabel by clockLabelState.collectAsState()
                AlarmScreen(
                    alarm = alarm,
                    clockLabel = clockLabel,
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
            val alarm = app.container.alarmRepo.getById(alarmId)
            state.value = alarm
            clockLabelState.value = alarm?.clockId
                ?.let { app.container.clockRepo.getById(it) }
                ?.label
                ?.takeIf { it.isNotBlank() }
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
    clockLabel: String?,
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
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        contentColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.padding(18.dp).size(64.dp),
                        )
                    }
                    Spacer(Modifier.height(28.dp))
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
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (alarm != null) {
                        val anchorLabel = clockLabel ?: alarm.zoneId
                        if (alarm.label.isNotBlank()) {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                alarm.label,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            anchorLabel,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (clockLabel != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                alarm.zoneId,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            )
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    stringResource(R.string.snooze_for),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    listOf(1, 5, 10).forEach { mins ->
                        OutlinedButton(
                            onClick = { onSnooze(mins) },
                            modifier = Modifier.weight(1f).height(60.dp),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(
                                1.5.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.75f),
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                contentColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Text(
                                stringResource(R.string.snooze_minutes, mins),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(96.dp),
                    shape = RoundedCornerShape(30.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.dismiss),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
