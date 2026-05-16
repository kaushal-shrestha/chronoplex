package com.zoneanchor.alarm.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.zoneanchor.alarm.R
import com.zoneanchor.alarm.data.AlarmZoneSource
import com.zoneanchor.alarm.domain.AppearanceMode
import com.zoneanchor.alarm.domain.ThemePalette
import com.zoneanchor.alarm.ui.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel) {
    val state by vm.state.collectAsState()
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_settings)) }) },
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
                            onClick = { vm.setAppearance(mode) },
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
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ThemePalette.values().forEach { p ->
                        FilterChip(
                            selected = state.palette == p,
                            onClick = { vm.setPalette(p) },
                            label = { Text(p.displayName) },
                        )
                    }
                }
            }
            item {
                SectionLabel(R.string.zone_source)
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
        }
    }
}

@Composable
private fun AlarmZoneSourceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.height(0.dp))
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
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
