package com.zoneanchor.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.zoneanchor.model.AppPalette

fun colorSchemeFor(palette: AppPalette, dark: Boolean): ColorScheme {
    val primary = Color(if (dark) palette.darkAccent else palette.lightPrimary)
    return if (dark) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color(0xFF08111F),
            secondary = primary,
            background = Color(0xFF0F172A),
            onBackground = Color(0xFFF8FAFC),
            surface = Color(0xFF1E293B),
            onSurface = Color(0xFFF8FAFC),
            surfaceVariant = Color(0xFF334155),
            onSurfaceVariant = Color(0xFFCBD5E1),
            outline = Color(0xFF475569),
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            secondary = primary,
            background = Color(palette.lightBackground),
            onBackground = Color(0xFF0F172A),
            surface = Color(palette.lightSurface),
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFE2E8F0),
            onSurfaceVariant = Color(0xFF435168),
            outline = Color(0xFFE1E7EF),
        )
    }
}

fun Long.toComposeColor() = Color(this)
