/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.component

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp

private val MIN_SEGMENT_HEIGHT = 48.dp
private val TAB_PADDING_HORIZONTAL = 16.dp
private val TAB_PADDING_VERTICAL = 12.dp

/** Horizontal space a segment needs besides its label: content padding, the selection check mark and the border. */
private val SEGMENT_CHROME = 56.dp

/**
 * A single-choice row of [tabs] (e.g. the day-details tabs, T-802), each announced with its selection state. The tabs
 * are equal-width segments when every label fits on one line; otherwise (large font scales, long translations) they
 * become a horizontally scrollable tab row, so no label is ever cut off (T-1701). Segments are at least 48 dp tall:
 * adjacent 40 dp segments would share their widened touch areas (T-1700).
 */
@Composable
public fun SegmentedTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val measurer = rememberTextMeasurer()
        val style = MaterialTheme.typography.labelLarge
        val chrome = with(LocalDensity.current) { SEGMENT_CHROME.roundToPx() }
        val widest = tabs.maxOfOrNull { measurer.measure(it, style, maxLines = 1).size.width } ?: 0
        if ((widest + chrome) * tabs.size <= constraints.maxWidth) {
            Segments(tabs, selectedIndex, onSelect)
        } else {
            ScrollingTabs(tabs, selectedIndex, onSelect)
        }
    }
}

@Composable
private fun Segments(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        tabs.forEachIndexed { index, label ->
            SegmentedButton(
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                modifier = Modifier.heightIn(min = MIN_SEGMENT_HEIGHT),
                shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                label = { Text(label, maxLines = 1) },
            )
        }
    }
}

@Composable
private fun ScrollingTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    SecondaryScrollableTabRow(selectedTabIndex = selectedIndex, edgePadding = 0.dp) {
        tabs.forEachIndexed { index, label ->
            // The content overload grows with the label; the `text` slot has a fixed height that clips large text.
            Tab(
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                modifier = Modifier.heightIn(min = MIN_SEGMENT_HEIGHT),
            ) {
                Text(
                    label,
                    modifier = Modifier.padding(horizontal = TAB_PADDING_HORIZONTAL, vertical = TAB_PADDING_VERTICAL),
                    maxLines = 1,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
