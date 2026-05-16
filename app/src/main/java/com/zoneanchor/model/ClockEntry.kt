package com.zoneanchor.model

data class ClockEntry(
    val zoneId: String,
    val label: String = "",
    val position: Int = 0,
) {
    val displayName: String get() = label.ifBlank { zoneId }
}
