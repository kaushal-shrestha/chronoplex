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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronoplex.app.R
import com.chronoplex.app.alarm.AlarmScheduler
import com.chronoplex.app.AppContainer
import com.chronoplex.app.domain.Alarm
import com.chronoplex.app.domain.DayMask
import com.chronoplex.app.domain.Group
import com.chronoplex.app.ui.AlarmEditViewModel
import com.chronoplex.app.ui.AlarmsViewModel
import kotlinx.coroutines.launch
import com.chronoplex.app.ui.needsExactAlarmGrant
import com.chronoplex.app.ui.needsNotificationGrant
import com.chronoplex.app.ui.openAppNotificationSettings
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(
    vm: AlarmsViewModel,
    editVm: AlarmEditViewModel,
    container: AppContainer,
    onOpenExactAlarmSettings: () -> Unit,
) {
    val alarms by vm.alarms.collectAsState()
    val groups by vm.groups.collectAsState()
    val groupingEnabled by vm.groupingEnabled.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    var exactOk by remember { mutableStateOf(!needsExactAlarmGrant(context)) }
    var notifOk by remember { mutableStateOf(!needsNotificationGrant(context)) }
    val allPermsOk = exactOk && notifOk
    var menuOpen by remember { mutableStateOf(false) }
    var manageGroupsOpen by remember { mutableStateOf(false) }
    var moveTarget by remember { mutableStateOf<Alarm?>(null) }
    var ungroupedCollapsed by remember { mutableStateOf(false) }
    var editSheetOpen by remember { mutableStateOf(false) }
    var zonePickerRestrict by remember { mutableStateOf<Boolean?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedLabel = stringResource(R.string.alarm_deleted)
    val undoLabel = stringResource(R.string.undo)
    fun openAdd() {
        editVm.load(0L)
        editSheetOpen = true
    }
    fun openEdit(alarm: Alarm) {
        editVm.load(alarm.id)
        editSheetOpen = true
    }
    fun handleDelete(alarm: Alarm) {
        vm.delete(alarm)
        scope.launch {
            val r = snackbarHostState.showSnackbar(
                message = deletedLabel,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Long,
            )
            if (r == SnackbarResult.ActionPerformed) vm.restore(alarm)
        }
    }

    // Re-check permissions whenever we come back to the foreground — e.g. after the
    // user toggles a permission in system settings and returns.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        exactOk = !needsExactAlarmGrant(context)
        notifOk = !needsNotificationGrant(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_alarms)) },
                actions = {
                    IconButton(onClick = { showPermissionDialog = true }) {
                        Icon(
                            if (allPermsOk) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = stringResource(R.string.alarm_readiness),
                            tint = if (allPermsOk) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.error,
                        )
                    }
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
            FloatingActionButton(onClick = ::openAdd) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_alarm))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (alarms.isEmpty()) {
                    item { EmptyState(R.string.empty_alarms_title, R.string.empty_alarms_body) }
                } else if (!groupingEnabled) {
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmRow(
                            alarm = alarm,
                            groupingEnabled = false,
                            onClick = { openEdit(alarm) },
                            onToggle = { vm.toggleEnabled(alarm) },
                            onDelete = { handleDelete(alarm) },
                            onMove = { moveTarget = alarm },
                        )
                    }
                } else {
                    val byGroup = alarms.groupBy { it.groupId }
                    groups.forEach { g ->
                        val members = byGroup[g.id].orEmpty()
                        item(key = "group-${g.id}") {
                            GroupHeader(g, members.size, onToggleCollapsed = { vm.toggleCollapsed(g) })
                        }
                        if (!g.collapsed) {
                            items(members, key = { "g${g.id}-${it.id}" }) { alarm ->
                                AlarmRow(
                                    alarm = alarm,
                                    groupingEnabled = true,
                                    onClick = { openEdit(alarm) },
                                    onToggle = { vm.toggleEnabled(alarm) },
                                    onDelete = { handleDelete(alarm) },
                                    onMove = { moveTarget = alarm },
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
                            items(ungrouped, key = { "u-${it.id}" }) { alarm ->
                                AlarmRow(
                                    alarm = alarm,
                                    groupingEnabled = true,
                                    onClick = { openEdit(alarm) },
                                    onToggle = { vm.toggleEnabled(alarm) },
                                    onDelete = { handleDelete(alarm) },
                                    onMove = { moveTarget = alarm },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPermissionDialog) {
        PermissionStatusDialog(
            exactOk = exactOk,
            notifOk = notifOk,
            onOpenExactSettings = { showPermissionDialog = false; onOpenExactAlarmSettings() },
            onOpenNotificationSettings = {
                showPermissionDialog = false
                openAppNotificationSettings(context)
            },
            onDismiss = { showPermissionDialog = false },
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
            memberCount = { gid -> alarms.count { it.groupId == gid } },
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
        AlarmEditSheet(
            vm = editVm,
            onPickZone = { restrict ->
                editSheetOpen = false
                zonePickerRestrict = restrict
            },
            onDismiss = { editSheetOpen = false },
        )
    }
    zonePickerRestrict?.let { restrict ->
        ZonePickerSheet(
            restrictToAdded = restrict,
            container = container,
            onPick = { zoneId ->
                editVm.setZone(zoneId)
                zonePickerRestrict = null
                editSheetOpen = true
            },
            onDismiss = {
                zonePickerRestrict = null
                editSheetOpen = true
            },
        )
    }
}

@Composable
private fun PermissionStatusDialog(
    exactOk: Boolean,
    notifOk: Boolean,
    onOpenExactSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.alarm_readiness)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PermissionLine(
                    label = stringResource(R.string.permission_label_exact),
                    ok = exactOk,
                    rationale = stringResource(R.string.permission_exact_alarm_rationale),
                    actionLabel = stringResource(R.string.grant),
                    onAction = onOpenExactSettings,
                )
                PermissionLine(
                    label = stringResource(R.string.permission_label_notifications),
                    ok = notifOk,
                    rationale = stringResource(R.string.permission_notifications_rationale),
                    actionLabel = stringResource(R.string.open),
                    onAction = onOpenNotificationSettings,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dismiss)) }
        },
    )
}

@Composable
private fun PermissionLine(
    label: String,
    ok: Boolean,
    rationale: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                if (ok) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            Text(label, style = MaterialTheme.typography.titleSmall)
        }
        if (!ok) {
            Text(rationale, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 4.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun AlarmRow(
    alarm: Alarm,
    groupingEnabled: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit,
) {
    val timeFmt = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val timeText = remember(alarm.hour, alarm.minute) {
        timeFmt.format(LocalTime.of(alarm.hour, alarm.minute))
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        timeText,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (alarm.enabled) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (alarm.label.isNotBlank()) {
                        Text(
                            alarm.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        alarm.zoneId,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = alarm.enabled, onCheckedChange = { onToggle() })
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    repeatLabel(alarm),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (alarm.enabled) {
                    val snoozed = alarm.isSnoozed()
                    Text(
                        if (snoozed) snoozedText(alarm) else nextFireText(alarm),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (snoozed) MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.primary,
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
}

@Composable
private fun repeatLabel(alarm: Alarm): String {
    if (alarm.isOneShot) return stringResource(R.string.repeat_once)
    val mask = alarm.daysMask
    if (mask == DayMask.EVERY_DAY) return stringResource(R.string.repeat_every_day)
    if (mask == DayMask.WEEKDAYS) return stringResource(R.string.repeat_weekdays)
    if (mask == DayMask.WEEKENDS) return stringResource(R.string.repeat_weekends)
    // Collect names up-front so the join lambda doesn't invoke @Composable functions.
    val names = alarm.daysOfWeek.sortedBy { it.value }.map { shortDayName(it) }
    return names.joinToString(", ")
}

@Composable
private fun shortDayName(d: java.time.DayOfWeek): String = when (d) {
    java.time.DayOfWeek.MONDAY -> stringResource(R.string.day_mon)
    java.time.DayOfWeek.TUESDAY -> stringResource(R.string.day_tue)
    java.time.DayOfWeek.WEDNESDAY -> stringResource(R.string.day_wed)
    java.time.DayOfWeek.THURSDAY -> stringResource(R.string.day_thu)
    java.time.DayOfWeek.FRIDAY -> stringResource(R.string.day_fri)
    java.time.DayOfWeek.SATURDAY -> stringResource(R.string.day_sat)
    java.time.DayOfWeek.SUNDAY -> stringResource(R.string.day_sun)
}

@Composable
private fun snoozedText(alarm: Alarm): String {
    val zone = runCatching { ZoneId.of(alarm.zoneId) }.getOrElse { ZoneId.systemDefault() }
    val until = alarm.snoozeUntilMillis ?: return ""
    val ts = ZonedDateTime.ofInstant(Instant.ofEpochMilli(until), zone)
    val fmt = remember { DateTimeFormatter.ofPattern("h:mm a") }
    return stringResource(R.string.snoozed_until, fmt.format(ts))
}

@Composable
private fun nextFireText(alarm: Alarm): String {
    val next = AlarmScheduler.nextTriggerMillis(alarm) ?: return stringResource(R.string.never_fires)
    val zone = runCatching { ZoneId.of(alarm.zoneId) }.getOrElse { ZoneId.systemDefault() }
    val zoned = ZonedDateTime.ofInstant(Instant.ofEpochMilli(next), zone)
    val durLabel = humanDuration(Duration.between(Instant.now(), Instant.ofEpochMilli(next)))
    val dayLabel = DateTimeFormatter.ofPattern("EEE h:mm a").format(zoned)
    return stringResource(R.string.next_fires, "$dayLabel ($durLabel)")
}

private fun humanDuration(d: Duration): String {
    if (d.isNegative || d.isZero) return "now"
    val totalMin = d.toMinutes()
    val h = totalMin / 60
    val m = totalMin % 60
    return when {
        h >= 24 -> "${h / 24}d ${h % 24}h"
        h > 0 -> "${h}h ${m}m"
        else -> "${m}m"
    }
}
