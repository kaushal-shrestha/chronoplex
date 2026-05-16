package com.zoneanchor.ui.alarms

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zoneanchor.ZoneAnchorApp
import com.zoneanchor.alarm.NotificationHelper
import com.zoneanchor.model.ZonedAlarm
import com.zoneanchor.ui.components.daysSummary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AlarmsScreen(onEdit: (Int?) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as ZoneAnchorApp
    val viewModel: AlarmsViewModel = viewModel(factory = AlarmsViewModel.factory(app))
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val lastAction by viewModel.lastAction.collectAsStateWithLifecycle()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AlarmStatusPanel(
                exactAllowed = app.alarmScheduler.canScheduleExactAlarms(),
                notificationsAllowed = NotificationHelper.canPostNotifications(context),
                lastAction = lastAction,
                onEnableExact = {
                    ContextCompat.startActivity(context, app.alarmScheduler.exactAlarmSettingsIntent(), null)
                },
            )
        }
        if (alarms.isEmpty()) {
            item {
                Text("No alarms yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(alarms, key = { it.id }) { alarm ->
                AlarmCard(
                    alarm = alarm,
                    onEdit = { onEdit(alarm.id) },
                    onCancel = { viewModel.cancel(alarm.id) },
                )
            }
        }
    }
}

@Composable
private fun AlarmStatusPanel(
    exactAllowed: Boolean,
    notificationsAllowed: Boolean,
    lastAction: String,
    onEnableExact: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Alarm readiness", style = MaterialTheme.typography.titleMedium)
            Text(
                if (exactAllowed) "Exact alarms enabled." else "Exact alarm permission is needed.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!exactAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Button(onClick = onEnableExact) { Text("Enable exact alarms") }
            }
            Text(
                if (notificationsAllowed) "Notifications allowed." else "Notifications are not allowed yet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (lastAction.isNotBlank()) Text(lastAction)
        }
    }
}

@Composable
private fun AlarmCard(
    alarm: ZonedAlarm,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(alarm.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    "%d:%02d".format(alarm.hour, alarm.minute),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(daysSummary(alarm.daysOfWeekMask), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(alarm.zoneId, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${if (alarm.soundMode == ZonedAlarm.SOUND_SILENT) "Silent" else "Default sound"} · " +
                        if (alarm.vibrate) "Vibrate" else "No vibration",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(nextText(alarm), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit alarm")
            }
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Delete, contentDescription = "Cancel alarm")
            }
        }
    }
}

private fun nextText(alarm: ZonedAlarm): String {
    if (alarm.nextTriggerAtMillis <= 0L) return "Next: not scheduled"
    val zone = ZoneId.of(ZonedAlarm.validZoneId(alarm.zoneId))
    val next = Instant.ofEpochMilli(alarm.nextTriggerAtMillis).atZone(zone)
    return "Next: ${nextFormat.format(next)}"
}

private val nextFormat = DateTimeFormatter.ofPattern("EEE, MMM d h:mm a z")
