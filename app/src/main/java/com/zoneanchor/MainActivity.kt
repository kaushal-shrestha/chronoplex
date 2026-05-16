package com.zoneanchor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zoneanchor.alarm.NotificationHelper
import com.zoneanchor.model.AppearanceMode
import com.zoneanchor.ui.components.AppScaffold
import com.zoneanchor.ui.nav.AppNavHost
import com.zoneanchor.ui.nav.Screen
import com.zoneanchor.ui.theme.ZoneAnchorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        NotificationHelper.requestNotificationPermissionIfNeeded(this)
        val app = application as ZoneAnchorApp
        setContent {
            val palette by app.settingsRepository.paletteFlow.collectAsStateWithLifecycle(
                initialValue = com.zoneanchor.model.AppPalette.DAYBREAK,
            )
            val appearance by app.settingsRepository.appearanceFlow.collectAsStateWithLifecycle(
                initialValue = AppearanceMode.SYSTEM,
            )
            val systemDark = isSystemInDarkTheme()
            val dark = when (appearance) {
                AppearanceMode.SYSTEM -> systemDark
                AppearanceMode.LIGHT -> false
                AppearanceMode.DARK -> true
            }
            ZoneAnchorTheme(palette = palette, appearanceMode = appearance) {
                val navController = rememberNavController()
                val backStack by navController.currentBackStackEntryAsState()
                val route = backStack?.destination?.route ?: Screen.Clocks.route
                val topRoute = topRouteFor(route)
                val barColor = MaterialTheme.colorScheme.surface.toArgb()
                SideEffect {
                    window.statusBarColor = barColor
                    window.navigationBarColor = barColor
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !dark
                        isAppearanceLightNavigationBars = !dark
                    }
                }
                AppScaffold(
                    currentTopRoute = topRoute,
                    showFab = route == Screen.Clocks.route || route == Screen.Alarms.route,
                    onTopRouteSelected = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Clocks.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onAdd = {
                        if (topRoute == Screen.Alarms.route) {
                            navController.navigate(Screen.AlarmEditor.create())
                        } else {
                            navController.navigate(Screen.ClockEditor.create())
                        }
                    },
                ) { padding ->
                    AppNavHost(navController, Modifier.padding(padding))
                }
            }
        }
    }

    private fun topRouteFor(route: String): String = when {
        route.startsWith("alarms") -> Screen.Alarms.route
        route.startsWith("settings") -> Screen.Settings.route
        else -> Screen.Clocks.route
    }
}
