package com.zoneanchor.alarm.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoneanchor.alarm.R
import com.zoneanchor.alarm.alarm.AlarmScheduler
import com.zoneanchor.alarm.domain.Alarm
import com.zoneanchor.alarm.domain.DayMask
import com.zoneanchor.alarm.ui.AlarmsViewModel
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(
    vm: AlarmsViewModel,
    permissionBanner: @Composable () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Alarm) -> Unit,
) {
    val alarms by vm.alarms.collectAsState()
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_alarms)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_alarm))
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { permissionBanner() }
                if (alarms.isEmpty()) {
                    item { EmptyState(R.string.empty_alarms_title, R.string.empty_alarms_body) }
                } else {
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmRow(
                            alarm = alarm,
                            onClick = { onEdit(alarm) },
                            onToggle = { vm.toggleEnabled(alarm) },
                            onDelete = { vm.delete(alarm) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmRow(
    alarm: Alarm,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val timeFmt = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val timeText = remember(alarm.hour, alarm.minute) {
        timeFmt.format(LocalTime.of(alarm.hour, alarm.minute))
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        timeText,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (alarm.enabled) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (alarm.label.isNotBlank()) {
                        Text(
                            alarm.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        alarm.zoneId,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = alarm.enabled, onCheckedChange = { onToggle() })
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    repeatLabel(alarm),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (alarm.enabled) {
                    Text(
                        nextFireText(alarm),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun repeatLabel(alarm: Alarm): String {
    if (alarm.isOneShot) return stringResource(R.string.repeat_once)
    val mask = alarm.daysMask
    if (mask == DayMask.EVERY_DAY) return stringResource(R.string.repeat_every_day)
    if (mask == DayMask.WEEKDAYS) return stringResource(R.string.repeat_weekdays)
    if (mask == DayMask.WEEKENDS) return stringResource(R.string.repeat_weekends)
    // Collect names up-front so the join lambda doesn't invoke @Composable functions.
    val names = alarm.daysOfWeek.sortedBy { it.value }.map { shortDayName(it) }
    return names.joinToString(", ")
}

@Composable
private fun shortDayName(d: java.time.DayOfWeek): String = when (d) {
    java.time.DayOfWeek.MONDAY -> stringResource(R.string.day_mon)
    java.time.DayOfWeek.TUESDAY -> stringResource(R.string.day_tue)
    java.time.DayOfWeek.WEDNESDAY -> stringResource(R.string.day_wed)
    java.time.DayOfWeek.THURSDAY -> stringResource(R.string.day_thu)
    java.time.DayOfWeek.FRIDAY -> stringResource(R.string.day_fri)
    java.time.DayOfWeek.SATURDAY -> stringResource(R.string.day_sat)
    java.time.DayOfWeek.SUNDAY -> stringResource(R.string.day_sun)
}

@Composable
private fun nextFireText(alarm: Alarm): String {
    val next = AlarmScheduler.nextTriggerMillis(alarm) ?: return stringResource(R.string.never_fires)
    val zone = runCatching { ZoneId.of(alarm.zoneId) }.getOrElse { ZoneId.systemDefault() }
    val zoned = ZonedDateTime.ofInstant(Instant.ofEpochMilli(next), zone)
    val durLabel = humanDuration(Duration.between(Instant.now(), Instant.ofEpochMilli(next)))
    val dayLabel = DateTimeFormatter.ofPattern("EEE h:mm a").format(zoned)
    return stringResource(R.string.next_fires, "$dayLabel ($durLabel)")
}

private fun humanDuration(d: Duration): String {
    if (d.isNegative || d.isZero) return "now"
    val totalMin = d.toMinutes()
    val h = totalMin / 60
    val m = totalMin % 60
    return when {
        h >= 24 -> "${h / 24}d ${h % 24}h"
        h > 0 -> "${h}h ${m}m"
        else -> "${m}m"
    }
}
