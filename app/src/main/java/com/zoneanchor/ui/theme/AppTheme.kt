package com.zoneanchor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.zoneanchor.model.AppPalette
import com.zoneanchor.model.AppearanceMode

@Composable
fun ZoneAnchorTheme(
    palette: AppPalette,
    appearanceMode: AppearanceMode,
    content: @Composable () -> Unit,
) {
    val dark = when (appearanceMode) {
        AppearanceMode.SYSTEM -> isSystemInDarkTheme()
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = colorSchemeFor(palette, dark),
        typography = androidx.compose.material3.Typography(),
        content = content,
    )
}
