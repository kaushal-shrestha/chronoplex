package com.zoneanchor.ui.nav

import android.net.Uri

sealed class Screen(val route: String) {
    data object Clocks : Screen("clocks")
    data object Alarms : Screen("alarms")
    data object Settings : Screen("settings")

    data object ClockEditor : Screen("clocks/editor?zoneId={zoneId}") {
        fun create(zoneId: String? = null) = "clocks/editor?zoneId=${Uri.encode(zoneId.orEmpty())}"
    }

    data object AlarmEditor : Screen("alarms/editor?id={id}") {
        fun create(id: Int? = null) = "alarms/editor?id=${id ?: ""}"
    }
}

val topLevelScreens = listOf(Screen.Clocks, Screen.Alarms, Screen.Settings)
