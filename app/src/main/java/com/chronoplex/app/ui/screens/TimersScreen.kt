package com.chronoplex.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronoplex.app.R
import com.chronoplex.app.domain.Timer
import com.chronoplex.app.domain.TimerState
import com.chronoplex.app.ui.TimersViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimersScreen(
    vm: TimersViewModel,
    onAdd: () -> Unit,
    onEdit: (Timer) -> Unit,
) {
    val timers by vm.timers.collectAsState()
    // Single per-screen tick drives every running timer's countdown.
    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(500L)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_timers)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_timer))
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (timers.isEmpty()) {
                    item { EmptyState(R.string.empty_timers_title, R.string.empty_timers_body) }
                } else {
                    items(timers, key = { it.id }) { timer ->
                        TimerRow(
                            timer = timer,
                            nowMillis = now,
                            onClick = { onEdit(timer) },
                            onPrimaryAction = {
                                when (timer.state) {
                                    TimerState.IDLE, TimerState.PAUSED, TimerState.FINISHED -> vm.start(timer)
                                    TimerState.RUNNING -> vm.pause(timer)
                                }
                            },
                            onReset = { vm.reset(timer) },
                            onDelete = { vm.delete(timer) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimerRow(
    timer: Timer,
    nowMillis: Long,
    onClick: () -> Unit,
    onPrimaryAction: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
) {
    val remaining = timer.remainingMillis(nowMillis)
    val progress = if (timer.durationMillis > 0L) {
        1f - (remaining.toFloat() / timer.durationMillis.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    val label = timer.label.ifBlank { stateLabel(timer.state) }
                    Text(label, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        formatDuration(remaining),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (timer.state == TimerState.FINISHED) MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        stateLabel(timer.state),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onPrimaryAction) {
                    Icon(
                        when (timer.state) {
                            TimerState.RUNNING -> Icons.Default.Pause
                            else -> Icons.Default.PlayArrow
                        },
                        contentDescription = when (timer.state) {
                            TimerState.RUNNING -> stringResource(R.string.pause)
                            TimerState.PAUSED -> stringResource(R.string.resume)
                            else -> stringResource(R.string.start)
                        },
                    )
                }
                IconButton(onClick = onReset) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.reset))
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (timer.state == TimerState.RUNNING || timer.state == TimerState.PAUSED) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun stateLabel(state: TimerState): String = when (state) {
    TimerState.IDLE -> stringResource(R.string.timer_state_idle)
    TimerState.RUNNING -> stringResource(R.string.timer_state_running)
    TimerState.PAUSED -> stringResource(R.string.timer_state_paused)
    TimerState.FINISHED -> stringResource(R.string.timer_state_finished)
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
