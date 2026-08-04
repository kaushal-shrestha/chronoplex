package com.zoneanchor.app.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.zoneanchor.app.ui.tappable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.PublicOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoneanchor.app.ZoneAnchorApp
import com.zoneanchor.app.R
import com.zoneanchor.app.data.AlarmZoneSource
import com.zoneanchor.app.domain.AlarmRepeatType
import com.zoneanchor.app.domain.Clock
import com.zoneanchor.app.domain.DayMask
import com.zoneanchor.app.ui.AlarmEditState
import com.zoneanchor.app.ui.AlarmEditViewModel
import com.zoneanchor.app.ui.rememberTapFeedback
import com.zoneanchor.app.ui.rememberToggleFeedback
import java.time.DayOfWeek
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlarmEditSheet(
    vm: AlarmEditViewModel,
    onPickZone: (restrictToAdded: Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val s by vm.state.collectAsState()
    val clocks by vm.clocks.collectAsState()
    val groups by vm.groups.collectAsState()
    val groupingEnabled by vm.groupingEnabled.collectAsState()
    var groupPickerOpen by remember { mutableStateOf(false) }
    var attachedLabelPickerOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val is24Hour = remember(context) { DateFormat.is24HourFormat(context) }
    val firstDay by (context.applicationContext as ZoneAnchorApp).container.settings.firstDayOfWeek
        .collectAsState(initial = DayOfWeek.MONDAY)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var advancedRepeatOpen by remember { mutableStateOf(false) }

    val timeState = rememberTimePickerState(initialHour = s.hour, initialMinute = s.minute, is24Hour = is24Hour)
    // Re-uses our standard tap feedback (click sound + haptic) for any TimePicker tick:
    // dial drag, dial tap, AM/PM toggle, and tapping the hour/minute display.
    val pickerFeedback = rememberTapFeedback {}
    var firstTimeTick by remember { mutableStateOf(true) }
    LaunchedEffect(timeState.hour, timeState.minute) {
        vm.setTime(timeState.hour, timeState.minute)
        if (firstTimeTick) firstTimeTick = false
        else pickerFeedback()
    }
    var firstSelTick by remember { mutableStateOf(true) }
    LaunchedEffect(timeState.selection) {
        if (firstSelTick) firstSelTick = false
        else pickerFeedback()
    }

    fun dismissAnimated() {
        scope.launch {
            sheetState.hide()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = rememberTapFeedback(::dismissAnimated)) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                }
                Text(
                    if (s.id == 0L) stringResource(R.string.add_alarm) else stringResource(R.string.edit_alarm),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                if (s.id != 0L) {
                    TextButton(onClick = rememberTapFeedback {
                        val id = s.id
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                            onDelete(id)
                        }
                    }) {
                        Text(
                            stringResource(R.string.delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                TextButton(
                    onClick = rememberTapFeedback { vm.save { dismissAnimated() } },
                    enabled = s.zoneId.isNotBlank(),
                ) {
                    Text(stringResource(R.string.save))
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TimePicker(state = timeState)
                }

                OutlinedTextField(
                    value = s.label,
                    onValueChange = vm::setLabel,
                    label = { Text(stringResource(R.string.label)) },
                    placeholder = { Text(stringResource(R.string.optional)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                val attachedClock = clocks.firstOrNull { it.id == s.clockId }
                val manualZoneSelectionEnabled = attachedClock == null
                OutlinedCard(modifier = Modifier.fillMaxWidth().tappable { attachedLabelPickerOpen = true }) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(stringResource(R.string.attached_label), style = MaterialTheme.typography.labelMedium)
                        Text(
                            attachedClock?.label ?: stringResource(R.string.none),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }

                Column(modifier = Modifier.alpha(if (manualZoneSelectionEnabled) 1f else 0.42f)) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val sources = listOf(AlarmZoneSource.ADDED_CLOCKS, AlarmZoneSource.ALL_ZONES)
                        sources.forEachIndexed { i, src ->
                            SegmentedButton(
                                enabled = manualZoneSelectionEnabled,
                                selected = s.zoneSource == src,
                                onClick = rememberTapFeedback { vm.setZoneSource(src) },
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
                        modifier = if (manualZoneSelectionEnabled) {
                            Modifier
                                .fillMaxWidth()
                                .tappable { onPickZone(s.zoneSource == AlarmZoneSource.ADDED_CLOCKS) }
                        } else {
                            Modifier.fillMaxWidth()
                        }
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

                RepeatSection(
                    state = s,
                    vm = vm,
                    firstDay = firstDay,
                    advancedOpen = advancedRepeatOpen,
                    onAdvancedOpenChange = { advancedRepeatOpen = it },
                )

                SwitchRow(
                    label = stringResource(R.string.sound),
                    sublabel = if (s.soundEnabled) stringResource(R.string.sound_default) else stringResource(R.string.sound_silent),
                    checked = s.soundEnabled,
                    onCheckedChange = vm::setSound,
                )
                SwitchRow(
                    label = stringResource(R.string.vibration),
                    sublabel = if (s.vibrationEnabled) stringResource(R.string.vibration_on) else stringResource(R.string.vibration_off),
                    checked = s.vibrationEnabled,
                    onCheckedChange = vm::setVibration,
                )

                if (groupingEnabled) {
                    val currentGroupName = groups.firstOrNull { it.id == s.groupId }?.name
                        ?: stringResource(R.string.ungrouped)
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth().tappable { groupPickerOpen = true }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(stringResource(R.string.move_to_group), style = MaterialTheme.typography.labelMedium)
                                Text(currentGroupName, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (groupPickerOpen) {
        GroupPickerDialog(
            currentGroupId = s.groupId,
            groups = groups,
            onSelect = { vm.setGroupId(it); groupPickerOpen = false },
            onCreateAndSelect = { name -> vm.createAndSelectGroup(name); groupPickerOpen = false },
            onDismiss = { groupPickerOpen = false },
        )
    }

    if (attachedLabelPickerOpen) {
        AttachedLabelPickerDialog(
            clocks = clocks,
            selectedClockId = s.clockId,
            onSelect = {
                vm.setAttachedClock(it)
                attachedLabelPickerOpen = false
            },
            onDismiss = { attachedLabelPickerOpen = false },
        )
    }
}

@Composable
private fun AttachedLabelPickerDialog(
    clocks: List<Clock>,
    selectedClockId: Long?,
    onSelect: (Clock?) -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.attached_label)) },
        text = {
            Column {
                AttachedLabelOption(
                    label = stringResource(R.string.none),
                    sublabel = stringResource(R.string.attached_label_none_hint),
                    selected = selectedClockId == null,
                    onClick = { onSelect(null) },
                )
                clocks.forEach { clock ->
                    AttachedLabelOption(
                        label = clock.label,
                        sublabel = clock.zoneId,
                        selected = selectedClockId == clock.id,
                        onClick = { onSelect(clock) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = rememberTapFeedback(onDismiss)) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun AttachedLabelOption(
    label: String,
    sublabel: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().tappable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.RadioButton(selected = selected, onClick = rememberTapFeedback(onClick))
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                sublabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
        Switch(checked = checked, onCheckedChange = rememberToggleFeedback(onCheckedChange))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RepeatSection(
    state: AlarmEditState,
    vm: AlarmEditViewModel,
    firstDay: DayOfWeek,
    advancedOpen: Boolean,
    onAdvancedOpenChange: (Boolean) -> Unit,
) {
    val advancedActive = state.repeatType == AlarmRepeatType.MONTHLY_DAY ||
        state.repeatType == AlarmRepeatType.MONTHLY_WEEKDAY ||
        state.repeatInterval > 1
    Column {
        Text(stringResource(R.string.repeat), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        if (state.repeatType != AlarmRepeatType.MONTHLY_DAY &&
            state.repeatType != AlarmRepeatType.MONTHLY_WEEKDAY
        ) {
            DaySelector(
                firstDay = firstDay,
                selected = { DayMask.contains(state.daysMask, it) },
                onClick = { vm.toggleDay(it) },
            )
            Spacer(Modifier.height(8.dp))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.effectiveRepeatType == AlarmRepeatType.ONCE,
                onClick = rememberTapFeedback { vm.setDaysMask(0) },
                label = { Text(stringResource(R.string.repeat_once)) },
            )
            FilterChip(
                selected = state.repeatType == AlarmRepeatType.WEEKLY &&
                    state.repeatInterval == 1 &&
                    state.daysMask == DayMask.WEEKDAYS,
                onClick = rememberTapFeedback { vm.setDaysMask(DayMask.WEEKDAYS) },
                label = { Text(stringResource(R.string.repeat_weekdays)) },
            )
            FilterChip(
                selected = state.repeatType == AlarmRepeatType.WEEKLY &&
                    state.repeatInterval == 1 &&
                    state.daysMask == DayMask.WEEKENDS,
                onClick = rememberTapFeedback { vm.setDaysMask(DayMask.WEEKENDS) },
                label = { Text(stringResource(R.string.repeat_weekends)) },
            )
            FilterChip(
                selected = state.repeatType == AlarmRepeatType.WEEKLY &&
                    state.repeatInterval == 1 &&
                    state.daysMask == DayMask.EVERY_DAY,
                onClick = rememberTapFeedback { vm.setDaysMask(DayMask.EVERY_DAY) },
                label = { Text(stringResource(R.string.repeat_every_day)) },
            )
        }
        TextButton(
            onClick = rememberTapFeedback {
                if (!advancedOpen && state.effectiveRepeatType == AlarmRepeatType.ONCE) {
                    vm.setRepeatType(AlarmRepeatType.WEEKLY)
                }
                onAdvancedOpenChange(!advancedOpen)
            },
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Icon(
                if (advancedOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
            )
            Text(stringResource(if (advancedActive) R.string.repeat_advanced_active else R.string.repeat_advanced))
        }
        if (advancedOpen) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.repeatType == AlarmRepeatType.WEEKLY,
                        onClick = rememberTapFeedback { vm.setRepeatType(AlarmRepeatType.WEEKLY) },
                        label = { Text(stringResource(R.string.repeat_weekly)) },
                    )
                    FilterChip(
                        selected = state.repeatType == AlarmRepeatType.MONTHLY_DAY,
                        onClick = rememberTapFeedback { vm.setRepeatType(AlarmRepeatType.MONTHLY_DAY) },
                        label = { Text(stringResource(R.string.repeat_monthly_day)) },
                    )
                    FilterChip(
                        selected = state.repeatType == AlarmRepeatType.MONTHLY_WEEKDAY,
                        onClick = rememberTapFeedback { vm.setRepeatType(AlarmRepeatType.MONTHLY_WEEKDAY) },
                        label = { Text(stringResource(R.string.repeat_monthly_weekday)) },
                    )
                }
                IntervalRow(
                    interval = state.repeatInterval,
                    unit = stringResource(
                        if (state.repeatType == AlarmRepeatType.WEEKLY) R.string.repeat_weeks
                        else R.string.repeat_months
                    ),
                    onChange = vm::setRepeatInterval,
                )
                OutlinedTextField(
                    value = state.repeatStartDate,
                    onValueChange = vm::setRepeatStartDate,
                    label = { Text(stringResource(R.string.repeat_start_date)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                when (state.repeatType) {
                    AlarmRepeatType.MONTHLY_DAY -> MonthlyDayControls(state = state, vm = vm)
                    AlarmRepeatType.MONTHLY_WEEKDAY -> MonthlyWeekdayControls(
                        state = state,
                        vm = vm,
                        firstDay = firstDay,
                    )
                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun DaySelector(
    firstDay: DayOfWeek,
    selected: (DayOfWeek) -> Boolean,
    onClick: (DayOfWeek) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        orderedDays(firstDay).forEach { d ->
            DayChip(
                letter = letterFor(d),
                selected = selected(d),
                fullName = fullDayName(d),
                onClick = { onClick(d) },
            )
        }
    }
}

@Composable
private fun IntervalRow(interval: Int, unit: String, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.repeat_every), modifier = Modifier.weight(1f))
        IconButton(onClick = rememberTapFeedback { onChange(interval - 1) }) {
            Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.decrease))
        }
        Text(interval.toString(), style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = rememberTapFeedback { onChange(interval + 1) }) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.increase))
        }
        Text(unit, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MonthlyDayControls(state: AlarmEditState, vm: AlarmEditViewModel) {
    OutlinedTextField(
        value = state.monthlyDay.toString(),
        onValueChange = { value ->
            value.filter { it.isDigit() }.toIntOrNull()?.let(vm::setMonthlyDay)
        },
        label = { Text(stringResource(R.string.repeat_day_of_month)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthlyWeekdayControls(
    state: AlarmEditState,
    vm: AlarmEditViewModel,
    firstDay: DayOfWeek,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                1 to R.string.repeat_first,
                2 to R.string.repeat_second,
                3 to R.string.repeat_third,
                4 to R.string.repeat_fourth,
                -1 to R.string.repeat_last,
            ).forEach { (ordinal, label) ->
                FilterChip(
                    selected = state.monthlyOrdinal == ordinal,
                    onClick = rememberTapFeedback { vm.setMonthlyOrdinal(ordinal) },
                    label = { Text(stringResource(label)) },
                )
            }
        }
        DaySelector(
            firstDay = firstDay,
            selected = { it.value == state.monthlyWeekday },
            onClick = vm::setMonthlyWeekday,
        )
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
            .tappable(onClick = onClick)
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

private val AlarmEditState.effectiveRepeatType: AlarmRepeatType
    get() = if (repeatType == AlarmRepeatType.WEEKLY && daysMask == 0) {
        AlarmRepeatType.ONCE
    } else {
        repeatType
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
