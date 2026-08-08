package com.zoneanchor.app.alarm

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.zoneanchor.app.R
import com.zoneanchor.app.ZoneAnchorApp
import com.zoneanchor.app.domain.Alarm
import com.zoneanchor.app.ui.theme.ZoneAnchorTheme
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
            val app = LocalContext.current.applicationContext as ZoneAnchorApp
            val appearance by app.container.settings.appearance.collectAsState(initial = com.zoneanchor.app.domain.AppearanceMode.SYSTEM)
            val palette by app.container.settings.palette.collectAsState(initial = com.zoneanchor.app.domain.ThemePalette.Anchor)
            ZoneAnchorTheme(appearance = appearance, palette = palette) {
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
        val app = applicationContext as ZoneAnchorApp
        lifecycleScope.launch {
            state.value = app.container.alarmRepo.getById(alarmId)
        }
    }

    private fun dismiss() {
        val app = applicationContext as ZoneAnchorApp
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
        val app = applicationContext as ZoneAnchorApp
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
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.42f))
            AlarmIdentity(alarm = alarm)
            Spacer(Modifier.weight(0.58f))
            AlarmActions(onDismiss = onDismiss, onSnooze = onSnooze)
        }
    }
}

@androidx.compose.runtime.Composable
private fun AlarmIdentity(alarm: Alarm?) {
    val now = remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(alarm?.zoneId) {
        while (true) {
            now.value = if (alarm != null) ZonedDateTime.now(ZoneId.of(alarm.zoneId)) else ZonedDateTime.now()
            delay(1000)
        }
    }
    val timeFormat = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val dateFormat = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(116.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)),
            ) {}
            Icon(
                Icons.Default.AccessTime,
                contentDescription = null,
                modifier = Modifier.size(62.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(30.dp))
        Text(
            text = timeFormat.format(now.value),
            fontSize = 72.sp,
            lineHeight = 76.sp,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = alarm?.zoneId ?: dateFormat.format(now.value),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        alarm?.label
            ?.takeIf { it.isNotBlank() }
            ?.let { label ->
                Spacer(Modifier.height(18.dp))
                Text(
                    text = label,
                    fontSize = 26.sp,
                    lineHeight = 32.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
    }
}

@androidx.compose.runtime.Composable
private fun AlarmActions(
    onDismiss: () -> Unit,
    onSnooze: (Int) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.74f),
        shape = RoundedCornerShape(32.dp),
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.snooze_for),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                listOf(1, 5, 10).forEach { mins ->
                    OutlinedButton(
                        onClick = { onSnooze(mins) },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) { Text(stringResource(R.string.snooze_minutes, mins)) }
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) { Text(stringResource(R.string.dismiss)) }
        }
    }
}
