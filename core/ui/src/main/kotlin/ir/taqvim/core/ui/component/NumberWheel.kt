/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val MIN_ITEM_HEIGHT = 48.dp
private val ITEM_PADDING = 16.dp
private const val VISIBLE_ITEMS = 3
private val BAND_SHAPE = RoundedCornerShape(12.dp)

/**
 * A vertical, snapping wheel choosing [value] from [range] (T-701), e.g. the year, month and day of [DatePickerSheet].
 * Items are shown with [format] (localized digits or month names). Accessibility services see one adjustable control
 * named [label] whose state is the formatted value; they change it through the progress action.
 */
@Composable
public fun NumberWheel(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    format: (Int) -> String = Int::toString,
) {
    val itemHeight = wheelItemHeight()
    val state = rememberLazyListState(initialFirstVisibleItemIndex = WheelMath.indexOf(value, range))
    val centered = rememberCenteredValue(state, range, itemHeight)
    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val scrolling = state.isScrollInProgress
    LaunchedEffect(value, range) {
        if (centered.value != WheelMath.valueAt(WheelMath.indexOf(value, range), range)) {
            state.scrollToItem(WheelMath.indexOf(value, range))
        }
    }
    LaunchedEffect(centered.value, scrolling) {
        if (!scrolling && centered.value != currentValue) currentOnValueChange(centered.value)
    }
    Box(
        modifier
            .height(itemHeight * VISIBLE_ITEMS)
            .clearAndSetSemantics {
                contentDescription = label
                stateDescription = format(value)
                progressBarRangeInfo =
                    ProgressBarRangeInfo(value.toFloat(), range.first.toFloat()..range.last.toFloat())
                setProgress { target ->
                    currentOnValueChange(WheelMath.valueForProgress(target, range))
                    true
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        WheelList(state, range, centered.value, itemHeight, format)
    }
}

/** Item height: one title line plus padding, never below the 48 dp touch target. */
@Composable
private fun wheelItemHeight(): Dp {
    val lineHeight = MaterialTheme.typography.titleLarge.lineHeight
    return with(LocalDensity.current) {
        if (lineHeight.isSp) maxOf(MIN_ITEM_HEIGHT, lineHeight.toDp() + ITEM_PADDING) else MIN_ITEM_HEIGHT
    }
}

/** The value of [range] currently in the selection band of [state]. */
@Composable
private fun rememberCenteredValue(
    state: LazyListState,
    range: IntRange,
    itemHeight: Dp,
): State<Int> {
    val itemPx = with(LocalDensity.current) { itemHeight.roundToPx() }
    val count = range.last - range.first + 1
    return remember(state, range, itemPx) {
        derivedStateOf {
            val index =
                WheelMath.centeredIndex(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset, itemPx, count)
            WheelMath.valueAt(index, range)
        }
    }
}

@Composable
private fun WheelList(
    state: LazyListState,
    range: IntRange,
    centered: Int,
    itemHeight: Dp,
    format: (Int) -> String,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(itemHeight)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, BAND_SHAPE),
    )
    LazyColumn(
        state = state,
        flingBehavior = rememberSnapFlingBehavior(state),
        contentPadding = PaddingValues(vertical = itemHeight),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(range.last - range.first + 1) { index ->
            val itemValue = range.first + index
            WheelItem(format(itemValue), selected = itemValue == centered, height = itemHeight)
        }
    }
}

@Composable
private fun WheelItem(
    text: String,
    selected: Boolean,
    height: Dp,
) {
    Box(Modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = if (selected) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
