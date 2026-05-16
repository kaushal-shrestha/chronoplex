package com.zoneanchor.ui.clocks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zoneanchor.ZoneAnchorApp
import com.zoneanchor.model.ClockEntry
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

@Composable
fun ClocksScreen(onEdit: (String?) -> Unit) {
    val app = LocalContext.current.applicationContext as ZoneAnchorApp
    val viewModel: ClocksViewModel = viewModel(factory = ClocksViewModel.factory(app))
    val clocks by viewModel.clocks.collectAsStateWithLifecycle()
    var tick by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            tick++
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ClockCard(
                clock = ClockEntry(ZoneId.systemDefault().id, "Device time"),
                tick = tick,
                canEdit = false,
                onEdit = {},
                onDelete = {},
            )
        }
        items(clocks, key = { it.zoneId }) { clock ->
            ClockCard(
                clock = clock,
                tick = tick,
                canEdit = true,
                onEdit = { onEdit(clock.zoneId) },
                onDelete = { viewModel.delete(clock.zoneId) },
            )
        }
    }
}

@Composable
private fun ClockCard(
    clock: ClockEntry,
    tick: Long,
    canEdit: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val now = remember(tick, clock.zoneId) { ZonedDateTime.now(ZoneId.of(clock.zoneId)) }
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                Text(clock.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    timeFormat.format(now),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "${clock.zoneId} — ${dateFormat.format(now)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (canEdit) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit clock")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete clock")
                }
            }
        }
    }
}

private val timeFormat = DateTimeFormatter.ofPattern("h:mm:ss a")
private val dateFormat = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")
