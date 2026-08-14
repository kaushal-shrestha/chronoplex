package com.zoneanchor.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zoneanchor.app.AppContainer
import com.zoneanchor.app.R
import com.zoneanchor.app.domain.Clock
import com.zoneanchor.app.domain.Group
import com.zoneanchor.app.ui.ClockEditViewModel
import com.zoneanchor.app.ui.ClocksViewModel
import com.zoneanchor.app.ui.rememberTapFeedback
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.time.ZonedDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClocksScreen(
    vm: ClocksViewModel,
    editVm: ClockEditViewModel,
    container: AppContainer,
) {
    val clocks by vm.clocks.collectAsState()
    val alarms by vm.alarms.collectAsState()
    val groups by vm.groups.collectAsState()
    val groupingEnabled by vm.groupingEnabled.collectAsState()
    var menuOpen by remember { mutableStateOf(false) }
    var confirmClearAll by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var manageGroupsOpen by remember { mutableStateOf(false) }
    var reorderMode by remember { mutableStateOf(false) }
    var actionsTarget by remember { mutableStateOf<Clock?>(null) }
    var moveTarget by remember { mutableStateOf<Clock?>(null) }
    var deleteTarget by remember { mutableStateOf<Clock?>(null) }
    var editSheetOpen by remember { mutableStateOf(false) }
    var zonePickerOpen by remember { mutableStateOf(false) }
    var converterOpen by rememberSaveable { mutableStateOf(false) }
    var converterSourceClockId by rememberSaveable { mutableStateOf<Long?>(null) }
    var converterPinnedEpochMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    fun openAdd() {
        editVm.load(0L)
        editSheetOpen = true
    }
    fun openEdit(clock: Clock) {
        editVm.load(clock.id)
        editSheetOpen = true
    }
    // Auto-exit reorder if the list empties out.
    if (clocks.isEmpty() && reorderMode) reorderMode = false
    if (clocks.isEmpty() && converterOpen) converterOpen = false
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedLabel = stringResource(R.string.clock_deleted)
    val undoLabel = stringResource(R.string.undo)
    fun attachedAlarmCount(clock: Clock): Int = alarms.count { it.clockId == clock.id }
    fun deleteWithUndo(clock: Clock) {
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
    fun handleDelete(clock: Clock) {
        if (attachedAlarmCount(clock) > 0) deleteTarget = clock else deleteWithUndo(clock)
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
                        IconButton(onClick = rememberTapFeedback { reorderMode = false }) {
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.done))
                        }
                    } else {
                        IconButton(onClick = rememberTapFeedback { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(
                                            if (converterOpen) R.string.hide_converter
                                            else R.string.convert_time,
                                        )
                                    )
                                },
                                onClick = rememberTapFeedback {
                                    menuOpen = false
                                    converterOpen = !converterOpen
                                },
                                enabled = clocks.isNotEmpty(),
                            )
                            HorizontalDivider()
                            if (clocks.size >= 2) {
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
                                text = { Text(stringResource(R.string.reset_clocks)) },
                                onClick = rememberTapFeedback {
                                    menuOpen = false
                                    confirmReset = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.clear_all_clocks)) },
                                onClick = rememberTapFeedback {
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
                val onFabClick = rememberTapFeedback { openAdd() }
                FloatingActionButton(onClick = onFabClick) {
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
                    converterOpen = converterOpen,
                    converterSourceClockId = converterSourceClockId,
                    converterPinnedEpochMillis = converterPinnedEpochMillis,
                    onConverterSourceClock = { converterSourceClockId = it },
                    onConverterPinnedEpochMillis = { converterPinnedEpochMillis = it },
                    onCloseConverter = { converterOpen = false },
                    onLongPress = { actionsTarget = it },
                    onDelete = ::handleDelete,
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
    actionsTarget?.let { clock ->
        val editLabel = stringResource(R.string.edit)
        val moveLabel = stringResource(R.string.move_to_group)
        val deleteLabel = stringResource(R.string.delete)
        RowActionsSheet(
            title = clock.label,
            actions = buildList {
                add(RowAction(
                    label = editLabel,
                    icon = Icons.Default.Edit,
                    onClick = { actionsTarget = null; openEdit(clock) },
                ))
                if (groupingEnabled) {
                    add(RowAction(
                        label = moveLabel,
                        icon = Icons.Default.Folder,
                        onClick = { actionsTarget = null; moveTarget = clock },
                    ))
                }
                add(RowAction(
                    label = deleteLabel,
                    icon = Icons.Default.DeleteOutline,
                    tint = MaterialTheme.colorScheme.error,
                    onClick = { actionsTarget = null; handleDelete(clock) },
                ))
            },
            onDismiss = { actionsTarget = null },
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

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.reset_clocks)) },
            text = { Text(stringResource(R.string.reset_clocks_message)) },
            confirmButton = {
                TextButton(onClick = rememberTapFeedback {
                    confirmReset = false
                    vm.resetToDefaults()
                }) { Text(stringResource(R.string.reset)) }
            },
            dismissButton = {
                TextButton(onClick = rememberTapFeedback { confirmReset = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    deleteTarget?.let { clock ->
        val count = attachedAlarmCount(clock)
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.clock_delete_detaches_title, clock.label)) },
            text = {
                Text(
                    stringResource(
                        R.string.clock_delete_detaches_message,
                        count,
                        clock.zoneId,
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = rememberTapFeedback {
                    deleteTarget = null
                    vm.delete(clock.id)
                }) {
                    Text(stringResource(R.string.delete_clock))
                }
            },
            dismissButton = {
                TextButton(onClick = rememberTapFeedback { deleteTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (editSheetOpen) {
        ClockEditSheet(
            vm = editVm,
            onPickZone = {
                editSheetOpen = false
                zonePickerOpen = true
            },
            onDelete = { id ->
                clocks.firstOrNull { it.id == id }?.let { handleDelete(it) }
            },
            onDismiss = { editSheetOpen = false },
        )
    }
    if (zonePickerOpen) {
        ZonePickerSheet(
            restrictToAdded = false,
            container = container,
            onPick = { zoneId ->
                editVm.setZone(zoneId)
                zonePickerOpen = false
                editSheetOpen = true
            },
            onDismiss = {
                zonePickerOpen = false
                editSheetOpen = true
            },
        )
    }
}

@Composable
private fun ClocksList(
    clocks: List<Clock>,
    groups: List<Group>,
    groupingEnabled: Boolean,
    converterOpen: Boolean,
    converterSourceClockId: Long?,
    converterPinnedEpochMillis: Long?,
    onConverterSourceClock: (Long?) -> Unit,
    onConverterPinnedEpochMillis: (Long?) -> Unit,
    onCloseConverter: () -> Unit,
    onLongPress: (Clock) -> Unit,
    onDelete: (Clock) -> Unit,
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
        if (converterOpen) {
            item {
                TimeConverterCard(
                    clocks = clocks,
                    nowEpochMillis = now.toInstant().toEpochMilli(),
                    sourceClockId = converterSourceClockId,
                    pinnedEpochMillis = converterPinnedEpochMillis,
                    onPinnedEpochMillisChange = onConverterPinnedEpochMillis,
                    onClose = onCloseConverter,
                )
            }
        }
        item {
            LocalTimeCard(
                now = now,
                converterOpen = converterOpen,
                isConverterSource = converterSourceClockId == null,
                onUseAsConverterSource = { onConverterSourceClock(null) },
            )
        }
        if (clocks.isEmpty()) {
            item { EmptyState(R.string.empty_clocks_title, R.string.empty_clocks_body) }
        } else if (!groupingEnabled) {
            items(clocks, key = { it.id }) { clock ->
                ClockRow(
                    clock = clock,
                    nowEpochMillis = now.toInstant().toEpochMilli(),
                    converterOpen = converterOpen,
                    isConverterSource = converterSourceClockId == clock.id,
                    onClick = if (converterOpen) ({ onConverterSourceClock(clock.id) }) else null,
                    onLongClick = { onLongPress(clock) },
                    onDelete = { onDelete(clock) },
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
                            converterOpen = converterOpen,
                            isConverterSource = converterSourceClockId == clock.id,
                            onClick = if (converterOpen) ({ onConverterSourceClock(clock.id) }) else null,
                            onLongClick = { onLongPress(clock) },
                            onDelete = { onDelete(clock) },
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
                            converterOpen = converterOpen,
                            isConverterSource = converterSourceClockId == clock.id,
                            onClick = if (converterOpen) ({ onConverterSourceClock(clock.id) }) else null,
                            onLongClick = { onLongPress(clock) },
                            onDelete = { onDelete(clock) },
                        )
                    }
                }
            }
        }
    }
}
