package com.zoneanchor.alarm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PublicOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.text.format.DateFormat
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.zoneanchor.alarm.R
import com.zoneanchor.alarm.data.AlarmZoneSource
import com.zoneanchor.alarm.domain.DayMask
import com.zoneanchor.alarm.ui.AlarmEditViewModel
import com.zoneanchor.alarm.ZoneAnchorApp
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlarmEditScreen(
    vm: AlarmEditViewModel,
    onPickZone: (restrictToAdded: Boolean) -> Unit,
    selectedZoneFromPicker: String?,
    onZoneConsumed: () -> Unit,
    onClose: () -> Unit,
) {
    LaunchedEffect(selectedZoneFromPicker) {
        if (!selectedZoneFromPicker.isNullOrBlank()) {
            vm.setZone(selectedZoneFromPicker)
            onZoneConsumed()
        }
    }

    val s by vm.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val is24Hour = remember(context) { DateFormat.is24HourFormat(context) }
    val firstDay by (context.applicationContext as ZoneAnchorApp).container.settings.firstDayOfWeek
        .collectAsState(initial = DayOfWeek.MONDAY)

    val timeState = rememberTimePickerState(initialHour = s.hour, initialMinute = s.minute, is24Hour = is24Hour)
    LaunchedEffect(timeState.hour, timeState.minute) {
        vm.setTime(timeState.hour, timeState.minute)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (s.id == 0L) stringResource(R.string.add_alarm) else stringResource(R.string.edit_alarm)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { vm.save(onClose) },
                        enabled = s.zoneId.isNotBlank(),
                    ) {
                        Icon(Icons.Default.Check, contentDescription = stringResource(R.string.save))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TimePicker(state = timeState)
                }
            }

            item {
                Column {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val sources = listOf(AlarmZoneSource.ADDED_CLOCKS, AlarmZoneSource.ALL_ZONES)
                        sources.forEachIndexed { i, src ->
                            SegmentedButton(
                                selected = s.zoneSource == src,
                                onClick = { vm.setZoneSource(src) },
                                shape = SegmentedButtonDefaults.itemShape(i, sources.size),
                            ) {
                                Text(when (src) {
                                    AlarmZoneSource.ADDED_CLOCKS -> stringResource(R.string.zone_source_added)
                                    AlarmZoneSource.ALL_ZONES -> stringResource(R.string.zone_source_all)
                                })
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPickZone(s.zoneSource == AlarmZoneSource.ADDED_CLOCKS) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.PublicOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(stringResource(R.string.time_zone), style = MaterialTheme.typography.labelMedium)
                                Text(
                                    if (s.zoneId.isBlank()) stringResource(R.string.select_time_zone) else s.zoneId,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = s.label,
                    onValueChange = vm::setLabel,
                    label = { Text(stringResource(R.string.label)) },
                    placeholder = { Text(stringResource(R.string.optional)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                Text(stringResource(R.string.repeat), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    orderedDays(firstDay).forEach { d ->
                        DayChip(
                            letter = letterFor(d),
                            selected = DayMask.contains(s.daysMask, d),
                            fullName = fullDayName(d),
                            onClick = { vm.toggleDay(d) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = s.daysMask == 0,
                        onClick = { vm.setDaysMask(0) },
                        label = { Text(stringResource(R.string.repeat_once)) },
                    )
                    FilterChip(
                        selected = s.daysMask == DayMask.WEEKDAYS,
                        onClick = { vm.setDaysMask(DayMask.WEEKDAYS) },
                        label = { Text(stringResource(R.string.repeat_weekdays)) },
                    )
                    FilterChip(
                        selected = s.daysMask == DayMask.WEEKENDS,
                        onClick = { vm.setDaysMask(DayMask.WEEKENDS) },
                        label = { Text(stringResource(R.string.repeat_weekends)) },
                    )
                    FilterChip(
                        selected = s.daysMask == DayMask.EVERY_DAY,
                        onClick = { vm.setDaysMask(DayMask.EVERY_DAY) },
                        label = { Text(stringResource(R.string.repeat_every_day)) },
                    )
                }
            }

            item {
                SwitchRow(
                    label = stringResource(R.string.sound),
                    sublabel = if (s.soundEnabled) stringResource(R.string.sound_default) else stringResource(R.string.sound_silent),
                    checked = s.soundEnabled,
                    onCheckedChange = vm::setSound,
                )
            }
            item {
                SwitchRow(
                    label = stringResource(R.string.vibration),
                    sublabel = if (s.vibrationEnabled) stringResource(R.string.vibration_on) else stringResource(R.string.vibration_off),
                    checked = s.vibrationEnabled,
                    onCheckedChange = vm::setVibration,
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    sublabel: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(
                sublabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DayChip(letter: String, selected: Boolean, fullName: String, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val borderColor = if (selected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outline
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = fullName },
        contentAlignment = Alignment.Center,
    ) {
        Text(letter, fontWeight = FontWeight.SemiBold, color = fg, fontSize = 15.sp)
    }
}

private fun letterFor(d: DayOfWeek): String = when (d) {
    DayOfWeek.MONDAY -> "M"
    DayOfWeek.TUESDAY -> "T"
    DayOfWeek.WEDNESDAY -> "W"
    DayOfWeek.THURSDAY -> "T"
    DayOfWeek.FRIDAY -> "F"
    DayOfWeek.SATURDAY -> "S"
    DayOfWeek.SUNDAY -> "S"
}

@Composable
private fun fullDayName(d: DayOfWeek): String = when (d) {
    DayOfWeek.MONDAY -> stringResource(R.string.day_monday)
    DayOfWeek.TUESDAY -> stringResource(R.string.day_tue)
    DayOfWeek.WEDNESDAY -> stringResource(R.string.day_wed)
    DayOfWeek.THURSDAY -> stringResource(R.string.day_thu)
    DayOfWeek.FRIDAY -> stringResource(R.string.day_fri)
    DayOfWeek.SATURDAY -> stringResource(R.string.day_sat)
    DayOfWeek.SUNDAY -> stringResource(R.string.day_sunday)
}

private fun orderedDays(first: DayOfWeek): List<DayOfWeek> {
    val all = DayOfWeek.values().toList()
    val idx = all.indexOf(first)
    return all.drop(idx) + all.take(idx)
}
