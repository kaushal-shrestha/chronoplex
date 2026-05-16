package com.zoneanchor.ui.alarms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zoneanchor.ZoneAnchorApp
import com.zoneanchor.model.ClockEntry
import com.zoneanchor.model.ZonedAlarm
import com.zoneanchor.ui.components.DayOfWeekPicker
import com.zoneanchor.ui.components.ZonePicker
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditorScreen(alarmId: Int?, onDone: () -> Unit) {
    val app = LocalContext.current.applicationContext as ZoneAnchorApp
    val viewModel: AlarmsViewModel = viewModel(factory = AlarmsViewModel.factory(app))
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val clocks by viewModel.clocks.collectAsStateWithLifecycle()
    val lastAction by viewModel.lastAction.collectAsStateWithLifecycle()
    val existing = alarms.firstOrNull { it.id == alarmId }
    var initialized by rememberSaveable(alarmId) { mutableStateOf(false) }
    var label by rememberSaveable(alarmId) { mutableStateOf("") }
    var zoneId by rememberSaveable(alarmId) { mutableStateOf(clocks.firstOrNull()?.zoneId ?: ZoneId.systemDefault().id) }
    var hour by rememberSaveable(alarmId) { mutableIntStateOf(ZonedAlarm.DEFAULT_HOUR) }
    var minute by rememberSaveable(alarmId) { mutableIntStateOf(ZonedAlarm.DEFAULT_MINUTE) }
    var daysMask by rememberSaveable(alarmId) { mutableIntStateOf(ZonedAlarm.ALL_DAYS) }
    var soundMode by rememberSaveable(alarmId) { mutableStateOf(ZonedAlarm.SOUND_DEFAULT) }
    var vibrate by rememberSaveable(alarmId) { mutableStateOf(true) }
    var useClockZones by rememberSaveable(alarmId) { mutableStateOf(clocks.isNotEmpty()) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(existing?.id, clocks.size) {
        if (!initialized && existing != null) {
            label = existing.label
            zoneId = existing.zoneId
            hour = existing.hour
            minute = existing.minute
            daysMask = existing.daysOfWeekMask
            soundMode = existing.soundMode
            vibrate = existing.vibrate
            useClockZones = clocks.any { it.zoneId == existing.zoneId }
            initialized = true
        } else if (!initialized && alarmId == null && clocks.isNotEmpty()) {
            zoneId = clocks.first().zoneId
            useClockZones = true
            initialized = true
        }
    }

    LaunchedEffect(useClockZones, clocks) {
        if (useClockZones && clocks.isNotEmpty() && clocks.none { it.zoneId == zoneId }) {
            zoneId = clocks.first().zoneId
        }
    }

    Column(
        modifier = Modifier.padding(PaddingValues(16.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(if (alarmId == null) "Add alarm" else "Edit alarm", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text("Alarm label") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        TimeRow(hour = hour, minute = minute, onClick = { showTimePicker = true })
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Use clocks list", modifier = Modifier.weight(1f))
            Switch(
                checked = useClockZones && clocks.isNotEmpty(),
                enabled = clocks.isNotEmpty(),
                onCheckedChange = { useClockZones = it },
            )
        }
        ZonePicker(
            zoneId = zoneId,
            onZoneSelected = { zoneId = it },
            allowedZones = if (useClockZones && clocks.isNotEmpty()) clocks else null,
        )
        Text("Days", style = MaterialTheme.typography.titleMedium)
        DayOfWeekPicker(selectedMask = daysMask, onSelectedMaskChange = { daysMask = it })
        Text("Sound", style = MaterialTheme.typography.titleMedium)
        SoundRow("Default", ZonedAlarm.SOUND_DEFAULT, soundMode) { soundMode = it }
        SoundRow("Silent", ZonedAlarm.SOUND_SILENT, soundMode) { soundMode = it }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Vibration", modifier = Modifier.weight(1f))
            Switch(checked = vibrate, onCheckedChange = { vibrate = it })
        }
        if (lastAction.isNotBlank()) Text(lastAction, color = MaterialTheme.colorScheme.error)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onDone, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = {
                    viewModel.save(alarmId, label, zoneId, hour, minute, daysMask, soundMode, vibrate, onDone)
                },
                modifier = Modifier.weight(1f),
            ) { Text("Save") }
        }
    }
    if (showTimePicker) {
        TimePickerDialog(hour, minute, onDismiss = { showTimePicker = false }) { h, m ->
            hour = h
            minute = m
            showTimePicker = false
        }
    }
}

@Composable
private fun TimeRow(hour: Int, minute: Int, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text("Time  %d:%02d".format(hour, minute))
    }
}

@Composable
private fun SoundRow(label: String, value: String, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onSelect(value) }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected == value, onClick = { onSelect(value) })
        Text(label)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    hour: Int,
    minute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select time") },
        text = { TimePicker(state = state) },
        confirmButton = {
            Button(onClick = { onConfirm(state.hour, state.minute) }) { Text("Done") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
