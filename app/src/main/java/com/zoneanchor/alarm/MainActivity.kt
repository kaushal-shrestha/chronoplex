package com.zoneanchor.alarm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.zoneanchor.alarm.domain.AppearanceMode
import com.zoneanchor.alarm.domain.ThemePalette
import com.zoneanchor.alarm.ui.AppRoot
import com.zoneanchor.alarm.ui.theme.ZoneAnchorTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* user choice — handled by system */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        requestNotificationPermissionIfNeeded()

        val app = applicationContext as ZoneAnchorApp
        setContent {
            val appearance by app.container.settings.appearance.collectAsState(initial = AppearanceMode.SYSTEM)
            val palette by app.container.settings.palette.collectAsState(initial = ThemePalette.Anchor)
            ZoneAnchorTheme(appearance = appearance, palette = palette) {
                AppRoot(
                    container = app.container,
                    onOpenExactAlarmSettings = ::openExactAlarmSettings,
                )
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    .setData(Uri.parse("package:$packageName")),
            )
        }
    }

    companion object {
        const val EXTRA_OPEN_ALARM_ID = "extra_open_alarm_id"
    }
}
