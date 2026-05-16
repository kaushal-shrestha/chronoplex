package com.zoneanchor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.zoneanchor.model.ZonedAlarm

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayOfWeekPicker(
    selectedMask: Int,
    onSelectedMaskChange: (Int) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        days.forEachIndexed { index, label ->
            val bit = 1 shl index
            FilterChip(
                selected = (selectedMask and bit) != 0,
                onClick = {
                    val next = selectedMask xor bit
                    onSelectedMaskChange(next and ZonedAlarm.ALL_DAYS)
                },
                label = { Text(label) },
            )
        }
    }
}

fun daysSummary(mask: Int): String {
    val normalized = ZonedAlarm.normalizeDays(mask)
    return when (normalized) {
        ZonedAlarm.ALL_DAYS -> "Every day"
        0b0011111 -> "Weekdays"
        0b1100000 -> "Weekends"
        else -> days.filterIndexed { index, _ -> (normalized and (1 shl index)) != 0 }.joinToString(", ")
    }
}

private val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
