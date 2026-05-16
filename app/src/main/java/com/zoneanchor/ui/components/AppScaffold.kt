package com.zoneanchor.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import com.zoneanchor.ui.nav.Screen

@Composable
fun AppScaffold(
    currentTopRoute: String,
    showFab: Boolean,
    onTopRouteSelected: (Screen) -> Unit,
    onAdd: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTopRoute == Screen.Clocks.route,
                    onClick = { onTopRouteSelected(Screen.Clocks) },
                    icon = { Icon(Icons.Default.Public, contentDescription = null) },
                    label = { androidx.compose.material3.Text("Clocks") },
                )
                NavigationBarItem(
                    selected = currentTopRoute == Screen.Alarms.route,
                    onClick = { onTopRouteSelected(Screen.Alarms) },
                    icon = { Icon(Icons.Default.Alarm, contentDescription = null) },
                    label = { androidx.compose.material3.Text("Alarms") },
                )
                NavigationBarItem(
                    selected = currentTopRoute == Screen.Settings.route,
                    onClick = { onTopRouteSelected(Screen.Settings) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { androidx.compose.material3.Text("Settings") },
                )
            }
        },
        content = content,
    )
}
