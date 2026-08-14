package com.zoneanchor.app.ui.screens

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoneanchor.app.R
import com.zoneanchor.app.alarm.AlarmScheduler
import com.zoneanchor.app.AppContainer
import com.zoneanchor.app.data.AlarmZoneDisplayMode
import com.zoneanchor.app.domain.Alarm
import com.zoneanchor.app.domain.AlarmRepeatType
import com.zoneanchor.app.domain.Clock
import com.zoneanchor.app.domain.DayMask
import com.zoneanchor.app.domain.Group
import com.zoneanchor.app.ui.AlarmEditViewModel
import com.zoneanchor.app.ui.AlarmsViewModel
import com.zoneanchor.app.ui.longPressable
import com.zoneanchor.app.ui.rememberTapFeedback
import com.zoneanchor.app.ui.rememberToggleFeedback
import com.zoneanchor.app.ui.needsExactAlarmGrant
import com.zoneanchor.app.ui.needsNotificationGrant
import com.zoneanchor.app.ui.openAppNotificationSettings
import java.time.Duration
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(
    vm: AlarmsViewModel,
    editVm: AlarmEditViewModel,
    container: AppContainer,
    onOpenExactAlarmSettings: () -> Unit,
) {
    val alarms by vm.alarms.collectAsState()
    val clocks by vm.clocks.collectAsState()
    val alarmZoneDisplay by vm.alarmZoneDisplay.collectAsState()
    val groups by vm.groups.collectAsState()
    val groupingEnabled by vm.groupingEnabled.collectAsState()
    val clocksById = remember(clocks) { clocks.associateBy { it.id } }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val sortedAlarms = remember(alarms, nowMillis) { alarms.sortedWith(alarmRingOrderComparator(nowMillis)) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    var exactOk by remember { mutableStateOf(!needsExactAlarmGrant(context)) }
    var notifOk by remember { mutableStateOf(!needsNotificationGrant(context)) }
    val allPermsOk = exactOk && notifOk
    var menuOpen by remember { mutableStateOf(false) }
    var manageGroupsOpen by remember { mutableStateOf(false) }
    var confirmClearAll by remember { mutableStateOf(false) }
    var actionsTarget by remember { mutableStateOf<Alarm?>(null) }
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

    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(30_000)
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
                    IconButton(onClick = rememberTapFeedback { showPermissionDialog = true }) {
                        Icon(
                            if (allPermsOk) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = stringResource(R.string.alarm_readiness),
                            tint = if (allPermsOk) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.error,
                        )
                    }
                    IconButton(onClick = rememberTapFeedback { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (groupingEnabled) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.manage_groups)) },
                                onClick = rememberTapFeedback { menuOpen = false; manageGroupsOpen = true },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.clear_all_alarms)) },
                            onClick = rememberTapFeedback { menuOpen = false; confirmClearAll = true },
                            enabled = alarms.isNotEmpty(),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            val onFabClick = rememberTapFeedback { openAdd() }
            FloatingActionButton(onClick = onFabClick) {
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
                    items(sortedAlarms, key = { it.id }) { alarm ->
                        AlarmRow(
                            alarm = alarm,
                            zoneLabel = alarmZoneLabel(alarm, clocksById, alarmZoneDisplay),
                            nowMillis = nowMillis,
                            onLongClick = { actionsTarget = alarm },
                            onToggle = { vm.toggleEnabled(alarm) },
                        )
                    }
                } else {
                    val byGroup = sortedAlarms.groupBy { it.groupId }
                    groups.forEach { g ->
                        val members = byGroup[g.id].orEmpty()
                        item(key = "group-${g.id}") {
                            GroupHeader(g, members.size, onToggleCollapsed = { vm.toggleCollapsed(g) })
                        }
                        if (!g.collapsed) {
                            items(members, key = { "g${g.id}-${it.id}" }) { alarm ->
                                AlarmRow(
                                    alarm = alarm,
                                    zoneLabel = alarmZoneLabel(alarm, clocksById, alarmZoneDisplay),
                                    nowMillis = nowMillis,
                                    onLongClick = { actionsTarget = alarm },
                                    onToggle = { vm.toggleEnabled(alarm) },
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
                                    zoneLabel = alarmZoneLabel(alarm, clocksById, alarmZoneDisplay),
                                    nowMillis = nowMillis,
                                    onLongClick = { actionsTarget = alarm },
                                    onToggle = { vm.toggleEnabled(alarm) },
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

    actionsTarget?.let { alarm ->
        val editLabel = stringResource(R.string.edit)
        val moveLabel = stringResource(R.string.move_to_group)
        val deleteLabel = stringResource(R.string.delete)
        RowActionsSheet(
            title = alarm.label.ifBlank { null },
            actions = buildList {
                add(RowAction(
                    label = editLabel,
                    icon = Icons.Default.Edit,
                    onClick = { actionsTarget = null; openEdit(alarm) },
                ))
                if (groupingEnabled) {
                    add(RowAction(
                        label = moveLabel,
                        icon = Icons.Default.Folder,
                        onClick = { actionsTarget = null; moveTarget = alarm },
                    ))
                }
                add(RowAction(
                    label = deleteLabel,
                    icon = Icons.Default.DeleteOutline,
                    tint = MaterialTheme.colorScheme.error,
                    onClick = { actionsTarget = null; handleDelete(alarm) },
                ))
            },
            onDismiss = { actionsTarget = null },
        )
    }

    if (confirmClearAll) {
        AlertDialog(
            onDismissRequest = { confirmClearAll = false },
            title = { Text(stringResource(R.string.clear_all_alarms)) },
            text = { Text(stringResource(R.string.clear_all_alarms_message)) },
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

    if (editSheetOpen) {
        AlarmEditSheet(
            vm = editVm,
            onPickZone = { restrict ->
                editSheetOpen = false
                zonePickerRestrict = restrict
            },
            onDelete = { id ->
                alarms.firstOrNull { it.id == id }?.let { handleDelete(it) }
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
            TextButton(onClick = rememberTapFeedback(onDismiss)) { Text(stringResource(R.string.dismiss)) }
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
            Button(onClick = rememberTapFeedback(onAction)) { Text(actionLabel) }
        }
    }
}

@Composable
private fun AlarmRow(
    alarm: Alarm,
    zoneLabel: String,
    nowMillis: Long,
    onLongClick: () -> Unit,
    onToggle: () -> Unit,
) {
    val timeFmt = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val timeText = remember(alarm.hour, alarm.minute) {
        timeFmt.format(LocalTime.of(alarm.hour, alarm.minute))
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .longPressable(onLongPress = onLongClick),
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
                        zoneLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = alarm.enabled, onCheckedChange = rememberToggleFeedback { onToggle() })
            }
            Spacer(Modifier.height(8.dp))
            Text(
                repeatLabel(alarm),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (alarm.enabled) {
                nextFireInfo(alarm, nowMillis)?.let { info ->
                    Spacer(Modifier.height(8.dp))
                    FireInfoRow(info = info, highlighted = alarm.isSnoozed(nowMillis))
                }
            }
        }
    }
}

@Composable
private fun FireInfoRow(info: FireInfo, highlighted: Boolean) {
    val valueColor = if (highlighted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FireInfoText(label = "Clock", value = info.referenceTime, valueColor = valueColor, modifier = Modifier.weight(1.25f))
        FireInfoText(label = "Device", value = info.deviceTime, valueColor = valueColor, modifier = Modifier.weight(1.25f))
        FireInfoText(label = "In", value = info.remaining, valueColor = valueColor, modifier = Modifier.weight(0.7f))
    }
}

@Composable
private fun FireInfoText(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            "$label ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun alarmZoneLabel(
    alarm: Alarm,
    clocksById: Map<Long, Clock>,
    displayMode: AlarmZoneDisplayMode,
): String {
    if (displayMode == AlarmZoneDisplayMode.ZONE_ID) return alarm.zoneId
    val clock = alarm.clockId?.let(clocksById::get)
    return clock?.label?.takeIf { it.isNotBlank() } ?: alarm.zoneId
}

@Composable
private fun repeatLabel(alarm: Alarm): String {
    return when (alarm.effectiveRepeatType) {
        AlarmRepeatType.ONCE -> "Rings once"
        AlarmRepeatType.WEEKLY -> weeklyRepeatLabel(alarm)
        AlarmRepeatType.MONTHLY_DAY -> {
            val base = "day ${alarm.monthlyDay}"
            if (alarm.repeatInterval == 1) "Repeats monthly on $base" else "Repeats every ${alarm.repeatInterval} months on $base"
        }
        AlarmRepeatType.MONTHLY_WEEKDAY -> {
            val weekday = fullDayName(DayOfWeek.of(alarm.monthlyWeekday.coerceIn(1, 7)))
            val ordinal = ordinalLabel(alarm.monthlyOrdinal)
            val base = "$ordinal $weekday"
            if (alarm.repeatInterval == 1) "Repeats monthly on the $base" else "Repeats every ${alarm.repeatInterval} months on the $base"
        }
    }
}

@Composable
private fun weeklyRepeatLabel(alarm: Alarm): String {
    val mask = alarm.daysMask
    if (alarm.repeatInterval == 1) {
        return when (mask) {
            DayMask.EVERY_DAY -> "Repeats every day"
            DayMask.WEEKDAYS -> "Repeats weekdays"
            DayMask.WEEKENDS -> "Repeats weekends"
            else -> {
                val days = alarm.daysOfWeek.sortedBy { it.value }.map { fullDayName(it) }.joinToString(", ")
                if (mask.countSelectedDays() == 1) "Repeats every $days" else "Repeats on $days"
            }
        }
    }
    val base = when (mask) {
        DayMask.EVERY_DAY -> "every day"
        DayMask.WEEKDAYS -> "weekdays"
        DayMask.WEEKENDS -> "weekends"
        else -> alarm.daysOfWeek.sortedBy { it.value }.map { fullDayName(it) }.joinToString(", ")
    }
    return "Repeats every ${alarm.repeatInterval} weeks on $base"
}

@Composable
private fun fullDayName(d: DayOfWeek): String = when (d) {
    DayOfWeek.MONDAY -> stringResource(R.string.day_monday)
    DayOfWeek.TUESDAY -> "Tuesday"
    DayOfWeek.WEDNESDAY -> "Wednesday"
    DayOfWeek.THURSDAY -> "Thursday"
    DayOfWeek.FRIDAY -> "Friday"
    DayOfWeek.SATURDAY -> "Saturday"
    DayOfWeek.SUNDAY -> stringResource(R.string.day_sunday)
}

private fun Int.countSelectedDays(): Int = Integer.bitCount(this and DayMask.ALL_BITS)

@Composable
private fun ordinalLabel(ordinal: Int): String = when (ordinal) {
    1 -> stringResource(R.string.repeat_first).lowercase()
    2 -> stringResource(R.string.repeat_second).lowercase()
    3 -> stringResource(R.string.repeat_third).lowercase()
    4 -> stringResource(R.string.repeat_fourth).lowercase()
    else -> stringResource(R.string.repeat_last).lowercase()
}

private fun alarmRingOrderComparator(nowMillis: Long): Comparator<Alarm> =
    compareBy<Alarm>(
        { if (it.enabled) 0 else 1 },
        { alarmSortMillis(it, nowMillis) },
        { it.hour },
        { it.minute },
        { it.id },
    )

private fun alarmSortMillis(alarm: Alarm, nowMillis: Long): Long {
    if (!alarm.enabled) return Long.MAX_VALUE
    return alarm.snoozeUntilMillis?.takeIf { it > nowMillis }
        ?: AlarmScheduler.nextTriggerMillis(alarm, nowMillis)
        ?: Long.MAX_VALUE
}

private data class FireInfo(
    val referenceTime: String,
    val deviceTime: String,
    val remaining: String,
)

private fun nextFireInfo(alarm: Alarm, nowMillis: Long): FireInfo? {
    val next = alarm.snoozeUntilMillis?.takeIf { it > nowMillis }
        ?: AlarmScheduler.nextTriggerMillis(alarm, nowMillis)
        ?: return null
    val zone = runCatching { ZoneId.of(alarm.zoneId) }.getOrElse { ZoneId.systemDefault() }
    val instant = Instant.ofEpochMilli(next)
    val zoned = ZonedDateTime.ofInstant(instant, zone)
    val deviceZoned = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
    val durLabel = humanDuration(Duration.between(Instant.ofEpochMilli(nowMillis), instant))
    val fmt = DateTimeFormatter.ofPattern("MMM d h:mm a")
    return FireInfo(
        referenceTime = fmt.format(zoned),
        deviceTime = fmt.format(deviceZoned),
        remaining = durLabel,
    )
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
