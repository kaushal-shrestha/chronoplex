package com.zoneanchor.ui.nav

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.zoneanchor.ui.alarms.AlarmEditorScreen
import com.zoneanchor.ui.alarms.AlarmsScreen
import com.zoneanchor.ui.clocks.ClockEditorScreen
import com.zoneanchor.ui.clocks.ClocksScreen
import com.zoneanchor.ui.settings.SettingsScreen

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = Screen.Clocks.route, modifier = modifier) {
        composable(Screen.Clocks.route) {
            ClocksScreen(
                onEdit = { navController.navigate(Screen.ClockEditor.create(it)) },
            )
        }
        composable(Screen.Alarms.route) {
            AlarmsScreen(onEdit = { navController.navigate(Screen.AlarmEditor.create(it)) })
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
        composable(
            Screen.ClockEditor.route,
            arguments = listOf(navArgument("zoneId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }),
        ) {
            ClockEditorScreen(
                zoneId = it.arguments?.getString("zoneId")?.ifBlank { null }?.let(Uri::decode),
                onDone = { navController.popBackStack() },
            )
        }
        composable(
            Screen.AlarmEditor.route,
            arguments = listOf(navArgument("id") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }),
        ) {
            AlarmEditorScreen(
                alarmId = it.arguments?.getString("id")?.toIntOrNull(),
                onDone = { navController.popBackStack() },
            )
        }
    }
}
