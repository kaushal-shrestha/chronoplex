package com.zoneanchor.app.ui

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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zoneanchor.app.AppContainer
import com.zoneanchor.app.R
import com.zoneanchor.app.ui.rememberTapFeedback
import com.zoneanchor.app.ui.screens.AlarmsScreen
import com.zoneanchor.app.ui.screens.ClocksScreen
import com.zoneanchor.app.ui.screens.SettingsScreen
import com.zoneanchor.app.ui.screens.StopwatchesScreen
import com.zoneanchor.app.ui.screens.TimersScreen

object Routes {
    const val CLOCKS = "clocks"
    const val ALARMS = "alarms"
    const val TIMERS = "timers"
    const val STOPWATCHES = "stopwatches"
    const val SETTINGS = "settings"
}

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
                        onClick = rememberTapFeedback { navigateTab(nav, Routes.CLOCKS) },
                        icon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_clocks)) },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.ALARMS,
                        onClick = rememberTapFeedback { navigateTab(nav, Routes.ALARMS) },
                        icon = { Icon(Icons.Default.Alarm, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_alarms)) },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.TIMERS,
                        onClick = rememberTapFeedback { navigateTab(nav, Routes.TIMERS) },
                        icon = { Icon(Icons.Default.HourglassEmpty, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_timers)) },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.STOPWATCHES,
                        onClick = rememberTapFeedback { navigateTab(nav, Routes.STOPWATCHES) },
                        icon = { Icon(Icons.Default.Timer, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_stopwatches)) },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.SETTINGS,
                        onClick = rememberTapFeedback { navigateTab(nav, Routes.SETTINGS) },
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
                val editVm: ClockEditViewModel = viewModel(factory = factory)
                ClocksScreen(vm = vm, editVm = editVm, container = container)
            }
            composable(Routes.ALARMS) {
                val vm: AlarmsViewModel = viewModel(factory = factory)
                val editVm: AlarmEditViewModel = viewModel(factory = factory)
                AlarmsScreen(
                    vm = vm,
                    editVm = editVm,
                    container = container,
                    onOpenExactAlarmSettings = onOpenExactAlarmSettings,
                )
            }
            composable(Routes.TIMERS) {
                val vm: TimersViewModel = viewModel(factory = factory)
                val editVm: TimerEditViewModel = viewModel(factory = factory)
                TimersScreen(vm = vm, editVm = editVm)
            }
            composable(Routes.STOPWATCHES) {
                val vm: StopwatchesViewModel = viewModel(factory = factory)
                StopwatchesScreen(vm = vm)
            }
            composable(Routes.SETTINGS) {
                val vm: SettingsViewModel = viewModel(factory = factory)
                SettingsScreen(vm = vm)
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
