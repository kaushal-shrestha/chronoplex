package com.chronoplex.app.ui.screens

import com.chronoplex.app.ui.tappable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PublicOff
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chronoplex.app.R
import com.chronoplex.app.ui.ClockEditViewModel
import com.chronoplex.app.ui.rememberTapFeedback
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockEditSheet(
    vm: ClockEditViewModel,
    onPickZone: () -> Unit,
    onDismiss: () -> Unit,
) {
    val s by vm.state.collectAsState()
    val groups by vm.groups.collectAsState()
    val groupingEnabled by vm.groupingEnabled.collectAsState()
    var groupPickerOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

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
                    if (s.id == 0L) stringResource(R.string.add_clock) else stringResource(R.string.edit_clock),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
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
                OutlinedCard(modifier = Modifier.fillMaxWidth().tappable(onClick = onPickZone)) {
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

                if (groupingEnabled) {
                    val currentGroupName = groups.firstOrNull { it.id == s.groupId }?.name
                        ?: stringResource(R.string.ungrouped)
                    OutlinedCard(modifier = Modifier.fillMaxWidth().tappable { groupPickerOpen = true }) {
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
