package com.chronoplex.app.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.PublicOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chronoplex.app.AppContainer
import com.chronoplex.app.R
import com.chronoplex.app.ui.screens.AlarmEditScreen
import com.chronoplex.app.ui.screens.AlarmsScreen
import com.chronoplex.app.ui.screens.ClockEditScreen
import com.chronoplex.app.ui.screens.ClocksScreen
import com.chronoplex.app.ui.screens.SettingsScreen
import com.chronoplex.app.ui.screens.StopwatchesScreen
import com.chronoplex.app.ui.screens.TimerEditScreen
import com.chronoplex.app.ui.screens.TimersScreen
import com.chronoplex.app.ui.screens.ZonePickerScreen

object Routes {
    const val CLOCKS = "clocks"
    const val ALARMS = "alarms"
    const val TIMERS = "timers"
    const val STOPWATCHES = "stopwatches"
    const val SETTINGS = "settings"
    const val CLOCK_EDIT = "clock_edit"
    const val ALARM_EDIT = "alarm_edit"
    const val TIMER_EDIT = "timer_edit"
    const val ZONE_PICKER = "zone_picker"

    fun clockEdit(id: Long = 0L) = "$CLOCK_EDIT?id=$id"
    fun alarmEdit(id: Long = 0L) = "$ALARM_EDIT?id=$id"
    fun timerEdit(id: Long = 0L) = "$TIMER_EDIT?id=$id"
    fun zonePicker(restrictToAdded: Boolean) = "$ZONE_PICKER?restrict=$restrictToAdded"
}

const val SELECTED_ZONE_KEY = "selected_zone_id"

@Composable
fun AppRoot(
    container: AppContainer,
    onOpenExactAlarmSettings: () -> Unit,
) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val factory = remember(container) { AppViewModelFactory(container) }

    val tabRoutes = setOf(Routes.CLOCKS, Routes.ALARMS, Routes.TIMERS, Routes.STOPWATCHES, Routes.SETTINGS)
    val showBottomBar = currentRoute in tabRoutes

    Scaffold(
        // Each child screen has its own Scaffold + TopAppBar, which handles the
        // status-bar/cutout inset on its own. The bottomBar below also consumes
        // its own bottom inset. Without this override, the outer Scaffold would
        // ALSO add the top status-bar inset to its content padding, producing
        // visible double-spacing at the top of every screen.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.CLOCKS,
                        onClick = { navigateTab(nav, Routes.CLOCKS) },
                        icon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_clocks)) },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.ALARMS,
                        onClick = { navigateTab(nav, Routes.ALARMS) },
                        icon = { Icon(Icons.Default.Alarm, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_alarms)) },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.TIMERS,
                        onClick = { navigateTab(nav, Routes.TIMERS) },
                        icon = { Icon(Icons.Default.HourglassEmpty, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_timers)) },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.STOPWATCHES,
                        onClick = { navigateTab(nav, Routes.STOPWATCHES) },
                        icon = { Icon(Icons.Default.Timer, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_stopwatches)) },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.SETTINGS,
                        onClick = { navigateTab(nav, Routes.SETTINGS) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_settings)) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.CLOCKS,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            composable(Routes.CLOCKS) {
                val vm: ClocksViewModel = viewModel(factory = factory)
                ClocksScreen(
                    vm = vm,
                    onAdd = { nav.navigate(Routes.clockEdit()) },
                    onEdit = { nav.navigate(Routes.clockEdit(it.id)) },
                )
            }
            composable(Routes.ALARMS) {
                val vm: AlarmsViewModel = viewModel(factory = factory)
                AlarmsScreen(
                    vm = vm,
                    onOpenExactAlarmSettings = onOpenExactAlarmSettings,
                    onAdd = { nav.navigate(Routes.alarmEdit()) },
                    onEdit = { nav.navigate(Routes.alarmEdit(it.id)) },
                )
            }
            composable(Routes.TIMERS) {
                val vm: TimersViewModel = viewModel(factory = factory)
                TimersScreen(
                    vm = vm,
                    onAdd = { nav.navigate(Routes.timerEdit()) },
                    onEdit = { nav.navigate(Routes.timerEdit(it.id)) },
                )
            }
            composable(
                "${Routes.TIMER_EDIT}?id={id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L }),
            ) {
                val vm: TimerEditViewModel = viewModel(factory = factory)
                TimerEditScreen(vm = vm, onClose = { nav.popBackStack() })
            }
            composable(Routes.STOPWATCHES) {
                val vm: StopwatchesViewModel = viewModel(factory = factory)
                StopwatchesScreen(vm = vm)
            }
            composable(Routes.SETTINGS) {
                val vm: SettingsViewModel = viewModel(factory = factory)
                SettingsScreen(vm = vm)
            }
            composable(
                "${Routes.CLOCK_EDIT}?id={id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L }),
            ) { entry ->
                val vm: ClockEditViewModel = viewModel(factory = factory)
                val savedHandle = entry.savedStateHandle
                val zoneFlow = remember(savedHandle) {
                    savedHandle.getStateFlow<String?>(SELECTED_ZONE_KEY, null)
                }
                val selectedZone by zoneFlow.collectAsState()
                ClockEditScreen(
                    vm = vm,
                    onPickZone = { nav.navigate(Routes.zonePicker(restrictToAdded = false)) },
                    selectedZoneFromPicker = selectedZone,
                    onZoneConsumed = { savedHandle[SELECTED_ZONE_KEY] = null },
                    onClose = { nav.popBackStack() },
                )
            }
            composable(
                "${Routes.ALARM_EDIT}?id={id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L }),
            ) { entry ->
                val vm: AlarmEditViewModel = viewModel(factory = factory)
                val savedHandle = entry.savedStateHandle
                val zoneFlow = remember(savedHandle) {
                    savedHandle.getStateFlow<String?>(SELECTED_ZONE_KEY, null)
                }
                val selectedZone by zoneFlow.collectAsState()
                AlarmEditScreen(
                    vm = vm,
                    onPickZone = { restrict -> nav.navigate(Routes.zonePicker(restrictToAdded = restrict)) },
                    selectedZoneFromPicker = selectedZone,
                    onZoneConsumed = { savedHandle[SELECTED_ZONE_KEY] = null },
                    onClose = { nav.popBackStack() },
                )
            }
            composable(
                "${Routes.ZONE_PICKER}?restrict={restrict}",
                arguments = listOf(navArgument("restrict") { type = NavType.BoolType; defaultValue = false }),
            ) { entry ->
                val restrict = entry.arguments?.getBoolean("restrict") ?: false
                ZonePickerScreen(
                    restrictToAdded = restrict,
                    container = container,
                    onPick = { zoneId ->
                        nav.previousBackStackEntry?.savedStateHandle?.set(SELECTED_ZONE_KEY, zoneId)
                        nav.popBackStack()
                    },
                    onClose = { nav.popBackStack() },
                )
            }
        }
    }
}

private fun navigateTab(nav: androidx.navigation.NavController, route: String) {
    nav.navigate(route) {
        popUpTo(nav.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
