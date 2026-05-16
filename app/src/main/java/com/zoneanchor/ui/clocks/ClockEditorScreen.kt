package com.zoneanchor.ui.clocks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zoneanchor.ZoneAnchorApp
import com.zoneanchor.ui.components.ZonePicker
import java.time.ZoneId

@Composable
fun ClockEditorScreen(zoneId: String?, onDone: () -> Unit) {
    val app = LocalContext.current.applicationContext as ZoneAnchorApp
    val viewModel: ClocksViewModel = viewModel(factory = ClocksViewModel.factory(app))
    val clocks by viewModel.clocks.collectAsStateWithLifecycle()
    val existing = clocks.firstOrNull { it.zoneId == zoneId }
    var initialized by rememberSaveable(zoneId) { mutableStateOf(false) }
    var selectedZone by rememberSaveable(zoneId) { mutableStateOf(zoneId ?: ZoneId.systemDefault().id) }
    var label by rememberSaveable(zoneId) { mutableStateOf("") }

    LaunchedEffect(existing?.zoneId) {
        if (!initialized && existing != null) {
            selectedZone = existing.zoneId
            label = existing.label
            initialized = true
        }
    }

    Column(
        modifier = Modifier.padding(PaddingValues(16.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(if (zoneId == null) "Add clock" else "Edit clock", style = MaterialTheme.typography.headlineSmall)
        ZonePicker(zoneId = selectedZone, onZoneSelected = { selectedZone = it })
        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text("Custom label") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onDone, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Button(
                onClick = { viewModel.save(zoneId, selectedZone, label, onDone) },
                modifier = Modifier.weight(1f),
            ) {
                Text("Save")
            }
        }
    }
}
