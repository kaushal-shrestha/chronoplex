package com.zoneanchor.app

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
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.zoneanchor.app.domain.AppearanceMode
import com.zoneanchor.app.domain.ThemePalette
import com.zoneanchor.app.ui.AppRoot
import com.zoneanchor.app.ui.theme.ZoneAnchorTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* user choice — handled by system */ }
    private val openTimerId = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        openTimerId.value = intent.openTimerId()

        requestNotificationPermissionIfNeeded()

        val app = applicationContext as ZoneAnchorApp
        setContent {
            val appearance by app.container.settings.appearance.collectAsState(initial = AppearanceMode.SYSTEM)
            val palette by app.container.settings.palette.collectAsState(initial = ThemePalette.Anchor)
            ZoneAnchorTheme(appearance = appearance, palette = palette) {
                AppRoot(
                    container = app.container,
                    onOpenExactAlarmSettings = ::openExactAlarmSettings,
                    openTimerId = openTimerId.value,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openTimerId.value = intent.openTimerId()
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
        const val EXTRA_OPEN_TIMER_ID = "extra_open_timer_id"
    }
}

private fun Intent.openTimerId(): Long? =
    getLongExtra(MainActivity.EXTRA_OPEN_TIMER_ID, -1L).takeIf { it >= 0L }
