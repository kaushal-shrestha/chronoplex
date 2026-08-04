package com.zoneanchor.app.ui.theme

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
import com.zoneanchor.app.domain.AppearanceMode
import com.zoneanchor.app.domain.ThemePalette
import kotlin.math.pow

private data class PaletteSpec(
    val light: ColorScheme,
    val dark: ColorScheme,
)

private fun palette(
    primary: Color,
    secondary: Color,
    tertiary: Color,
    surfaceTint: Color = tertiary,
): PaletteSpec {
    val lightSurface = surfaceTint.blendWith(Color.White, 0.93f)
    val lightSurfaceVariant = surfaceTint.blendWith(Color(0xFFDDE5EA), 0.56f)
    val lightSurfaceContainer = surfaceTint.blendWith(Color.White, 0.86f)
    val lightSurfaceContainerHigh = surfaceTint.blendWith(Color.White, 0.80f)
    val darkSurface = primary.blendWith(Color(0xFF111820), 0.84f)
    val darkSurfaceVariant = primary.blendWith(Color(0xFF27323C), 0.68f)
    val darkSurfaceContainer = primary.blendWith(Color(0xFF17212A), 0.76f)
    val darkSurfaceContainerHigh = primary.blendWith(Color(0xFF1E2A35), 0.70f)

    val light = lightColorScheme(
        primary = primary,
        onPrimary = primary.contentColor(),
        primaryContainer = primary.blendWith(Color.White, 0.80f),
        onPrimaryContainer = primary.darken(0.56f),
        inversePrimary = primary.lighten(0.42f),
        secondary = secondary,
        onSecondary = secondary.contentColor(),
        secondaryContainer = secondary.blendWith(Color.White, 0.76f),
        onSecondaryContainer = secondary.darken(0.58f),
        tertiary = tertiary,
        onTertiary = tertiary.contentColor(),
        tertiaryContainer = tertiary.blendWith(Color.White, 0.68f),
        onTertiaryContainer = tertiary.darken(0.62f),
        background = lightSurface,
        onBackground = Color(0xFF181C20),
        surface = lightSurface,
        onSurface = Color(0xFF181C20),
        surfaceVariant = lightSurfaceVariant,
        onSurfaceVariant = Color(0xFF4C525B),
        surfaceTint = primary,
        inverseSurface = Color(0xFF2D3136),
        inverseOnSurface = Color(0xFFF2F4F7),
        outline = Color(0xFF7B818A),
        outlineVariant = lightSurfaceVariant.darken(0.10f),
        surfaceBright = lightSurface.blendWith(Color.White, 0.54f),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = lightSurfaceContainer.blendWith(Color.White, 0.28f),
        surfaceContainer = lightSurfaceContainer,
        surfaceContainerHigh = lightSurfaceContainerHigh,
        surfaceContainerHighest = lightSurfaceContainerHigh.darken(0.035f),
    )
    val dark = darkColorScheme(
        primary = primary.lighten(0.38f),
        onPrimary = primary.lighten(0.38f).contentColor(),
        primaryContainer = primary.darken(0.18f),
        onPrimaryContainer = primary.lighten(0.82f),
        inversePrimary = primary,
        secondary = secondary.lighten(0.22f),
        onSecondary = secondary.lighten(0.22f).contentColor(),
        secondaryContainer = secondary.darken(0.26f),
        onSecondaryContainer = secondary.lighten(0.80f),
        tertiary = tertiary.lighten(0.14f),
        onTertiary = tertiary.lighten(0.14f).contentColor(),
        tertiaryContainer = tertiary.darken(0.34f),
        onTertiaryContainer = tertiary.lighten(0.70f),
        background = darkSurface,
        onBackground = Color(0xFFE7ECF2),
        surface = darkSurface,
        onSurface = Color(0xFFE7ECF2),
        surfaceVariant = darkSurfaceVariant,
        onSurfaceVariant = Color(0xFFC1CBD6),
        surfaceTint = primary.lighten(0.38f),
        inverseSurface = Color(0xFFE7ECF2),
        inverseOnSurface = Color(0xFF20252A),
        outline = Color(0xFF8D98A4),
        outlineVariant = darkSurfaceVariant.lighten(0.08f),
        surfaceDim = darkSurface.darken(0.12f),
        surfaceContainerLowest = darkSurface.darken(0.20f),
        surfaceContainerLow = darkSurfaceContainer.darken(0.05f),
        surfaceContainer = darkSurfaceContainer,
        surfaceContainerHigh = darkSurfaceContainerHigh,
        surfaceContainerHighest = darkSurfaceContainerHigh.lighten(0.05f),
    )
    return PaletteSpec(light, dark)
}

private fun Color.blendWith(other: Color, amount: Float): Color {
    val weight = amount.coerceIn(0f, 1f)
    return Color(
        red = red + (other.red - red) * weight,
        green = green + (other.green - green) * weight,
        blue = blue + (other.blue - blue) * weight,
        alpha = alpha + (other.alpha - alpha) * weight,
    )
}

private fun Color.lighten(amount: Float): Color {
    return blendWith(Color.White, amount)
}

private fun Color.darken(amount: Float): Color {
    return blendWith(Color.Black, amount)
}

private fun Color.contentColor(): Color =
    if (relativeLuminance() > 0.48f) Color(0xFF161A1F) else Color.White

private fun Color.relativeLuminance(): Float {
    fun channel(value: Float): Float =
        if (value <= 0.03928f) value / 12.92f else ((value + 0.055f) / 1.055f).pow(2.4f)
    return 0.2126f * channel(red) + 0.7152f * channel(green) + 0.0722f * channel(blue)
}

private val palettes: Map<ThemePalette, PaletteSpec> = mapOf(
    ThemePalette.Anchor to palette(
        primary = Color(0xFF2E5FB7),
        secondary = Color(0xFF4A7AC2),
        tertiary = Color(0xFF6F9BD8),
    ),
    // Codex's five — ported faithfully from CODEX_REWRITE_SPEC.md palette colors.
    ThemePalette.Daybreak to palette(
        primary = Color(0xFF006C67),
        secondary = Color(0xFF2DD4BF),
        tertiary = Color(0xFFB9F2EC),
    ),
    ThemePalette.Harbor to palette(
        primary = Color(0xFF005B8C),
        secondary = Color(0xFF60A5FA),
        tertiary = Color(0xFFBAE6FD),
    ),
    ThemePalette.Grove to palette(
        primary = Color(0xFF237046),
        secondary = Color(0xFF4ADE80),
        tertiary = Color(0xFFBBF7D0),
    ),
    ThemePalette.Ember to palette(
        primary = Color(0xFFAD4543),
        secondary = Color(0xFFFB7185),
        tertiary = Color(0xFFFECDD3),
    ),
    ThemePalette.Twilight to palette(
        primary = Color(0xFF5C5BB0),
        secondary = Color(0xFFA78BFA),
        tertiary = Color(0xFFDDD6FE),
    ),
    // Claude's original five.
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
