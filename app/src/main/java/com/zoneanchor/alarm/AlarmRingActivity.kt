package com.zoneanchor.alarm

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zoneanchor.ZoneAnchorApp
import com.zoneanchor.model.AppPalette
import com.zoneanchor.model.AppearanceMode
import com.zoneanchor.model.ZonedAlarm
import com.zoneanchor.ui.theme.ZoneAnchorTheme
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

class AlarmRingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        val alarmId = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, ZonedAlarm.DEFAULT_ID)
        val app = application as ZoneAnchorApp
        setContent {
            val palette by app.settingsRepository.paletteFlow.collectAsStateWithLifecycle(AppPalette.DAYBREAK)
            val appearance by app.settingsRepository.appearanceFlow.collectAsStateWithLifecycle(AppearanceMode.SYSTEM)
            var alarm by remember { mutableStateOf<ZonedAlarm?>(null) }
            val scope = rememberCoroutineScope()
            LaunchedEffect(alarmId) { alarm = app.alarmRepository.find(alarmId) }
            ZoneAnchorTheme(palette, appearance) {
                val current = alarm
                if (current == null) {
                    LoadingRing()
                } else {
                    RingContent(
                        alarm = current,
                        onSnooze = {
                            scope.launch {
                                app.alarmScheduler.cancelSystemOnly(current.id)
                                app.alarmScheduler.scheduleOneOff(current, Instant.now().plusSeconds(9 * 60L))
                                NotificationHelper.cancel(this@AlarmRingActivity, current.id)
                                finish()
                            }
                        },
                        onDismiss = {
                            scope.launch {
                                app.alarmScheduler.scheduleNext(current)
                                NotificationHelper.cancel(this@AlarmRingActivity, current.id)
                                finish()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingRing() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Alarm")
    }
}

@Composable
private fun RingContent(alarm: ZonedAlarm, onSnooze: () -> Unit, onDismiss: () -> Unit) {
    val zone = ZoneId.of(ZonedAlarm.validZoneId(alarm.zoneId))
    val now = ZonedDateTime.now(zone)
    val zonedTime = DateTimeFormatter.ofPattern("h:mm a z").format(now)
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(alarm.displayName, style = MaterialTheme.typography.headlineSmall)
        Text(
            "%d:%02d".format(alarm.hour, alarm.minute),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
        )
        Text("It is $zonedTime in ${alarm.zoneId}.")
        Row(Modifier.fillMaxWidth().padding(top = 32.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onSnooze, modifier = Modifier.weight(1f)) {
                Text("Snooze 9 min")
            }
            Button(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text("Dismiss")
            }
        }
    }
}
