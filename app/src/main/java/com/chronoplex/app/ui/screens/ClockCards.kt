package com.chronoplex.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronoplex.app.R
import com.chronoplex.app.domain.Clock
import com.chronoplex.app.ui.rememberTapFeedback
import com.chronoplex.app.ui.tappable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimeConverterCard(
    clocks: List<Clock>,
    nowEpochMillis: Long,
    sourceClockId: Long?,
    pinnedEpochMillis: Long?,
    onSourceClockSelected: (Long?) -> Unit,
    onPinnedEpochMillisChange: (Long?) -> Unit,
    onClose: () -> Unit,
) {
    val sourceClock = clocks.firstOrNull { it.id == sourceClockId }
    val sourceZoneId = sourceClock?.zoneId ?: ZoneId.systemDefault().id
    val sourceLabel = sourceClock?.label ?: stringResource(R.string.device_time)
    val selectedEpochMillis = pinnedEpochMillis ?: nowEpochMillis
    val sourceTime = remember(sourceZoneId, selectedEpochMillis) {
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(selectedEpochMillis), ZoneId.of(sourceZoneId))
    }
    val timeFmt = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }
    var timePickerOpen by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.convert_time),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = rememberTapFeedback(onClose)) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                }
            }
            Text(
                stringResource(R.string.converter_reference_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.converter_when_it_is),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                        )
                        Text(
                            timeFmt.format(sourceTime),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "${sourceLabel} - ${dateFmt.format(sourceTime)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f),
                        )
                    }
                    TextButton(onClick = rememberTapFeedback { timePickerOpen = true }) {
                        Text(stringResource(R.string.pick_time))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            ) {
                AssistChip(
                    onClick = rememberTapFeedback { onPinnedEpochMillisChange(null) },
                    label = { Text(stringResource(R.string.converter_now)) },
                )
                AssistChip(
                    onClick = rememberTapFeedback {
                        onPinnedEpochMillisChange(sourceTime.minusHours(1).toInstant().toEpochMilli())
                    },
                    label = { Text(stringResource(R.string.previous_hour)) },
                )
                AssistChip(
                    onClick = rememberTapFeedback {
                        onPinnedEpochMillisChange(sourceTime.plusHours(1).toInstant().toEpochMilli())
                    },
                    label = { Text(stringResource(R.string.next_hour)) },
                )
                AssistChip(
                    onClick = rememberTapFeedback {
                        onPinnedEpochMillisChange(sourceTime.plusDays(1).toInstant().toEpochMilli())
                    },
                    label = { Text(stringResource(R.string.next_day)) },
                )
            }

            if (clocks.isEmpty()) {
                Text(
                    stringResource(R.string.converter_no_clocks),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    clocks.forEach { clock ->
                        val converted = remember(clock.zoneId, selectedEpochMillis) {
                            ZonedDateTime.ofInstant(
                                Instant.ofEpochMilli(selectedEpochMillis),
                                ZoneId.of(clock.zoneId),
                            )
                        }
                        ConverterResultRow(
                            label = clock.label,
                            time = timeFmt.format(converted),
                            dayLabel = relativeDayLabel(sourceTime.toLocalDate(), converted.toLocalDate()),
                            isSource = clock.id == sourceClockId,
                            onClick = { onSourceClockSelected(clock.id) },
                        )
                    }
                }
            }
        }
    }

    if (timePickerOpen) {
        val pickerState = rememberTimePickerState(
            initialHour = sourceTime.hour,
            initialMinute = sourceTime.minute,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { timePickerOpen = false },
            title = { Text(stringResource(R.string.pick_time)) },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(onClick = rememberTapFeedback {
                    val updated = sourceTime
                        .withHour(pickerState.hour)
                        .withMinute(pickerState.minute)
                        .withSecond(0)
                        .withNano(0)
                    onPinnedEpochMillisChange(updated.toInstant().toEpochMilli())
                    timePickerOpen = false
                }) {
                    Text(stringResource(R.string.done))
                }
            },
            dismissButton = {
                TextButton(onClick = rememberTapFeedback { timePickerOpen = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun ConverterResultRow(
    label: String,
    time: String,
    dayLabel: String,
    isSource: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .tappable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (isSource) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        },
        border = if (isSource) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
        } else {
            null
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSource) FontWeight.Bold else FontWeight.Normal,
                )
                Text(
                    if (isSource) stringResource(R.string.converter_source) else dayLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                time,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun relativeDayLabel(sourceDate: LocalDate, convertedDate: LocalDate): String {
    return when (ChronoUnit.DAYS.between(sourceDate, convertedDate)) {
        -1L -> stringResource(R.string.yesterday)
        0L -> stringResource(R.string.today)
        1L -> stringResource(R.string.tomorrow)
        else -> DateTimeFormatter.ofPattern("MMM d").format(convertedDate)
    }
}

@Composable
internal fun LocalTimeCard(
    now: ZonedDateTime,
    converterOpen: Boolean,
    isConverterSource: Boolean,
    onUseAsConverterSource: () -> Unit,
) {
    val fmt = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .tappable(onClick = {
                if (converterOpen) onUseAsConverterSource()
            }),
        colors = CardDefaults.cardColors(
            containerColor = if (converterOpen && !isConverterSource) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            contentColor = if (converterOpen && !isConverterSource) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            },
        ),
        border = if (converterOpen && isConverterSource) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.device_time),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    ZoneId.systemDefault().id,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    dateFmt.format(now),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    fmt.format(now),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
internal fun ClockRow(
    clock: Clock,
    nowEpochMillis: Long,
    converterOpen: Boolean,
    isConverterSource: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val zoneTime = remember(clock.zoneId, nowEpochMillis) {
        runCatching {
            ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(nowEpochMillis),
                ZoneId.of(clock.zoneId),
            )
        }.getOrNull()
    }
    val fmt = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .tappable(onLongClick = onLongClick, onClick = onClick),
        colors = if (converterOpen && isConverterSource) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        } else {
            CardDefaults.cardColors()
        },
        border = if (converterOpen && isConverterSource) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    clock.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    clock.zoneId,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (converterOpen && isConverterSource) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                if (zoneTime != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        dateFmt.format(zoneTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (converterOpen && isConverterSource) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    zoneTime?.let { fmt.format(it) } ?: "-",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            IconButton(onClick = rememberTapFeedback(onDelete)) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = stringResource(R.string.delete),
                    tint = if (converterOpen && isConverterSource) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}
