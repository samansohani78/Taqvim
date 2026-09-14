/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.taqvim.core.ui.component.ScreenSurface
import ir.taqvim.core.ui.component.SegmentedTabs
import ir.taqvim.core.ui.component.TopBar

private val CONTROLS_PADDING = 8.dp

/**
 * The week and day timeline (T-900), stateless: title, day/week tabs, day controls, the bar of a new event being drawn
 * and the time grid.
 */
@Composable
fun TimelineScreen(
    state: TimelineUiState,
    onAction: (TimelineAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = state.content
    if (content == null) {
        LoadingTimeline(modifier)
        return
    }
    val labels = rememberTimelineLabels(content)
    ScreenSurface(modifier = modifier, topBar = { TopBar(labels.title(content)) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SegmentedTabs(
                tabs = listOf(stringResource(R.string.timeline_mode_day), stringResource(R.string.timeline_mode_week)),
                selectedIndex = content.mode.ordinal,
                onSelect = { onAction(TimelineAction.ShowMode(TimelineMode.entries[it])) },
                modifier = Modifier.padding(horizontal = CONTROLS_PADDING),
            )
            // The bar of a new event takes the place of the day controls, so the grid does not move while drawing.
            val draft = content.draft
            if (draft == null) TimelineControls(onAction) else DraftBar(draft, labels, onAction)
            TimelineGrid(content, labels, onAction, Modifier.weight(1f).fillMaxWidth())
        }
    }
}

/** Previous, today and next. */
@Composable
private fun TimelineControls(onAction: (TimelineAction) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = CONTROLS_PADDING)) {
        ControlButton(stringResource(R.string.timeline_previous)) { onAction(TimelineAction.ShowPrevious) }
        ControlButton(stringResource(R.string.timeline_today)) { onAction(TimelineAction.GoToToday) }
        ControlButton(stringResource(R.string.timeline_next)) { onAction(TimelineAction.ShowNext) }
    }
}

/** The times of the new event being drawn with create and cancel. */
@Composable
private fun DraftBar(
    draft: TimelineDraft,
    labels: TimelineLabels,
    onAction: (TimelineAction) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = CONTROLS_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            labels.draft(draft),
            modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            autoSize = TextAutoSize.StepBased(minFontSize = MIN_CONTROL_TEXT_SIZE, maxFontSize = 14.sp),
        )
        TextButton(onClick = { onAction(TimelineAction.CancelDraft) }) {
            Text(stringResource(R.string.timeline_draft_cancel))
        }
        TextButton(onClick = { onAction(TimelineAction.ConfirmDraft) }) {
            Text(stringResource(R.string.timeline_draft_create))
        }
    }
}

@Composable
private fun RowScope.ControlButton(
    label: String,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick, modifier = Modifier.weight(1f)) {
        val maxSize = MaterialTheme.typography.labelLarge.fontSize
        Text(label, maxLines = 1, autoSize = TextAutoSize.StepBased(MIN_CONTROL_TEXT_SIZE, maxSize))
    }
}

/** Smallest size control labels shrink to at large font scales instead of being cut off (T-1701). */
private val MIN_CONTROL_TEXT_SIZE = 8.sp

@Composable
private fun LoadingTimeline(modifier: Modifier) {
    val description = stringResource(R.string.timeline_loading)
    ScreenSurface(modifier = modifier, topBar = { TopBar(stringResource(R.string.timeline_title)) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.semantics { contentDescription = description })
        }
    }
}
