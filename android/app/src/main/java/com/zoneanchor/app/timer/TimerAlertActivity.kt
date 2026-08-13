package com.zoneanchor.app.timer

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.zoneanchor.app.R
import com.zoneanchor.app.ZoneAnchorApp
import com.zoneanchor.app.domain.AppearanceMode
import com.zoneanchor.app.domain.ThemePalette
import com.zoneanchor.app.domain.Timer
import com.zoneanchor.app.ui.theme.ZoneAnchorTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class TimerAlertActivity : ComponentActivity() {

    private val state = MutableStateFlow<Timer?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepareWakeScreen()

        // Don't allow back to accidentally leave an alert ringing.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })

        loadTimer(intent.getLongExtra(EXTRA_TIMER_ID, -1L))

        setContent {
            val app = LocalContext.current.applicationContext as ZoneAnchorApp
            val appearance by app.container.settings.appearance.collectAsState(initial = AppearanceMode.SYSTEM)
            val palette by app.container.settings.palette.collectAsState(initial = ThemePalette.Anchor)
            ZoneAnchorTheme(appearance = appearance, palette = palette) {
                val timer by state.collectAsState()
                TimerAlertScreen(
                    timer = timer,
                    onDismiss = { dismiss() },
                    onAddMinutes = { minutes -> addMinutes(minutes) },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        loadTimer(intent.getLongExtra(EXTRA_TIMER_ID, -1L))
    }

    private fun prepareWakeScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            km.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun loadTimer(timerId: Long) {
        if (timerId < 0L) {
            finish()
            return
        }
        val app = applicationContext as ZoneAnchorApp
        lifecycleScope.launch {
            state.value = app.container.timerRepo.getById(timerId)
        }
    }

    private fun dismiss() {
        val current = state.value ?: return finish()
        val app = applicationContext as ZoneAnchorApp
        lifecycleScope.launch {
            app.container.timerScheduler.dismiss(current)
            finish()
        }
    }

    private fun addMinutes(minutes: Int) {
        val current = state.value ?: return finish()
        val app = applicationContext as ZoneAnchorApp
        lifecycleScope.launch {
            app.container.timerScheduler.addMinutes(current, minutes)
            finish()
        }
    }

    companion object {
        const val EXTRA_TIMER_ID = "extra_timer_id"
    }
}

@androidx.compose.runtime.Composable
private fun TimerAlertScreen(
    timer: Timer?,
    onDismiss: () -> Unit,
    onAddMinutes: (Int) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.42f))
            TimerAlertIdentity(timer = timer)
            Spacer(Modifier.weight(0.58f))
            TimerAlertActions(onDismiss = onDismiss, onAddMinutes = onAddMinutes)
        }
    }
}

@androidx.compose.runtime.Composable
private fun TimerAlertIdentity(timer: Timer?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(116.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)),
            ) {}
            Icon(
                Icons.Default.HourglassEmpty,
                contentDescription = null,
                modifier = Modifier.size(62.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(30.dp))
        Text(
            text = stringResource(R.string.timer_alert_title),
            fontSize = 52.sp,
            lineHeight = 58.sp,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = timer?.label?.ifBlank { stringResource(R.string.timer_finished_default_title) }
                ?: stringResource(R.string.timer_finished_default_title),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.timer_alert_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@androidx.compose.runtime.Composable
private fun TimerAlertActions(
    onDismiss: () -> Unit,
    onAddMinutes: (Int) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.74f),
        shape = RoundedCornerShape(32.dp),
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.add_time),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                listOf(1, 5, 10).forEach { mins ->
                    OutlinedButton(
                        onClick = { onAddMinutes(mins) },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) { Text(stringResource(R.string.snooze_minutes, mins)) }
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) { Text(stringResource(R.string.stop)) }
        }
    }
}
