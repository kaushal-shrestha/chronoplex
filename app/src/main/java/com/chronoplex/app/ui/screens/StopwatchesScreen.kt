package com.chronoplex.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import com.chronoplex.app.ui.tappable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import com.chronoplex.app.R
import com.chronoplex.app.domain.Group
import com.chronoplex.app.domain.Stopwatch
import com.chronoplex.app.domain.StopwatchState
import com.chronoplex.app.ui.StopwatchesViewModel
import com.chronoplex.app.ui.longPressable
import com.chronoplex.app.ui.rememberTapFeedback
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopwatchesScreen(vm: StopwatchesViewModel) {
    val stopwatches by vm.stopwatches.collectAsState()
    val groups by vm.groups.collectAsState()
    val groupingEnabled by vm.groupingEnabled.collectAsState()
    // 10 Hz tick for smooth centisecond display when any stopwatch is running.
    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(100L)
        }
    }
    var renameTarget by remember { mutableStateOf<Stopwatch?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var manageGroupsOpen by remember { mutableStateOf(false) }
    var moveTarget by remember { mutableStateOf<Stopwatch?>(null) }
    var actionsTarget by remember { mutableStateOf<Stopwatch?>(null) }
    var ungroupedCollapsed by remember { mutableStateOf(false) }
    var reorderMode by remember { mutableStateOf(false) }
    var confirmClearAll by remember { mutableStateOf(false) }
    if (stopwatches.isEmpty() && reorderMode) reorderMode = false
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedLabel = stringResource(R.string.stopwatch_deleted)
    val undoLabel = stringResource(R.string.undo)
    fun handleDelete(stopwatch: Stopwatch) {
        scope.launch {
            // Snapshot laps BEFORE deletion; delete() cascades to laps in the repo.
            val laps = vm.observeLaps(stopwatch.id).first()
            vm.delete(stopwatch)
            val r = snackbarHostState.showSnackbar(
                message = deletedLabel,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Long,
            )
            if (r == SnackbarResult.ActionPerformed) vm.restore(stopwatch, laps)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (reorderMode) stringResource(R.string.reorder)
                        else stringResource(R.string.tab_stopwatches)
                    )
                },
                actions = {
                    if (reorderMode) {
                        IconButton(onClick = rememberTapFeedback { reorderMode = false }) {
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.done))
                        }
                    } else {
                        IconButton(onClick = rememberTapFeedback { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            if (stopwatches.size >= 2) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.reorder)) },
                                    onClick = rememberTapFeedback { menuOpen = false; reorderMode = true },
                                )
                            }
                            if (groupingEnabled) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.manage_groups)) },
                                    onClick = rememberTapFeedback { menuOpen = false; manageGroupsOpen = true },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.clear_all_stopwatches)) },
                                onClick = rememberTapFeedback { menuOpen = false; confirmClearAll = true },
                                enabled = stopwatches.isNotEmpty(),
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (!reorderMode) {
                val onFabClick = rememberTapFeedback { vm.addStopwatch() }
                FloatingActionButton(onClick = onFabClick) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_stopwatch))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (reorderMode) {
                if (groupingEnabled) {
                    StopwatchesGroupedReorderableList(
                        stopwatches = stopwatches,
                        groups = groups,
                        onReorder = { vm.reorderItems(it) },
                    )
                } else {
                    StopwatchesReorderableList(stopwatches = stopwatches, onReorder = { vm.reorderItems(it) })
                }
                return@Box
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (stopwatches.isEmpty()) {
                    item { EmptyState(R.string.empty_stopwatches_title, R.string.empty_stopwatches_body) }
                } else if (!groupingEnabled) {
                    items(stopwatches, key = { it.id }) { sw ->
                        StopwatchCard(
                            stopwatch = sw, nowMillis = now, vm = vm,
                            onLongClick = { actionsTarget = sw },
                            onDelete = { handleDelete(sw) },
                        )
                    }
                } else {
                    val byGroup = stopwatches.groupBy { it.groupId }
                    groups.forEach { g ->
                        val members = byGroup[g.id].orEmpty()
                        item(key = "group-${g.id}") {
                            GroupHeader(g, members.size, onToggleCollapsed = { vm.toggleCollapsed(g) })
                        }
                        if (!g.collapsed) {
                            items(members, key = { "g${g.id}-${it.id}" }) { sw ->
                                StopwatchCard(
                                    stopwatch = sw, nowMillis = now, vm = vm,
                                    onLongClick = { actionsTarget = sw },
                                    onDelete = { handleDelete(sw) },
                                )
                            }
                        }
                    }
                    val ungrouped = byGroup[null].orEmpty()
                    if (ungrouped.isNotEmpty()) {
                        item(key = "ungrouped") {
                            UngroupedHeader(
                                itemCount = ungrouped.size,
                                collapsed = ungroupedCollapsed,
                                onToggleCollapsed = { ungroupedCollapsed = !ungroupedCollapsed },
                            )
                        }
                        if (!ungroupedCollapsed) {
                            items(ungrouped, key = { "u-${it.id}" }) { sw ->
                                StopwatchCard(
                                    stopwatch = sw, nowMillis = now, vm = vm,
                                    onLongClick = { actionsTarget = sw },
                                    onDelete = { handleDelete(sw) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    renameTarget?.let { target ->
        RenameDialog(
            initial = target.label,
            onDismiss = { renameTarget = null },
            onConfirm = { newLabel ->
                vm.rename(target, newLabel)
                renameTarget = null
            },
        )
    }

    actionsTarget?.let { sw ->
        val renameLabel = stringResource(R.string.rename)
        val moveLabel = stringResource(R.string.move_to_group)
        val deleteLabel = stringResource(R.string.delete)
        RowActionsSheet(
            title = sw.label.ifBlank { null },
            actions = buildList {
                add(RowAction(
                    label = renameLabel,
                    icon = Icons.Default.Edit,
                    onClick = { actionsTarget = null; renameTarget = sw },
                ))
                if (groupingEnabled) {
                    add(RowAction(
                        label = moveLabel,
                        icon = Icons.Default.Folder,
                        onClick = { actionsTarget = null; moveTarget = sw },
                    ))
                }
                add(RowAction(
                    label = deleteLabel,
                    icon = Icons.Default.DeleteOutline,
                    tint = MaterialTheme.colorScheme.error,
                    onClick = { actionsTarget = null; handleDelete(sw) },
                ))
            },
            onDismiss = { actionsTarget = null },
        )
    }

    if (manageGroupsOpen) {
        ManageGroupsDialog(
            groups = groups,
            onCreate = { vm.createGroup(it) },
            onRename = { id, name -> vm.renameGroup(id, name) },
            onDelete = { vm.deleteGroup(it) },
            onReorder = { vm.reorderGroups(it) },
            onDismiss = { manageGroupsOpen = false },
            memberCount = { gid -> stopwatches.count { it.groupId == gid } },
        )
    }
    moveTarget?.let { target ->
        MoveToGroupDialog(
            currentGroupId = target.groupId,
            groups = groups,
            onMove = { gid -> vm.moveToGroup(target.id, gid); moveTarget = null },
            onCreateAndMove = { name ->
                vm.createAndAssign(target.id, name)
                moveTarget = null
            },
            onDismiss = { moveTarget = null },
        )
    }

    if (confirmClearAll) {
        AlertDialog(
            onDismissRequest = { confirmClearAll = false },
            title = { Text(stringResource(R.string.clear_all_stopwatches)) },
            text = { Text(stringResource(R.string.clear_all_stopwatches_message)) },
            confirmButton = {
                TextButton(onClick = rememberTapFeedback {
                    confirmClearAll = false
                    vm.deleteAll()
                }) { Text(stringResource(R.string.clear)) }
            },
            dismissButton = {
                TextButton(onClick = rememberTapFeedback { confirmClearAll = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun StopwatchCard(
    stopwatch: Stopwatch,
    nowMillis: Long,
    vm: StopwatchesViewModel,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var lapsExpanded by remember { mutableStateOf(false) }
    val laps by vm.observeLaps(stopwatch.id).collectAsState(initial = emptyList())
    val elapsed = stopwatch.elapsedMillis(nowMillis)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .longPressable(onLongClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stopwatch.label.ifBlank { stringResource(R.string.tab_stopwatches) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (stopwatch.label.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                    )
                    Text(
                        formatCenti(elapsed),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        stateLabel(stopwatch.state),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = rememberTapFeedback {
                    when (stopwatch.state) {
                        StopwatchState.RUNNING -> vm.pause(stopwatch)
                        else -> vm.start(stopwatch)
                    }
                }) {
                    Icon(
                        when (stopwatch.state) {
                            StopwatchState.RUNNING -> Icons.Default.Pause
                            else -> Icons.Default.PlayArrow
                        },
                        contentDescription = null,
                    )
                }
                IconButton(
                    onClick = rememberTapFeedback { vm.lap(stopwatch) },
                    enabled = stopwatch.state == StopwatchState.RUNNING,
                ) {
                    Icon(Icons.Default.Timer, contentDescription = stringResource(R.string.lap))
                }
                IconButton(onClick = rememberTapFeedback { vm.reset(stopwatch) }) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.reset))
                }
                IconButton(onClick = rememberTapFeedback(onDelete)) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (laps.isNotEmpty()) {
                    Spacer(Modifier.weight(1f))
                    Row(
                        modifier = Modifier
                            .tappable { lapsExpanded = !lapsExpanded }
                            .padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${laps.size} ${stringResource(R.string.laps)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Icon(
                            if (lapsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            AnimatedVisibility(visible = lapsExpanded && laps.isNotEmpty()) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    // Show newest at top.
                    laps.asReversed().forEachIndexed { idxFromTop, lap ->
                        val prevTotal = if (lap.lapNumber > 1) {
                            laps.firstOrNull { it.lapNumber == lap.lapNumber - 1 }?.totalElapsedMillis ?: 0L
                        } else 0L
                        val split = lap.totalElapsedMillis - prevTotal
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(
                                stringResource(R.string.lap_number, lap.lapNumber),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                formatCenti(split),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                            Text(
                                formatCenti(lap.totalElapsedMillis),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StopwatchesReorderableList(stopwatches: List<Stopwatch>, onReorder: (List<Long>) -> Unit) {
    var local by remember(stopwatches.map { it.id }) { mutableStateOf(stopwatches) }
    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        local = local.toMutableList().apply {
            val fromIdx = indexOfFirst { it.id == from.key as Long }
            val toIdx = indexOfFirst { it.id == to.key as Long }
            if (fromIdx in indices && toIdx in indices) add(toIdx, removeAt(fromIdx))
        }
        onReorder(local.map { it.id })
    }
    Column {
        Text(
            stringResource(R.string.reorder_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(local, key = { it.id }) { sw ->
                ReorderableItem(reorderState, key = sw.id) {
                    Card(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.DragHandle,
                                contentDescription = stringResource(R.string.reorder),
                                modifier = Modifier.draggableHandle().size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    sw.label.ifBlank { stringResource(R.string.tab_stopwatches) },
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    stateLabel(sw.state),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StopwatchesGroupedReorderableList(
    stopwatches: List<Stopwatch>,
    groups: List<Group>,
    onReorder: (List<Long>) -> Unit,
) {
    var local by remember(stopwatches.map { it.id }) { mutableStateOf(stopwatches) }
    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        onMove = { from, to ->
            val fromId = from.key as? Long ?: return@rememberReorderableLazyListState
            val toId = to.key as? Long ?: return@rememberReorderableLazyListState
            val fromItem = local.firstOrNull { it.id == fromId } ?: return@rememberReorderableLazyListState
            val toItem = local.firstOrNull { it.id == toId } ?: return@rememberReorderableLazyListState
            if (fromItem.groupId != toItem.groupId) return@rememberReorderableLazyListState

            local = local.toMutableList().apply {
                val fromIdx = indexOfFirst { it.id == fromId }
                val toIdx = indexOfFirst { it.id == toId }
                if (fromIdx in indices && toIdx in indices) add(toIdx, removeAt(fromIdx))
            }
            val affectedIds = local.filter { it.groupId == fromItem.groupId }.map { it.id }
            onReorder(affectedIds)
        },
    )
    Column {
        Text(
            stringResource(R.string.reorder_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val byGroup = local.groupBy { it.groupId }
            groups.forEach { g ->
                val members = byGroup[g.id].orEmpty()
                item(key = "h-${g.id}") {
                    GroupHeader(g.copy(collapsed = false), members.size, onToggleCollapsed = { })
                }
                items(members, key = { it.id }) { sw ->
                    ReorderableItem(reorderState, key = sw.id) {
                        StopwatchDragRow(stopwatch = sw)
                    }
                }
            }
            val ungrouped = byGroup[null].orEmpty()
            if (ungrouped.isNotEmpty()) {
                item(key = "h-ungrouped") {
                    UngroupedHeader(itemCount = ungrouped.size, collapsed = false, onToggleCollapsed = { })
                }
                items(ungrouped, key = { it.id }) { sw ->
                    ReorderableItem(reorderState, key = sw.id) {
                        StopwatchDragRow(stopwatch = sw)
                    }
                }
            }
        }
    }
}

@Composable
private fun sh.calvin.reorderable.ReorderableCollectionItemScope.StopwatchDragRow(stopwatch: Stopwatch) {
    Card(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.DragHandle,
                contentDescription = stringResource(R.string.reorder),
                modifier = Modifier.draggableHandle().size(28.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stopwatch.label.ifBlank { stringResource(R.string.tab_stopwatches) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (stopwatch.label.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                )
                Text(
                    stateLabel(stopwatch.state),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RenameDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_stopwatch)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.label)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = rememberTapFeedback { onConfirm(text) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = rememberTapFeedback(onDismiss)) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun stateLabel(state: StopwatchState): String = when (state) {
    StopwatchState.IDLE -> stringResource(R.string.stopwatch_state_idle)
    StopwatchState.RUNNING -> stringResource(R.string.stopwatch_state_running)
    StopwatchState.PAUSED -> stringResource(R.string.stopwatch_state_paused)
}

/** Format millis as H:MM:SS.cc (or M:SS.cc if under an hour). Centiseconds only. */
private fun formatCenti(millis: Long): String {
    val total = millis.coerceAtLeast(0L)
    val centi = (total / 10) % 100
    val seconds = (total / 1000) % 60
    val minutes = (total / 60_000) % 60
    val hours = total / 3_600_000
    return if (hours > 0) {
        "%d:%02d:%02d.%02d".format(hours, minutes, seconds, centi)
    } else {
        "%d:%02d.%02d".format(minutes, seconds, centi)
    }
}
