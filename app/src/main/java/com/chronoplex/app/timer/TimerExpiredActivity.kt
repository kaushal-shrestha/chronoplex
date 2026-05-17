package com.chronoplex.app.timer

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.chronoplex.app.ChronoplexApp
import com.chronoplex.app.R
import com.chronoplex.app.domain.AppearanceMode
import com.chronoplex.app.domain.ThemePalette
import com.chronoplex.app.domain.Timer
import com.chronoplex.app.ui.theme.ChronoplexTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class TimerExpiredActivity : ComponentActivity() {

    private val state = MutableStateFlow<Timer?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
                .requestDismissKeyguard(this, null)
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

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })

        val id = intent.getLongExtra(EXTRA_TIMER_ID, -1L)
        loadTimer(id)

        setContent {
            val app = LocalContext.current.applicationContext as ChronoplexApp
            val appearance by app.container.settings.appearance.collectAsState(initial = AppearanceMode.SYSTEM)
            val palette by app.container.settings.palette.collectAsState(initial = ThemePalette.Anchor)
            ChronoplexTheme(appearance = appearance, palette = palette) {
                val t by state.collectAsState()
                ExpiredScreen(
                    timer = t,
                    onDismiss = ::dismiss,
                    onAddMinute = ::addMinute,
                )
            }
        }
    }

    private fun loadTimer(id: Long) {
        if (id < 0) return finish()
        val app = applicationContext as ChronoplexApp
        lifecycleScope.launch {
            state.value = app.container.timerRepo.getById(id)
        }
    }

    private fun dismiss() {
        val current = state.value ?: return finish()
        val app = applicationContext as ChronoplexApp
        lifecycleScope.launch {
            app.container.timerScheduler.dismiss(current)
            finish()
        }
    }

    private fun addMinute() {
        val current = state.value ?: return finish()
        val app = applicationContext as ChronoplexApp
        lifecycleScope.launch {
            app.container.timerScheduler.addMinute(current)
            finish()
        }
    }

    companion object {
        const val EXTRA_TIMER_ID = "extra_timer_id"
    }
}

@androidx.compose.runtime.Composable
private fun ExpiredScreen(
    timer: Timer?,
    onDismiss: () -> Unit,
    onAddMinute: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.HourglassEmpty,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        timer?.label?.ifBlank { stringResource(R.string.timer_finished_default_title) }
                            ?: stringResource(R.string.timer_finished_default_title),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.timer_finished_body),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            OutlinedButton(
                onClick = onAddMinute,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) { Text(stringResource(R.string.add_minute)) }
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) { Text(stringResource(R.string.stop)) }
        }
    }
}
