package com.zoneanchor.app.ui

import android.view.SoundEffectConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
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

/** Long-press only (no click handler). Use when the row has no tap action but should expose actions on hold. */
fun Modifier.longPressable(onLongPress: () -> Unit): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    this.pointerInput(Unit) {
        detectTapGestures(onLongPress = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onLongPress()
        })
    }
}

/** Variant that also supports long-press. Long-press fires a heavier haptic. */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.tappable(
    enabled: Boolean = true,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
): Modifier = composed {
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val wrappedClick: () -> Unit = {
        view.playSoundEffect(SoundEffectConstants.CLICK)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onClick()
    }
    val wrappedLongClick: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onLongClick()
    }
    this.combinedClickable(
        enabled = enabled,
        onClick = wrappedClick,
        onLongClick = wrappedLongClick,
    )
}
