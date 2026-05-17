package com.chronoplex.app.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
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
import com.chronoplex.app.domain.Timer
import com.chronoplex.app.domain.TimerState
import com.chronoplex.app.ui.TimerEditViewModel
import com.chronoplex.app.ui.TimersViewModel
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimersScreen(
    vm: TimersViewModel,
    editVm: TimerEditViewModel,
) {
    val timers by vm.timers.collectAsState()
    val groups by vm.groups.collectAsState()
    val groupingEnabled by vm.groupingEnabled.collectAsState()
    var menuOpen by remember { mutableStateOf(false) }
    var manageGroupsOpen by remember { mutableStateOf(false) }
    var moveTarget by remember { mutableStateOf<Timer?>(null) }
    var ungroupedCollapsed by remember { mutableStateOf(false) }
    var reorderMode by remember { mutableStateOf(false) }
    var editSheetOpen by remember { mutableStateOf(false) }
    if (timers.isEmpty() && reorderMode) reorderMode = false
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedLabel = stringResource(R.string.timer_deleted)
    val undoLabel = stringResource(R.string.undo)
    fun openAdd() {
        editVm.load(0L)
        editSheetOpen = true
    }
    fun openEdit(timer: Timer) {
        editVm.load(timer.id)
        editSheetOpen = true
    }
    fun handleDelete(timer: Timer) {
        vm.delete(timer)
        scope.launch {
            val r = snackbarHostState.showSnackbar(
                message = deletedLabel,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Long,
            )
            if (r == SnackbarResult.ActionPerformed) vm.restore(timer)
        }
    }

    // Single per-screen tick drives every running timer's countdown.
    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(500L)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (reorderMode) stringResource(R.string.reorder)
                        else stringResource(R.string.tab_timers)
                    )
                },
                actions = {
                    if (reorderMode) {
                        IconButton(onClick = { reorderMode = false }) {
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.done))
                        }
                    } else {
                        val anyMenuContent = timers.size >= 2 || groupingEnabled
                        if (anyMenuContent) {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                if (timers.size >= 2) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.reorder)) },
                                        onClick = { menuOpen = false; reorderMode = true },
                                    )
                                }
                                if (groupingEnabled) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.manage_groups)) },
                                        onClick = { menuOpen = false; manageGroupsOpen = true },
                                    )
                                }
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (!reorderMode) {
                FloatingActionButton(onClick = ::openAdd) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_timer))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (reorderMode) {
                if (groupingEnabled) {
                    TimersGroupedReorderableList(
                        timers = timers,
                        groups = groups,
                        onReorder = { vm.reorderItems(it) },
                    )
                } else {
                    TimersReorderableList(timers = timers, onReorder = { vm.reorderItems(it) })
                }
                return@Box
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (timers.isEmpty()) {
                    item { EmptyState(R.string.empty_timers_title, R.string.empty_timers_body) }
                } else if (!groupingEnabled) {
                    items(timers, key = { it.id }) { timer ->
                        TimerRow(
                            timer = timer,
                            nowMillis = now,
                            groupingEnabled = false,
                            onClick = { openEdit(timer) },
                            onPrimaryAction = {
                                when (timer.state) {
                                    TimerState.IDLE, TimerState.PAUSED, TimerState.FINISHED -> vm.start(timer)
                                    TimerState.RUNNING -> vm.pause(timer)
                                }
                            },
                            onReset = { vm.reset(timer) },
                            onDelete = { handleDelete(timer) },
                            onMove = { moveTarget = timer },
                        )
                    }
                } else {
                    val byGroup = timers.groupBy { it.groupId }
                    groups.forEach { g ->
                        val members = byGroup[g.id].orEmpty()
                        item(key = "group-${g.id}") {
                            GroupHeader(g, members.size, onToggleCollapsed = { vm.toggleCollapsed(g) })
                        }
                        if (!g.collapsed) {
                            items(members, key = { "g${g.id}-${it.id}" }) { timer ->
                                TimerRow(
                                    timer = timer,
                                    nowMillis = now,
                                    groupingEnabled = true,
                                    onClick = { openEdit(timer) },
                                    onPrimaryAction = {
                                        when (timer.state) {
                                            TimerState.IDLE, TimerState.PAUSED, TimerState.FINISHED -> vm.start(timer)
                                            TimerState.RUNNING -> vm.pause(timer)
                                        }
                                    },
                                    onReset = { vm.reset(timer) },
                                    onDelete = { handleDelete(timer) },
                                    onMove = { moveTarget = timer },
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
                            items(ungrouped, key = { "u-${it.id}" }) { timer ->
                                TimerRow(
                                    timer = timer,
                                    nowMillis = now,
                                    groupingEnabled = true,
                                    onClick = { openEdit(timer) },
                                    onPrimaryAction = {
                                        when (timer.state) {
                                            TimerState.IDLE, TimerState.PAUSED, TimerState.FINISHED -> vm.start(timer)
                                            TimerState.RUNNING -> vm.pause(timer)
                                        }
                                    },
                                    onReset = { vm.reset(timer) },
                                    onDelete = { handleDelete(timer) },
                                    onMove = { moveTarget = timer },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (manageGroupsOpen) {
        ManageGroupsDialog(
            groups = groups,
            onCreate = { vm.createGroup(it) },
            onRename = { id, name -> vm.renameGroup(id, name) },
            onDelete = { vm.deleteGroup(it) },
            onReorder = { vm.reorderGroups(it) },
            onDismiss = { manageGroupsOpen = false },
            memberCount = { gid -> timers.count { it.groupId == gid } },
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

    if (editSheetOpen) {
        TimerEditSheet(vm = editVm, onDismiss = { editSheetOpen = false })
    }
}

@Composable
private fun TimerRow(
    timer: Timer,
    nowMillis: Long,
    groupingEnabled: Boolean,
    onClick: () -> Unit,
    onPrimaryAction: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit,
) {
    val remaining = timer.remainingMillis(nowMillis)
    val progress = if (timer.durationMillis > 0L) {
        1f - (remaining.toFloat() / timer.durationMillis.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    val label = timer.label.ifBlank { stateLabel(timer.state) }
                    Text(
                        label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (timer.label.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        formatDuration(remaining),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (timer.state == TimerState.FINISHED) MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        stateLabel(timer.state),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onPrimaryAction) {
                    Icon(
                        when (timer.state) {
                            TimerState.RUNNING -> Icons.Default.Pause
                            else -> Icons.Default.PlayArrow
                        },
                        contentDescription = when (timer.state) {
                            TimerState.RUNNING -> stringResource(R.string.pause)
                            TimerState.PAUSED -> stringResource(R.string.resume)
                            else -> stringResource(R.string.start)
                        },
                    )
                }
                IconButton(onClick = onReset) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.reset))
                }
                if (groupingEnabled) {
                    IconButton(onClick = onMove) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.move_to_group),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (timer.state == TimerState.RUNNING || timer.state == TimerState.PAUSED) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun stateLabel(state: TimerState): String = when (state) {
    TimerState.IDLE -> stringResource(R.string.timer_state_idle)
    TimerState.RUNNING -> stringResource(R.string.timer_state_running)
    TimerState.PAUSED -> stringResource(R.string.timer_state_paused)
    TimerState.FINISHED -> stringResource(R.string.timer_state_finished)
}

@Composable
private fun TimersReorderableList(timers: List<Timer>, onReorder: (List<Long>) -> Unit) {
    var local by remember(timers.map { it.id }) { mutableStateOf(timers) }
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
            items(local, key = { it.id }) { timer ->
                ReorderableItem(reorderState, key = timer.id) {
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
                                    timer.label.ifBlank { stateLabel(timer.state) },
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    formatDuration(timer.durationMillis),
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
}

@Composable
private fun TimersGroupedReorderableList(
    timers: List<Timer>,
    groups: List<Group>,
    onReorder: (List<Long>) -> Unit,
) {
    var local by remember(timers.map { it.id }) { mutableStateOf(timers) }
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
                items(members, key = { it.id }) { timer ->
                    ReorderableItem(reorderState, key = timer.id) {
                        TimerDragRow(timer = timer)
                    }
                }
            }
            val ungrouped = byGroup[null].orEmpty()
            if (ungrouped.isNotEmpty()) {
                item(key = "h-ungrouped") {
                    UngroupedHeader(itemCount = ungrouped.size, collapsed = false, onToggleCollapsed = { })
                }
                items(ungrouped, key = { it.id }) { timer ->
                    ReorderableItem(reorderState, key = timer.id) {
                        TimerDragRow(timer = timer)
                    }
                }
            }
        }
    }
}

@Composable
private fun sh.calvin.reorderable.ReorderableCollectionItemScope.TimerDragRow(timer: Timer) {
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
                    timer.label.ifBlank { stateLabel(timer.state) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (timer.label.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                )
                Text(
                    formatDuration(timer.durationMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
