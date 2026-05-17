package com.chronoplex.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PublicOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.chronoplex.app.R
import com.chronoplex.app.ui.ClockEditViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockEditScreen(
    vm: ClockEditViewModel,
    onPickZone: () -> Unit,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (s.id == 0L) stringResource(R.string.add_clock) else stringResource(R.string.edit_clock)) },
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
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onPickZone)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.PublicOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(0.dp))
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(stringResource(R.string.time_zone), style = MaterialTheme.typography.labelMedium)
                        Text(
                            if (s.zoneId.isBlank()) stringResource(R.string.select_time_zone) else s.zoneId,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

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
