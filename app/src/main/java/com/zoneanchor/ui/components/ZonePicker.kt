package com.zoneanchor.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zoneanchor.model.ClockEntry
import java.time.ZoneId

@Composable
fun ZonePicker(
    zoneId: String,
    onZoneSelected: (String) -> Unit,
    allowedZones: List<ClockEntry>? = null,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text(zoneId, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = { open = true }) { Text("Choose") }
    }
    if (open) {
        ZonePickerDialog(
            selectedZone = zoneId,
            allowedZones = allowedZones,
            onDismiss = { open = false },
            onZoneSelected = {
                onZoneSelected(it)
                open = false
            },
        )
    }
}

@Composable
private fun ZonePickerDialog(
    selectedZone: String,
    allowedZones: List<ClockEntry>?,
    onDismiss: () -> Unit,
    onZoneSelected: (String) -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf("") }
    val zoneRows by remember(filter, allowedZones) {
        mutableStateOf(zoneRows(filter, allowedZones))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select time zone") },
        text = {
            Column {
                OutlinedTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("Search") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(zoneRows, key = { it.zoneId }) { row ->
                        ListItem(
                            headlineContent = { Text(row.displayName) },
                            supportingContent = {
                                if (row.displayName != row.zoneId) Text(row.zoneId)
                            },
                            trailingContent = {
                                if (row.zoneId == selectedZone) Text("Selected")
                            },
                            modifier = Modifier.clickable { onZoneSelected(row.zoneId) },
                        )
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Done") } },
    )
}

private fun zoneRows(filter: String, allowedZones: List<ClockEntry>?): List<ClockEntry> {
    if (allowedZones != null) return allowedZones
    val all = ZoneId.getAvailableZoneIds().sorted()
    val query = filter.trim()
    if (query.isEmpty()) {
        val pinned = listOf(ZoneId.systemDefault().id, "America/New_York", "UTC").distinct()
        return (pinned + all.filterNot { it in pinned }).map { ClockEntry(it) }
    }
    return all.filter { it.contains(query, ignoreCase = true) }.take(100).map { ClockEntry(it) }
}
