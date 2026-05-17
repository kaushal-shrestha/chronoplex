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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import com.chronoplex.app.R
import com.chronoplex.app.domain.Clock
import com.chronoplex.app.domain.Group
import com.chronoplex.app.ui.ClocksViewModel
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlinx.coroutines.delay
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClocksScreen(
    vm: ClocksViewModel,
    onAdd: () -> Unit,
    onEdit: (Clock) -> Unit,
) {
    val clocks by vm.clocks.collectAsState()
    val groups by vm.groups.collectAsState()
    val groupingEnabled by vm.groupingEnabled.collectAsState()
    var menuOpen by remember { mutableStateOf(false) }
    var confirmClearAll by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var manageGroupsOpen by remember { mutableStateOf(false) }
    var moveTarget by remember { mutableStateOf<Clock?>(null) }
    var reorderMode by remember { mutableStateOf(false) }
    // Auto-exit reorder if the list empties out.
    if (clocks.isEmpty() && reorderMode) reorderMode = false
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedLabel = stringResource(R.string.clock_deleted)
    val undoLabel = stringResource(R.string.undo)
    fun handleDelete(clock: Clock) {
        vm.delete(clock.id)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = deletedLabel,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) vm.restore(clock)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (reorderMode) stringResource(R.string.reorder)
                        else stringResource(R.string.tab_clocks)
                    )
                },
                actions = {
                    if (reorderMode) {
                        IconButton(onClick = { reorderMode = false }) {
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.done))
                        }
                    } else {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            if (clocks.size >= 2) {
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
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.reset_clocks)) },
                                onClick = {
                                    menuOpen = false
                                    confirmReset = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.clear_all_clocks)) },
                                onClick = {
                                    menuOpen = false
                                    confirmClearAll = true
                                },
                                enabled = clocks.isNotEmpty(),
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (!reorderMode) {
                FloatingActionButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_clock))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (reorderMode) {
                if (groupingEnabled) {
                    ClocksGroupedReorderableList(
                        clocks = clocks,
                        groups = groups,
                        onReorder = { vm.reorderItems(it) },
                    )
                } else {
                    ClocksReorderableList(
                        clocks = clocks,
                        onReorder = { vm.reorderItems(it) },
                    )
                }
            } else {
                ClocksList(
                    clocks = clocks,
                    groups = groups,
                    groupingEnabled = groupingEnabled,
                    onEdit = onEdit,
                    onDelete = ::handleDelete,
                    onMove = { moveTarget = it },
                    onToggleCollapsed = { vm.toggleCollapsed(it) },
                )
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
            memberCount = { gid -> clocks.count { it.groupId == gid } },
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
            title = { Text(stringResource(R.string.clear_all_clocks)) },
            text = { Text(stringResource(R.string.clear_all_clocks_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClearAll = false
                    vm.deleteAll()
                }) { Text(stringResource(R.string.clear)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearAll = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.reset_clocks)) },
            text = { Text(stringResource(R.string.reset_clocks_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    vm.resetToDefaults()
                }) { Text(stringResource(R.string.reset)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun ClocksList(
    clocks: List<Clock>,
    groups: List<Group>,
    groupingEnabled: Boolean,
    onEdit: (Clock) -> Unit,
    onDelete: (Clock) -> Unit,
    onMove: (Clock) -> Unit,
    onToggleCollapsed: (Group) -> Unit,
) {
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    var ungroupedCollapsed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            now = ZonedDateTime.now()
            delay(1000)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { LocalTimeCard(now) }
        if (clocks.isEmpty()) {
            item { EmptyState(R.string.empty_clocks_title, R.string.empty_clocks_body) }
        } else if (!groupingEnabled) {
            items(clocks, key = { it.id }) { clock ->
                ClockRow(
                    clock = clock,
                    nowEpochMillis = now.toInstant().toEpochMilli(),
                    groupingEnabled = false,
                    onClick = { onEdit(clock) },
                    onDelete = { onDelete(clock) },
                    onMove = { onMove(clock) },
                )
            }
        } else {
            val byGroup = clocks.groupBy { it.groupId }
            groups.forEach { g ->
                val members = byGroup[g.id].orEmpty()
                item(key = "group-${g.id}") {
                    GroupHeader(g, members.size, onToggleCollapsed = { onToggleCollapsed(g) })
                }
                if (!g.collapsed) {
                    items(members, key = { "g${g.id}-${it.id}" }) { clock ->
                        ClockRow(
                            clock = clock,
                            nowEpochMillis = now.toInstant().toEpochMilli(),
                            groupingEnabled = true,
                            onClick = { onEdit(clock) },
                            onDelete = { onDelete(clock) },
                            onMove = { onMove(clock) },
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
                    items(ungrouped, key = { "u-${it.id}" }) { clock ->
                        ClockRow(
                            clock = clock,
                            nowEpochMillis = now.toInstant().toEpochMilli(),
                            groupingEnabled = true,
                            onClick = { onEdit(clock) },
                            onDelete = { onDelete(clock) },
                            onMove = { onMove(clock) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalTimeCard(now: ZonedDateTime) {
    val fmt = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.device_time),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    ZoneId.systemDefault().id,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    dateFmt.format(now),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    fmt.format(now),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun ClockRow(
    clock: Clock,
    nowEpochMillis: Long,
    groupingEnabled: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit,
) {
    val zoneTime = remember(clock.zoneId, nowEpochMillis) {
        runCatching {
            ZonedDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(nowEpochMillis),
                ZoneId.of(clock.zoneId),
            )
        }.getOrNull()
    }
    val fmt = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    clock.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    clock.zoneId,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (zoneTime != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        dateFmt.format(zoneTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    zoneTime?.let { fmt.format(it) } ?: "—",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            IconButton(onClick = onClick) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit_clock),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
    }
}

@Composable
private fun ClocksReorderableList(
    clocks: List<Clock>,
    onReorder: (List<Long>) -> Unit,
) {
    // Local mirror of the upstream list so the LazyColumn can show the drag in flight.
    // Resync from upstream when the set of ids changes (e.g., delete from elsewhere).
    var local by remember(clocks.map { it.id }) { mutableStateOf(clocks) }

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
            items(local, key = { it.id }) { clock ->
                ReorderableItem(reorderState, key = clock.id) {
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
                                Text(clock.label, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    clock.zoneId,
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

/**
 * Grouped reorder list. Items can be reordered within their group only;
 * group headers act as fixed anchors. Reordering of the groups themselves
 * is handled in ManageGroupsDialog instead (drag handles are awkward in a
 * long mixed list).
 */
@Composable
private fun ClocksGroupedReorderableList(
    clocks: List<Clock>,
    groups: List<Group>,
    onReorder: (List<Long>) -> Unit,
) {
    var local by remember(clocks.map { it.id }) { mutableStateOf(clocks) }
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
                items(members, key = { it.id }) { clock ->
                    ReorderableItem(reorderState, key = clock.id) {
                        ClockDragRow(clock = clock)
                    }
                }
            }
            val ungrouped = byGroup[null].orEmpty()
            if (ungrouped.isNotEmpty()) {
                item(key = "h-ungrouped") {
                    UngroupedHeader(itemCount = ungrouped.size, collapsed = false, onToggleCollapsed = { })
                }
                items(ungrouped, key = { it.id }) { clock ->
                    ReorderableItem(reorderState, key = clock.id) {
                        ClockDragRow(clock = clock)
                    }
                }
            }
        }
    }
}

@Composable
private fun sh.calvin.reorderable.ReorderableCollectionItemScope.ClockDragRow(clock: Clock) {
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
                    clock.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    clock.zoneId,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun EmptyState(titleRes: Int, bodyRes: Int) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.Schedule,
            contentDescription = null,
            modifier = Modifier.padding(8.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(bodyRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
