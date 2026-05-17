package com.chronoplex.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_stopwatches)) },
                actions = {
                    if (groupingEnabled) {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.manage_groups)) },
                                onClick = { menuOpen = false; manageGroupsOpen = true },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { vm.addStopwatch() }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_stopwatch))
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
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
                            groupingEnabled = false,
                            onRename = { renameTarget = sw },
                            onMove = { moveTarget = sw },
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
                                    groupingEnabled = true,
                                    onRename = { renameTarget = sw },
                                    onMove = { moveTarget = sw },
                                )
                            }
                        }
                    }
                    val ungrouped = byGroup[null].orEmpty()
                    if (ungrouped.isNotEmpty()) {
                        item(key = "ungrouped") { UngroupedHeader(ungrouped.size) }
                        items(ungrouped, key = { "u-${it.id}" }) { sw ->
                            StopwatchCard(
                                stopwatch = sw, nowMillis = now, vm = vm,
                                groupingEnabled = true,
                                onRename = { renameTarget = sw },
                                onMove = { moveTarget = sw },
                            )
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

    if (manageGroupsOpen) {
        ManageGroupsDialog(
            groups = groups,
            onCreate = { vm.createGroup(it) },
            onRename = { id, name -> vm.renameGroup(id, name) },
            onDelete = { vm.deleteGroup(it) },
            onDismiss = { manageGroupsOpen = false },
        )
    }
    moveTarget?.let { target ->
        MoveToGroupDialog(
            currentGroupId = target.groupId,
            groups = groups,
            onMove = { gid -> vm.moveToGroup(target.id, gid); moveTarget = null },
            onCreateAndMove = { name -> vm.createGroup(name); moveTarget = null },
            onDismiss = { moveTarget = null },
        )
    }
}

@Composable
private fun StopwatchCard(
    stopwatch: Stopwatch,
    nowMillis: Long,
    vm: StopwatchesViewModel,
    groupingEnabled: Boolean,
    onRename: () -> Unit,
    onMove: () -> Unit,
) {
    var lapsExpanded by remember { mutableStateOf(false) }
    val laps by vm.observeLaps(stopwatch.id).collectAsState(initial = emptyList())
    val elapsed = stopwatch.elapsedMillis(nowMillis)

    Card(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stopwatch.label.ifBlank { stringResource(R.string.tab_stopwatches) },
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        IconButton(onClick = onRename) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.rename))
                        }
                    }
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
                IconButton(onClick = {
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
                    onClick = { vm.lap(stopwatch) },
                    enabled = stopwatch.state == StopwatchState.RUNNING,
                ) {
                    Icon(Icons.Default.Timer, contentDescription = stringResource(R.string.lap))
                }
                IconButton(onClick = { vm.reset(stopwatch) }) {
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
                IconButton(onClick = { vm.delete(stopwatch) }) {
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
                            .clickable { lapsExpanded = !lapsExpanded }
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
            TextButton(onClick = { onConfirm(text) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
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
