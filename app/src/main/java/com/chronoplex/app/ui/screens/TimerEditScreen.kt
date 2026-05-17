package com.chronoplex.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronoplex.app.R
import com.chronoplex.app.ui.DurationField
import com.chronoplex.app.ui.TimerEditViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TimerEditScreen(
    vm: TimerEditViewModel,
    onClose: () -> Unit,
) {
    val s by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (s.id == 0L) stringResource(R.string.add_timer) else stringResource(R.string.edit_timer)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { vm.save(onClose) },
                        enabled = s.isValid,
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
                Text(stringResource(R.string.presets), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PresetChip(R.string.preset_1m, 1 * 60_000L, vm)
                    PresetChip(R.string.preset_5m, 5 * 60_000L, vm)
                    PresetChip(R.string.preset_10m, 10 * 60_000L, vm)
                    PresetChip(R.string.preset_15m, 15 * 60_000L, vm)
                    PresetChip(R.string.preset_30m, 30 * 60_000L, vm)
                    PresetChip(R.string.preset_1h, 60 * 60_000L, vm)
                }
            }

            item {
                Text(stringResource(R.string.duration), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DurationCell(
                        value = s.hours,
                        unit = stringResource(R.string.hours_short),
                        focused = s.focusedField == DurationField.HOURS,
                        modifier = Modifier.weight(1f),
                        onClick = { vm.setFocus(DurationField.HOURS) },
                    )
                    DurationCell(
                        value = s.minutes,
                        unit = stringResource(R.string.minutes_short),
                        focused = s.focusedField == DurationField.MINUTES,
                        modifier = Modifier.weight(1f),
                        onClick = { vm.setFocus(DurationField.MINUTES) },
                    )
                    DurationCell(
                        value = s.seconds,
                        unit = stringResource(R.string.seconds_short),
                        focused = s.focusedField == DurationField.SECONDS,
                        modifier = Modifier.weight(1f),
                        onClick = { vm.setFocus(DurationField.SECONDS) },
                    )
                }
            }

            item {
                Keypad(
                    onDigit = vm::typeDigit,
                    onBackspace = vm::backspace,
                    onClear = vm::clearField,
                )
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
        }
    }
}

@Composable
private fun PresetChip(labelRes: Int, millis: Long, vm: TimerEditViewModel) {
    FilterChip(
        selected = false,
        onClick = { vm.setPresetMillis(millis) },
        label = { Text(stringResource(labelRes)) },
    )
}

@Composable
private fun DurationCell(
    value: Int,
    unit: String,
    focused: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val borderColor = if (focused) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outline
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .border(if (focused) 2.dp else 1.dp, borderColor, shape)
            .background(if (focused) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "%02d".format(value),
            fontSize = 36.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            unit,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            icon != null -> Icon(icon, contentDescription = iconDescription)
            text != null -> Text(text, fontSize = 22.sp, fontWeight = FontWeight.Medium)
        }
    }
}
