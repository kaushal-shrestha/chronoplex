package com.chronoplex.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chronoplex.app.R
import com.chronoplex.app.domain.Group
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Picker dialog letting the user move an item between groups (including "no group")
 * or create a new group inline.
 */
@Composable
fun MoveToGroupDialog(
    currentGroupId: Long?,
    groups: List<Group>,
    onMove: (Long?) -> Unit,
    onCreateAndMove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var creating by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.move_to_group)) },
        text = {
            Column {
                // "Ungrouped" option
                GroupOptionRow(
                    label = stringResource(R.string.ungrouped),
                    selected = currentGroupId == null,
                    onClick = { onMove(null) },
                )
                groups.forEach { g ->
                    GroupOptionRow(
                        label = g.name,
                        selected = currentGroupId == g.id,
                        onClick = { onMove(g.id) },
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (creating) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newGroupName,
                            onValueChange = { newGroupName = it },
                            label = { Text(stringResource(R.string.new_group_name)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = {
                                if (newGroupName.isNotBlank()) {
                                    onCreateAndMove(newGroupName.trim())
                                }
                            },
                            enabled = newGroupName.isNotBlank(),
                        ) {
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.save))
                        }
                        IconButton(onClick = { creating = false; newGroupName = "" }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                        }
                    }
                } else {
                    TextButton(onClick = { creating = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.size(4.dp))
                        Text(stringResource(R.string.new_group))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun GroupOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}

/**
 * List + create + rename + delete dialog for the groups of a single tab type.
 *
 * Optionally takes a [memberCount] lookup so the delete confirmation can say how
 * many items will become Ungrouped. If null, a generic message is used.
 */
@Composable
fun ManageGroupsDialog(
    groups: List<Group>,
    onCreate: (String) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    onReorder: (List<Long>) -> Unit,
    onDismiss: () -> Unit,
    memberCount: ((Long) -> Int)? = null,
) {
    var newName by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<Group?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<Group?>(null) }
    // Local mirror of the group list so the LazyColumn shows the drag in flight.
    var local by remember(groups.map { it.id }) { mutableStateOf(groups) }
    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        onMove = { from, to ->
            val fromId = from.key as? Long ?: return@rememberReorderableLazyListState
            val toId = to.key as? Long ?: return@rememberReorderableLazyListState
            local = local.toMutableList().apply {
                val fromIdx = indexOfFirst { it.id == fromId }
                val toIdx = indexOfFirst { it.id == toId }
                if (fromIdx in indices && toIdx in indices) add(toIdx, removeAt(fromIdx))
            }
            onReorder(local.map { it.id })
        },
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manage_groups)) },
        text = {
            Column {
                if (local.isEmpty()) {
                    Text(
                        stringResource(R.string.no_groups_yet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(state = lazyListState) {
                        items(local, key = { it.id }) { g ->
                            ReorderableItem(reorderState, key = g.id) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Default.DragHandle,
                                        contentDescription = stringResource(R.string.reorder),
                                        modifier = Modifier.draggableHandle(),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.size(8.dp))
                                    Text(g.name, modifier = Modifier.weight(1f))
                                    IconButton(onClick = {
                                        renameTarget = g
                                        renameText = g.name
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.rename))
                                    }
                                    IconButton(onClick = { deleteTarget = g }) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = stringResource(R.string.delete),
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text(stringResource(R.string.new_group_name)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = {
                            if (newName.isNotBlank()) {
                                onCreate(newName.trim())
                                newName = ""
                            }
                        },
                        enabled = newName.isNotBlank(),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
        },
    )

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(stringResource(R.string.rename_group)) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text(stringResource(R.string.label)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameText.isNotBlank()) onRename(target.id, renameText.trim())
                    renameTarget = null
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    deleteTarget?.let { target ->
        val count = memberCount?.invoke(target.id)
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.delete_group)) },
            text = {
                Text(
                    if (count != null)
                        stringResource(R.string.delete_group_with_count, target.name, count)
                    else
                        stringResource(R.string.delete_group_message, target.name)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target.id)
                    deleteTarget = null
                }) {
                    Text(
                        stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

/** Collapsible header row for a named group. */
@Composable
fun GroupHeader(
    group: Group,
    itemCount: Int,
    onToggleCollapsed: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleCollapsed)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            if (group.collapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            group.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        Text(
            "$itemCount",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Collapsible header for items that don't belong to any group. */
@Composable
fun UngroupedHeader(itemCount: Int, collapsed: Boolean, onToggleCollapsed: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleCollapsed)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            if (collapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            Icons.Outlined.FolderOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.ungrouped),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            "$itemCount",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Group picker used by the per-item editor screens. Includes an inline
 * "+ New group" affordance — submitting creates the group and immediately
 * selects it for the in-progress edit (via [onCreateAndSelect]).
 */
@Composable
fun GroupPickerDialog(
    currentGroupId: Long?,
    groups: List<Group>,
    onSelect: (Long?) -> Unit,
    onCreateAndSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var creating by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.move_to_group)) },
        text = {
            Column {
                GroupOptionRow(
                    label = stringResource(R.string.ungrouped),
                    selected = currentGroupId == null,
                    onClick = { onSelect(null) },
                )
                groups.forEach { g ->
                    GroupOptionRow(
                        label = g.name,
                        selected = currentGroupId == g.id,
                        onClick = { onSelect(g.id) },
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (creating) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newGroupName,
                            onValueChange = { newGroupName = it },
                            label = { Text(stringResource(R.string.new_group_name)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = {
                                if (newGroupName.isNotBlank()) {
                                    onCreateAndSelect(newGroupName.trim())
                                    creating = false
                                    newGroupName = ""
                                }
                            },
                            enabled = newGroupName.isNotBlank(),
                        ) { Icon(Icons.Default.Check, contentDescription = stringResource(R.string.save)) }
                        IconButton(onClick = { creating = false; newGroupName = "" }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                        }
                    }
                } else {
                    TextButton(onClick = { creating = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.size(4.dp))
                        Text(stringResource(R.string.new_group))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
        },
    )
}
