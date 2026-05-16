package com.zoneanchor.model

enum class AppPalette(
    val legacyId: String,
    val title: String,
    val description: String,
    val lightBackground: Long,
    val lightSurface: Long,
    val lightPrimary: Long,
    val darkAccent: Long,
) {
    DAYBREAK(
        "daybreak",
        "Daybreak",
        "Clean paper, deep ink, and calm teal.",
        0xFFF7F9FC,
        0xFFFFFFFF,
        0xFF006C67,
        0xFF2DD4BF,
    ),
    HARBOR(
        "harbor",
        "Harbor",
        "Soft blue-gray with a crisp marine accent.",
        0xFFEFF6FA,
        0xFFFDFEFF,
        0xFF005B8C,
        0xFF60A5FA,
    ),
    GROVE(
        "grove",
        "Grove",
        "A green workspace with warm ivory surfaces.",
        0xFFF1F7F1,
        0xFFFFFDF7,
        0xFF237046,
        0xFF4ADE80,
    ),
    EMBER(
        "ember",
        "Ember",
        "Warm rose accents on a quiet neutral base.",
        0xFFFAF6F4,
        0xFFFFFFFF,
        0xFFAD4543,
        0xFFFB7185,
    ),
    TWILIGHT(
        "twilight",
        "Twilight",
        "Cool slate tones with a bright evening accent.",
        0xFFEEF2F8,
        0xFFFCFDFF,
        0xFF5C5BB0,
        0xFFA78BFA,
    );

    companion object {
        fun fromName(value: String?) = entries.firstOrNull { it.name == value } ?: DAYBREAK
        fun fromLegacyId(value: String?) = entries.firstOrNull { it.legacyId == value } ?: DAYBREAK
    }
}
