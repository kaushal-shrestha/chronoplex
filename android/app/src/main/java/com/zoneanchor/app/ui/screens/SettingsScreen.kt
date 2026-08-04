package com.zoneanchor.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zoneanchor.app.ui.tappable
import com.zoneanchor.app.R
import com.zoneanchor.app.data.AlarmZoneDisplayMode
import com.zoneanchor.app.data.AlarmZoneSource
import com.zoneanchor.app.domain.AppearanceMode
import com.zoneanchor.app.domain.ThemePalette
import com.zoneanchor.app.ui.SettingsViewModel
import com.zoneanchor.app.ui.rememberTapFeedback
import com.zoneanchor.app.ui.rememberToggleFeedback
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel) {
    val state by vm.state.collectAsState()
    var aboutOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var paletteOpen by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_settings)) },
                actions = {
                    IconButton(onClick = rememberTapFeedback { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.about)) },
                            onClick = rememberTapFeedback { menuOpen = false; aboutOpen = true },
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                SectionLabel(R.string.appearance)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    AppearanceMode.values().forEachIndexed { i, mode ->
                        SegmentedButton(
                            selected = state.appearance == mode,
                            onClick = rememberTapFeedback { vm.setAppearance(mode) },
                            shape = SegmentedButtonDefaults.itemShape(i, AppearanceMode.values().size),
                        ) {
                            Text(when (mode) {
                                AppearanceMode.SYSTEM -> stringResource(R.string.appearance_system)
                                AppearanceMode.LIGHT -> stringResource(R.string.appearance_light)
                                AppearanceMode.DARK -> stringResource(R.string.appearance_dark)
                            })
                        }
                    }
                }
            }
            item {
                SectionLabel(R.string.theme_palette)
                PreferenceRow(
                    title = state.palette.displayName,
                    supportingText = state.palette.description,
                    leading = { PalettePreview(state.palette) },
                    onClick = { paletteOpen = true },
                )
            }
            item {
                SectionLabel(R.string.zone_source_settings)
                Column {
                    AlarmZoneSourceRow(
                        label = stringResource(R.string.zone_source_added),
                        selected = state.alarmZoneSource == AlarmZoneSource.ADDED_CLOCKS,
                        onClick = { vm.setZoneSource(AlarmZoneSource.ADDED_CLOCKS) },
                    )
                    AlarmZoneSourceRow(
                        label = stringResource(R.string.zone_source_all),
                        selected = state.alarmZoneSource == AlarmZoneSource.ALL_ZONES,
                        onClick = { vm.setZoneSource(AlarmZoneSource.ALL_ZONES) },
                    )
                }
            }
            item {
                SectionLabel(R.string.alarm_zone_display)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val options = listOf(AlarmZoneDisplayMode.CLOCK_LABEL, AlarmZoneDisplayMode.ZONE_ID)
                    options.forEachIndexed { i, mode ->
                        SegmentedButton(
                            selected = state.alarmZoneDisplay == mode,
                            onClick = rememberTapFeedback { vm.setAlarmZoneDisplay(mode) },
                            shape = SegmentedButtonDefaults.itemShape(i, options.size),
                        ) {
                            Text(when (mode) {
                                AlarmZoneDisplayMode.CLOCK_LABEL -> stringResource(R.string.alarm_zone_display_label)
                                AlarmZoneDisplayMode.ZONE_ID -> stringResource(R.string.alarm_zone_display_zone)
                            })
                        }
                    }
                }
            }
            item {
                SectionLabel(R.string.first_day_of_week)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val options = listOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY)
                    options.forEachIndexed { i, d ->
                        SegmentedButton(
                            selected = state.firstDayOfWeek == d,
                            onClick = rememberTapFeedback { vm.setFirstDayOfWeek(d) },
                            shape = SegmentedButtonDefaults.itemShape(i, options.size),
                        ) {
                            Text(when (d) {
                                DayOfWeek.MONDAY -> stringResource(R.string.day_monday)
                                DayOfWeek.SUNDAY -> stringResource(R.string.day_sunday)
                                else -> d.name
                            })
                        }
                    }
                }
            }
            item {
                SectionLabel(R.string.grouping_section)
                GroupingToggleRow(
                    label = stringResource(R.string.grouping_clocks),
                    checked = state.clocksGrouping,
                    onCheckedChange = vm::setClocksGrouping,
                )
                GroupingToggleRow(
                    label = stringResource(R.string.grouping_alarms),
                    checked = state.alarmsGrouping,
                    onCheckedChange = vm::setAlarmsGrouping,
                )
                GroupingToggleRow(
                    label = stringResource(R.string.grouping_timers),
                    checked = state.timersGrouping,
                    onCheckedChange = vm::setTimersGrouping,
                )
                GroupingToggleRow(
                    label = stringResource(R.string.grouping_stopwatches),
                    checked = state.stopwatchesGrouping,
                    onCheckedChange = vm::setStopwatchesGrouping,
                )
            }
        }
    }

    if (aboutOpen) {
        AboutDialog(onDismiss = { aboutOpen = false })
    }
    if (paletteOpen) {
        PaletteDialog(
            selected = state.palette,
            onSelect = { palette ->
                vm.setPalette(palette)
                paletteOpen = false
            },
            onDismiss = { paletteOpen = false },
        )
    }
}

@Composable
private fun PaletteDialog(
    selected: ThemePalette,
    onSelect: (ThemePalette) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_palette)) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(ThemePalette.values()) { palette ->
                    PaletteChoiceRow(
                        palette = palette,
                        selected = selected == palette,
                        onClick = { onSelect(palette) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = rememberTapFeedback(onDismiss)) {
                Text(stringResource(R.string.done))
            }
        },
    )
}

@Composable
private fun PaletteChoiceRow(
    palette: ThemePalette,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .tappable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = rememberTapFeedback(onClick))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        ) {
            Text(palette.displayName, style = MaterialTheme.typography.bodyLarge)
            Text(
                palette.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PalettePreview(palette)
    }
}

@Composable
private fun PalettePreview(palette: ThemePalette) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        palette.previewColors().forEach { color ->
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

private fun ThemePalette.previewColors(): List<Color> = when (this) {
    ThemePalette.Anchor -> listOf(Color(0xFF2E5FB7), Color(0xFF4A7AC2), Color(0xFF6F9BD8))
    ThemePalette.Daybreak -> listOf(Color(0xFF006C67), Color(0xFF2DD4BF), Color(0xFFB9F2EC))
    ThemePalette.Harbor -> listOf(Color(0xFF005B8C), Color(0xFF60A5FA), Color(0xFFBAE6FD))
    ThemePalette.Grove -> listOf(Color(0xFF237046), Color(0xFF4ADE80), Color(0xFFBBF7D0))
    ThemePalette.Ember -> listOf(Color(0xFFAD4543), Color(0xFFFB7185), Color(0xFFFECDD3))
    ThemePalette.Twilight -> listOf(Color(0xFF5C5BB0), Color(0xFFA78BFA), Color(0xFFDDD6FE))
    ThemePalette.Sunrise -> listOf(Color(0xFFE0664B), Color(0xFFE89461), Color(0xFFE9B872))
    ThemePalette.Forest -> listOf(Color(0xFF2F7D5E), Color(0xFF4F9C7C), Color(0xFF89B98F))
    ThemePalette.Slate -> listOf(Color(0xFF4C5664), Color(0xFF6C7585), Color(0xFF98A0AE))
    ThemePalette.Plum -> listOf(Color(0xFF7A3E8F), Color(0xFF9B5BB5), Color(0xFFC586D8))
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val versionName = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: ""
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Image(
                    painter = painterResource(R.mipmap.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp)),
                )
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    stringResource(R.string.about_version, versionName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.about_description),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    stringResource(R.string.about_copyright),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = rememberTapFeedback(onDismiss)) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

@Composable
private fun GroupingToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tappable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = rememberToggleFeedback(onCheckedChange))
    }
}

@Composable
private fun AlarmZoneSourceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tappable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = rememberTapFeedback(onClick))
        Spacer(Modifier.width(8.dp))
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun PreferenceRow(
    title: String,
    supportingText: String,
    leading: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .tappable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
        leadingContent = leading,
        headlineContent = {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        supportingContent = {
            Text(
                supportingText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@Composable
private fun SectionLabel(textRes: Int) {
    Text(
        stringResource(textRes),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}
