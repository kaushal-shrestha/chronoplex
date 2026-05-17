package com.chronoplex.app.ui

import android.view.SoundEffectConstants
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView

@Composable
fun rememberTapFeedback(action: () -> Unit): () -> Unit {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    return {
        view.playSoundEffect(SoundEffectConstants.CLICK)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        action()
    }
}

/** Toggle variant — for [androidx.compose.material3.Switch.onCheckedChange] etc. */
@Composable
fun rememberToggleFeedback(action: (Boolean) -> Unit): (Boolean) -> Unit {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    return { value ->
        view.playSoundEffect(SoundEffectConstants.CLICK)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        action(value)
    }
}

/** Drop-in replacement for [Modifier.clickable] that also fires system tap sound + haptic. */
fun Modifier.tappable(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val wrapped = rememberTapFeedback(onClick)
    this.clickable(enabled = enabled, onClick = wrapped)
}
