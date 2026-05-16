package com.zoneanchor.alarm.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import com.zoneanchor.alarm.R
import com.zoneanchor.alarm.domain.Clock
import com.zoneanchor.alarm.ui.ClocksViewModel
import kotlinx.coroutines.delay
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClocksScreen(
    vm: ClocksViewModel,
    onAdd: () -> Unit,
    onEdit: (Clock) -> Unit,
) {
    val clocks by vm.clocks.collectAsState()
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_clocks)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_clock))
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            ClocksList(
                clocks = clocks,
                onEdit = onEdit,
                onDelete = { vm.delete(it.id) },
            )
        }
    }
}

@Composable
private fun ClocksList(
    clocks: List<Clock>,
    onEdit: (Clock) -> Unit,
    onDelete: (Clock) -> Unit,
) {
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = ZonedDateTime.now()
            delay(1000)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { LocalTimeCard(now) }
        if (clocks.isEmpty()) {
            item { EmptyState(R.string.empty_clocks_title, R.string.empty_clocks_body) }
        } else {
            items(clocks, key = { it.id }) { clock ->
                ClockRow(clock = clock, nowEpochMillis = now.toInstant().toEpochMilli(),
                    onClick = { onEdit(clock) }, onDelete = { onDelete(clock) })
            }
        }
    }
}

@Composable
private fun LocalTimeCard(now: ZonedDateTime) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                stringResource(R.string.local_time),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(4.dp))
            val fmt = remember { DateTimeFormatter.ofPattern("h:mm:ss a") }
            Text(
                fmt.format(now),
                fontSize = 36.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                ZoneId.systemDefault().id,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun ClockRow(
    clock: Clock,
    nowEpochMillis: Long,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val zoneTime = remember(clock.zoneId, nowEpochMillis) {
        runCatching {
            ZonedDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(nowEpochMillis),
                ZoneId.of(clock.zoneId),
            )
        }.getOrNull()
    }
    val fmt = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(clock.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    clock.zoneId,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (zoneTime != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        dateFmt.format(zoneTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    zoneTime?.let { fmt.format(it) } ?: "—",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun EmptyState(titleRes: Int, bodyRes: Int) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.Schedule,
            contentDescription = null,
            modifier = Modifier.padding(8.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(bodyRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
