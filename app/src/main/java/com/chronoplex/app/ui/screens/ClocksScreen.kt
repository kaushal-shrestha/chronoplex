package com.chronoplex.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronoplex.app.ui.tappable
import androidx.compose.foundation.shape.RoundedCornerShape
import com.chronoplex.app.AppContainer
import com.chronoplex.app.R
import com.chronoplex.app.domain.Clock
import com.chronoplex.app.domain.Group
import com.chronoplex.app.ui.ClockEditViewModel
import com.chronoplex.app.ui.ClocksViewModel
import com.chronoplex.app.ui.rememberTapFeedback
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClocksScreen(
    vm: ClocksViewModel,
    editVm: ClockEditViewModel,
    container: AppContainer,
) {
    val clocks by vm.clocks.collectAsState()
    val groups by vm.groups.collectAsState()
    val groupingEnabled by vm.groupingEnabled.collectAsState()
    var menuOpen by remember { mutableStateOf(false) }
    var confirmClearAll by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var manageGroupsOpen by remember { mutableStateOf(false) }
    var reorderMode by remember { mutableStateOf(false) }
    var actionsTarget by remember { mutableStateOf<Clock?>(null) }
    var moveTarget by remember { mutableStateOf<Clock?>(null) }
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
                    onEdit = ::openEdit,
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
        val moveLabel = stringResource(R.string.move_to_group)
        val deleteLabel = stringResource(R.string.delete)
        RowActionsSheet(
            title = clock.label,
            actions = buildList {
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
    onEdit: (Clock) -> Unit,
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
                    onClick = {
                        if (converterOpen) onConverterSourceClock(clock.id)
                        else onEdit(clock)
                    },
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
                            onClick = {
                                if (converterOpen) onConverterSourceClock(clock.id)
                                else onEdit(clock)
                            },
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
                            onClick = {
                                if (converterOpen) onConverterSourceClock(clock.id)
                                else onEdit(clock)
                            },
                            onLongClick = { onLongPress(clock) },
                            onDelete = { onDelete(clock) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeConverterCard(
    clocks: List<Clock>,
    nowEpochMillis: Long,
    sourceClockId: Long?,
    pinnedEpochMillis: Long?,
    onPinnedEpochMillisChange: (Long?) -> Unit,
    onClose: () -> Unit,
) {
    val sourceClock = clocks.firstOrNull { it.id == sourceClockId }
    val sourceZoneId = sourceClock?.zoneId ?: ZoneId.systemDefault().id
    val sourceLabel = sourceClock?.label ?: stringResource(R.string.device_time)
    val selectedEpochMillis = pinnedEpochMillis ?: nowEpochMillis
    val sourceTime = remember(sourceZoneId, selectedEpochMillis) {
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(selectedEpochMillis), ZoneId.of(sourceZoneId))
    }
    val timeFmt = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }
    var timePickerOpen by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.convert_time),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = rememberTapFeedback(onClose)) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                }
            }
            Text(
                stringResource(R.string.converter_reference_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.converter_when_it_is),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                        )
                        Text(
                            timeFmt.format(sourceTime),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "${sourceLabel} - ${dateFmt.format(sourceTime)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f),
                        )
                    }
                    TextButton(onClick = rememberTapFeedback { timePickerOpen = true }) {
                        Text(stringResource(R.string.pick_time))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            ) {
                AssistChip(
                    onClick = rememberTapFeedback { onPinnedEpochMillisChange(null) },
                    label = { Text(stringResource(R.string.converter_now)) },
                )
                AssistChip(
                    onClick = rememberTapFeedback {
                        onPinnedEpochMillisChange(sourceTime.minusHours(1).toInstant().toEpochMilli())
                    },
                    label = { Text(stringResource(R.string.previous_hour)) },
                )
                AssistChip(
                    onClick = rememberTapFeedback {
                        onPinnedEpochMillisChange(sourceTime.plusHours(1).toInstant().toEpochMilli())
                    },
                    label = { Text(stringResource(R.string.next_hour)) },
                )
                AssistChip(
                    onClick = rememberTapFeedback {
                        onPinnedEpochMillisChange(sourceTime.plusDays(1).toInstant().toEpochMilli())
                    },
                    label = { Text(stringResource(R.string.next_day)) },
                )
            }

            if (clocks.isEmpty()) {
                Text(
                    stringResource(R.string.converter_no_clocks),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    clocks.forEach { clock ->
                        val converted = remember(clock.zoneId, selectedEpochMillis) {
                            ZonedDateTime.ofInstant(
                                Instant.ofEpochMilli(selectedEpochMillis),
                                ZoneId.of(clock.zoneId),
                            )
                        }
                        ConverterResultRow(
                            label = clock.label,
                            time = timeFmt.format(converted),
                            dayLabel = relativeDayLabel(sourceTime.toLocalDate(), converted.toLocalDate()),
                            isSource = clock.id == sourceClockId,
                        )
                    }
                }
            }
        }
    }

    if (timePickerOpen) {
        val pickerState = rememberTimePickerState(
            initialHour = sourceTime.hour,
            initialMinute = sourceTime.minute,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { timePickerOpen = false },
            title = { Text(stringResource(R.string.pick_time)) },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(onClick = rememberTapFeedback {
                    val updated = sourceTime
                        .withHour(pickerState.hour)
                        .withMinute(pickerState.minute)
                        .withSecond(0)
                        .withNano(0)
                    onPinnedEpochMillisChange(updated.toInstant().toEpochMilli())
                    timePickerOpen = false
                }) {
                    Text(stringResource(R.string.done))
                }
            },
            dismissButton = {
                TextButton(onClick = rememberTapFeedback { timePickerOpen = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun ConverterResultRow(
    label: String,
    time: String,
    dayLabel: String,
    isSource: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isSource) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        },
        border = if (isSource) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
        } else {
            null
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSource) FontWeight.Bold else FontWeight.Normal,
                )
                Text(
                    if (isSource) stringResource(R.string.converter_source) else dayLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                time,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun relativeDayLabel(sourceDate: LocalDate, convertedDate: LocalDate): String {
    return when (ChronoUnit.DAYS.between(sourceDate, convertedDate)) {
        -1L -> stringResource(R.string.yesterday)
        0L -> stringResource(R.string.today)
        1L -> stringResource(R.string.tomorrow)
        else -> DateTimeFormatter.ofPattern("MMM d").format(convertedDate)
    }
}

@Composable
private fun LocalTimeCard(
    now: ZonedDateTime,
    converterOpen: Boolean,
    isConverterSource: Boolean,
    onUseAsConverterSource: () -> Unit,
) {
    val fmt = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .tappable(onClick = {
                if (converterOpen) onUseAsConverterSource()
            }),
        colors = CardDefaults.cardColors(
            containerColor = if (converterOpen && !isConverterSource) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            contentColor = if (converterOpen && !isConverterSource) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            },
        ),
        border = if (converterOpen && isConverterSource) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
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
    converterOpen: Boolean,
    isConverterSource: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
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
            .tappable(onLongClick = onLongClick, onClick = onClick),
        colors = if (converterOpen && isConverterSource) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        } else {
            CardDefaults.cardColors()
        },
        border = if (converterOpen && isConverterSource) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
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
                    color = if (converterOpen && isConverterSource) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                if (zoneTime != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        dateFmt.format(zoneTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (converterOpen && isConverterSource) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
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
            IconButton(onClick = rememberTapFeedback(onDelete)) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = stringResource(R.string.delete),
                    tint = if (converterOpen && isConverterSource) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
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
