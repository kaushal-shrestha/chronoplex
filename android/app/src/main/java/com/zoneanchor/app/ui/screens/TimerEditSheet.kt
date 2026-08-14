package com.zoneanchor.app.ui.screens

import androidx.compose.foundation.background
import com.zoneanchor.app.ui.tappable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoneanchor.app.R
import com.zoneanchor.app.ui.TimerEditViewModel
import com.zoneanchor.app.ui.rememberTapFeedback
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TimerEditSheet(
    vm: TimerEditViewModel,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val s by vm.state.collectAsState()
    val groups by vm.groups.collectAsState()
    val groupingEnabled by vm.groupingEnabled.collectAsState()
    var groupPickerOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val canStartFromSheet = s.id == 0L || s.timerState.canStartFromEditSheet()

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
                    if (s.id == 0L) stringResource(R.string.add_timer) else stringResource(R.string.edit_timer),
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
                    onClick = rememberTapFeedback { vm.save(autoStart = false) { dismissAnimated() } },
                    enabled = s.isValid,
                ) {
                    Text(stringResource(R.string.save))
                }
                if (canStartFromSheet) {
                    TextButton(
                        onClick = rememberTapFeedback { vm.save(autoStart = true) { dismissAnimated() } },
                        enabled = s.isValid,
                    ) {
                        Text(
                            stringResource(R.string.start),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column {
                    Text(stringResource(R.string.presets), style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PresetChip(R.string.preset_1m, 1 * 60_000L, vm)
                        PresetChip(R.string.preset_5m, 5 * 60_000L, vm)
                        PresetChip(R.string.preset_10m, 10 * 60_000L, vm)
                        PresetChip(R.string.preset_15m, 15 * 60_000L, vm)
                        PresetChip(R.string.preset_30m, 30 * 60_000L, vm)
                        PresetChip(R.string.preset_1h, 60 * 60_000L, vm)
                    }
                }

                DurationReadout(
                    hours = s.hours,
                    minutes = s.minutes,
                    seconds = s.seconds,
                    hoursUnit = stringResource(R.string.hours_short),
                    minutesUnit = stringResource(R.string.minutes_short),
                    secondsUnit = stringResource(R.string.seconds_short),
                )

                Keypad(
                    onDigit = vm::typeDigit,
                    onBackspace = vm::backspace,
                    onClear = vm::clearField,
                )

                OutlinedTextField(
                    value = s.label,
                    onValueChange = vm::setLabel,
                    label = { Text(stringResource(R.string.label)) },
                    placeholder = { Text(stringResource(R.string.optional)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
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
}

@Composable
private fun PresetChip(labelRes: Int, millis: Long, vm: TimerEditViewModel) {
    FilterChip(
        selected = false,
        onClick = rememberTapFeedback { vm.setPresetMillis(millis) },
        label = { Text(stringResource(labelRes)) },
    )
}

@Composable
private fun DurationReadout(
    hours: Int,
    minutes: Int,
    seconds: Int,
    hoursUnit: String,
    minutesUnit: String,
    secondsUnit: String,
) {
    val hLit = hours > 0
    val mLit = hLit || minutes > 0
    val sLit = mLit || seconds > 0
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom,
    ) {
        TimeSegment(value = hours, unit = hoursUnit, lit = hLit)
        Spacer(Modifier.width(8.dp))
        TimeSegment(value = minutes, unit = minutesUnit, lit = mLit)
        Spacer(Modifier.width(8.dp))
        TimeSegment(value = seconds, unit = secondsUnit, lit = sLit)
    }
}

@Composable
private fun TimeSegment(value: Int, unit: String, lit: Boolean) {
    val color = if (lit) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.outline
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            "%02d".format(value),
            fontSize = 52.sp,
            fontWeight = FontWeight.Light,
            color = color,
        )
        Text(
            unit,
            fontSize = 18.sp,
            fontWeight = FontWeight.Light,
            color = color,
            modifier = Modifier.padding(start = 2.dp, bottom = 10.dp),
        )
    }
}

@Composable
private fun Keypad(
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        KeypadRow {
            KeypadKey(text = "1", modifier = Modifier.weight(1f), onClick = { onDigit(1) })
            KeypadKey(text = "2", modifier = Modifier.weight(1f), onClick = { onDigit(2) })
            KeypadKey(text = "3", modifier = Modifier.weight(1f), onClick = { onDigit(3) })
        }
        KeypadRow {
            KeypadKey(text = "4", modifier = Modifier.weight(1f), onClick = { onDigit(4) })
            KeypadKey(text = "5", modifier = Modifier.weight(1f), onClick = { onDigit(5) })
            KeypadKey(text = "6", modifier = Modifier.weight(1f), onClick = { onDigit(6) })
        }
        KeypadRow {
            KeypadKey(text = "7", modifier = Modifier.weight(1f), onClick = { onDigit(7) })
            KeypadKey(text = "8", modifier = Modifier.weight(1f), onClick = { onDigit(8) })
            KeypadKey(text = "9", modifier = Modifier.weight(1f), onClick = { onDigit(9) })
        }
        KeypadRow {
            KeypadKey(text = stringResource(R.string.keypad_clear), modifier = Modifier.weight(1f), onClick = onClear)
            KeypadKey(text = "0", modifier = Modifier.weight(1f), onClick = { onDigit(0) })
            KeypadKey(
                icon = Icons.Default.Backspace,
                modifier = Modifier.weight(1f),
                onClick = onBackspace,
                iconDescription = stringResource(R.string.keypad_backspace),
            )
        }
    }
}

@Composable
private fun KeypadRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun KeypadKey(
    text: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconDescription: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val tapWithFeedback = rememberTapFeedback(onClick)
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .tappable(onClick = tapWithFeedback)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            icon != null -> Icon(icon, contentDescription = iconDescription)
            text != null -> Text(text, fontSize = 22.sp, fontWeight = FontWeight.Medium)
        }
    }
}
