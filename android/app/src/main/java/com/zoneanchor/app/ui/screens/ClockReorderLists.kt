package com.zoneanchor.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.zoneanchor.app.R
import com.zoneanchor.app.domain.Clock
import com.zoneanchor.app.domain.Group
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
internal fun ClocksReorderableList(
    clocks: List<Clock>,
    onReorder: (List<Long>) -> Unit,
) {
    // Local mirror of the upstream list so the LazyColumn can show the drag in flight.
    // Resync from upstream when the set of ids changes (e.g., delete from elsewhere).
    var local by remember(clocks.map { it.id }) { mutableStateOf(clocks) }

    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        local = local.toMutableList().apply {
            val fromIdx = indexOfFirst { it.id == from.key as Long }
            val toIdx = indexOfFirst { it.id == to.key as Long }
            if (fromIdx in indices && toIdx in indices) add(toIdx, removeAt(fromIdx))
        }
        onReorder(local.map { it.id })
    }

    Column {
        Text(
            stringResource(R.string.reorder_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(local, key = { it.id }) { clock ->
                ReorderableItem(reorderState, key = clock.id) {
                    Card(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.DragHandle,
                                contentDescription = stringResource(R.string.reorder),
                                modifier = Modifier.draggableHandle().size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(clock.label, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    clock.zoneId,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Grouped reorder list. Items can be reordered within their group only; group headers
 * act as fixed anchors. Group reordering stays in ManageGroupsDialog.
 */
@Composable
internal fun ClocksGroupedReorderableList(
    clocks: List<Clock>,
    groups: List<Group>,
    onReorder: (List<Long>) -> Unit,
) {
    var local by remember(clocks.map { it.id }) { mutableStateOf(clocks) }
    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        onMove = { from, to ->
            val fromId = from.key as? Long ?: return@rememberReorderableLazyListState
            val toId = to.key as? Long ?: return@rememberReorderableLazyListState
            val fromItem = local.firstOrNull { it.id == fromId } ?: return@rememberReorderableLazyListState
            val toItem = local.firstOrNull { it.id == toId } ?: return@rememberReorderableLazyListState
            if (fromItem.groupId != toItem.groupId) return@rememberReorderableLazyListState

            local = local.toMutableList().apply {
                val fromIdx = indexOfFirst { it.id == fromId }
                val toIdx = indexOfFirst { it.id == toId }
                if (fromIdx in indices && toIdx in indices) add(toIdx, removeAt(fromIdx))
            }
            val affectedIds = local.filter { it.groupId == fromItem.groupId }.map { it.id }
            onReorder(affectedIds)
        },
    )

    Column {
        Text(
            stringResource(R.string.reorder_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val byGroup = local.groupBy { it.groupId }
            groups.forEach { g ->
                val members = byGroup[g.id].orEmpty()
                item(key = "h-${g.id}") {
                    GroupHeader(g.copy(collapsed = false), members.size, onToggleCollapsed = { })
                }
                items(members, key = { it.id }) { clock ->
                    ReorderableItem(reorderState, key = clock.id) {
                        ClockDragRow(clock = clock)
                    }
                }
            }
            val ungrouped = byGroup[null].orEmpty()
            if (ungrouped.isNotEmpty()) {
                item(key = "h-ungrouped") {
                    UngroupedHeader(itemCount = ungrouped.size, collapsed = false, onToggleCollapsed = { })
                }
                items(ungrouped, key = { it.id }) { clock ->
                    ReorderableItem(reorderState, key = clock.id) {
                        ClockDragRow(clock = clock)
                    }
                }
            }
        }
    }
}

@Composable
private fun sh.calvin.reorderable.ReorderableCollectionItemScope.ClockDragRow(clock: Clock) {
    Card(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.DragHandle,
                contentDescription = stringResource(R.string.reorder),
                modifier = Modifier.draggableHandle().size(28.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    clock.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    clock.zoneId,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
