/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.component

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

/**
 * Taps, long presses, keys and their accessibility actions of a day cell, at a fraction of `combinedClickable`'s cost
 * (BUG-2).
 *
 * `combinedClickable` builds an interaction source, a ripple, hover and focus handling and a long-press detector for
 * every one of a page's 42 cells when the page is composed; on a page entering the month pager that was about a
 * third of the frame. Here a gesture detector that starts only on the first touch replaces it; the press is shown as a
 * Material pressed state layer ([pressedTint]) drawn from a state read in the draw phase, so a press redraws the cell
 * without recomposing it; the cell stays focusable and Enter or the D-pad centre clicks it; and the click and the
 * labelled long-press are published as semantics actions, which is what TalkBack and switch access use (T-1700).
 */
@Composable
internal fun Modifier.dayCellInput(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    longClickLabel: String?,
    pressedTint: Color,
): Modifier {
    val latestClick by rememberUpdatedState(onClick)
    val latestLongClick by rememberUpdatedState(onLongClick)
    val press = remember { PressState() }
    val hasLongClick = onLongClick != null
    return this
        .semantics {
            role = Role.Button
            onClick {
                latestClick()
                true
            }
            if (hasLongClick) {
                onLongClick(label = longClickLabel) {
                    latestLongClick?.invoke()
                    true
                }
            }
        }.pointerInput(hasLongClick) {
            detectTapGestures(
                onPress = {
                    press.pressed = true
                    tryAwaitRelease()
                    press.pressed = false
                },
                onLongPress = if (hasLongClick) ({ latestLongClick?.invoke() }) else null,
                onTap = { latestClick() },
            )
        }.onKeyEvent { event ->
            val activates = event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter
            (activates && event.type == KeyEventType.KeyUp).also { if (it) latestClick() }
        }.focusable()
        .drawWithContent {
            drawContent()
            if (press.pressed) drawRect(pressedTint)
        }
}

/** Whether a finger is down on a cell; read only while drawing, so a press redraws the cell and nothing else. */
private class PressState {
    var pressed by mutableStateOf(false)
}
