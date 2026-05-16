package com.zoneanchor.alarm.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.zoneanchor.alarm.domain.AppearanceMode
import com.zoneanchor.alarm.domain.ThemePalette

private data class PaletteSpec(
    val light: ColorScheme,
    val dark: ColorScheme,
)

private fun palette(primary: Color, secondary: Color, tertiary: Color): PaletteSpec {
    val light = lightColorScheme(
        primary = primary,
        secondary = secondary,
        tertiary = tertiary,
    )
    val dark = darkColorScheme(
        primary = primary.lighten(0.25f),
        secondary = secondary.lighten(0.2f),
        tertiary = tertiary.lighten(0.2f),
    )
    return PaletteSpec(light, dark)
}

private fun Color.lighten(amount: Float): Color {
    return Color(
        red = (red + (1f - red) * amount).coerceIn(0f, 1f),
        green = (green + (1f - green) * amount).coerceIn(0f, 1f),
        blue = (blue + (1f - blue) * amount).coerceIn(0f, 1f),
        alpha = alpha,
    )
}

private val palettes: Map<ThemePalette, PaletteSpec> = mapOf(
    ThemePalette.Anchor to palette(
        primary = Color(0xFF2E5FB7),
        secondary = Color(0xFF4A7AC2),
        tertiary = Color(0xFF6F9BD8),
    ),
    ThemePalette.Sunrise to palette(
        primary = Color(0xFFE0664B),
        secondary = Color(0xFFE89461),
        tertiary = Color(0xFFE9B872),
    ),
    ThemePalette.Forest to palette(
        primary = Color(0xFF2F7D5E),
        secondary = Color(0xFF4F9C7C),
        tertiary = Color(0xFF89B98F),
    ),
    ThemePalette.Slate to palette(
        primary = Color(0xFF4C5664),
        secondary = Color(0xFF6C7585),
        tertiary = Color(0xFF98A0AE),
    ),
    ThemePalette.Plum to palette(
        primary = Color(0xFF7A3E8F),
        secondary = Color(0xFF9B5BB5),
        tertiary = Color(0xFFC586D8),
    ),
)

@Composable
fun ZoneAnchorTheme(
    appearance: AppearanceMode,
    palette: ThemePalette,
    content: @Composable () -> Unit,
) {
    val useDark = when (appearance) {
        AppearanceMode.SYSTEM -> isSystemInDarkTheme()
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
    }

    val context = LocalContext.current
    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val spec = palettes[palette] ?: palettes.getValue(ThemePalette.Anchor)

    val scheme = when {
        // We only use dynamic color when the user has chosen "Anchor" as their palette
        // and is on Android 12+, so the selectable palettes always feel like a real choice.
        palette == ThemePalette.Anchor && dynamicAvailable ->
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        useDark -> spec.dark
        else -> spec.light
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Edge-to-edge: system bars are transparent by default on API 35+.
            // We only need to flip the icon brightness based on the chosen theme.
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !useDark
            controller.isAppearanceLightNavigationBars = !useDark
        }
    }

    MaterialTheme(colorScheme = scheme, content = content)
}
