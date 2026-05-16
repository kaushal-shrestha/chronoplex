package com.zoneanchor.model

enum class AppearanceMode(val legacyId: String, val title: String) {
    SYSTEM("system", "Follow system"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    companion object {
        fun fromName(value: String?) = entries.firstOrNull { it.name == value } ?: SYSTEM
        fun fromLegacyId(value: String?) = entries.firstOrNull { it.legacyId == value } ?: SYSTEM
    }
}
