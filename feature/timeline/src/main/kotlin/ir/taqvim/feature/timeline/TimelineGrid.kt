/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.timeline

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first

/** Height of one hour at zoom 1. */
internal val HOUR_HEIGHT = 48.dp

private val GUTTER_WIDTH = 52.dp
private val ALL_DAY_MAX_HEIGHT = 96.dp
private val ALL_DAY_EVENT_HEIGHT = 20.dp
private val HEADER_PADDING = 4.dp
private val CELL_PADDING = 1.dp

/** The first visible hour starts this long before the current time. */
private const val SCROLL_LEAD_MINUTES = 60

/** Where the grid scrolls when today is not shown. */
private const val DEFAULT_SCROLL_MINUTE = 8 * 60

/** Test tag of the scrolling time grid. */
internal const val GRID_TAG = "timeline_grid"

/** The day headers, all-day row and the scrolling time grid with hour gutter and day columns. */
@Composable
internal fun TimelineGrid(
    content: TimelineContent,
    labels: TimelineLabels,
    onAction: (TimelineAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val hourHeight = HOUR_HEIGHT * content.zoom
    val zoomActions = zoomActions(onAction)
    val currentOnAction by rememberUpdatedState(onAction)
    Column(modifier) {
        DayHeaders(content, labels)
        if (content.columns.any { it.allDay.isNotEmpty() }) AllDayRow(content.columns, labels, onAction)
        Row(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag(GRID_TAG)
                .pinchToZoom { currentOnAction(TimelineAction.ZoomBy(it)) }
                .semantics { customActions = zoomActions }
                .verticalScroll(scrollState),
        ) {
            HourGutter(hourHeight, labels)
            content.columns.forEach { column ->
                key(column.jdn.value) {
                    DayTimeColumn(column, content, hourHeight, labels, onAction, Modifier.weight(1f))
                }
            }
        }
    }
    AutoScroll(scrollState, content, hourHeight)
}

/** Scrolls to an hour before now (or to the morning) whenever other days are shown. */
@Composable
private fun AutoScroll(
    scrollState: ScrollState,
    content: TimelineContent,
    hourHeight: Dp,
) {
    val density = LocalDensity.current
    val showsToday = content.columns.any { it.jdn == content.now.day }
    val target = (if (showsToday) content.now.minute else DEFAULT_SCROLL_MINUTE) - SCROLL_LEAD_MINUTES
    val offset = with(density) { TimelineGeometry.offsetOf(target.coerceAtLeast(0), hourHeight.toPx()).roundToInt() }
    LaunchedEffect(content.columns.first().jdn, content.mode) {
        snapshotFlow { scrollState.maxValue }.first { it in 1 until Int.MAX_VALUE }
        scrollState.scrollTo(offset)
    }
}

@Composable
private fun DayHeaders(
    content: TimelineContent,
    labels: TimelineLabels,
) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(vertical = HEADER_PADDING)) {
        Spacer(Modifier.width(GUTTER_WIDTH))
        content.columns.forEach { column ->
            val isToday = column.jdn == content.now.day
            val color =
                when {
                    column.isHoliday -> colors.error
                    isToday -> colors.primary
                    column.isWeekend -> colors.onSurfaceVariant
                    else -> colors.onSurface
                }
            val description = labels.dayDescription(column.jdn)
            // Weekday and day number on their own lines, each shrinking to the column width instead of breaking a word
            // or being cut off in a narrow week column at large font scales (T-1701).
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = HEADER_LINE_GAP)
                    .semantics(mergeDescendants = true) { contentDescription = description },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                HeaderLine(labels.weekdayName(column.jdn), color, isToday)
                HeaderLine(labels.dayNumber(column.jdn), color, isToday)
            }
        }
    }
}

@Composable
private fun AllDayRow(
    columns: List<TimelineColumn>,
    labels: TimelineLabels,
    onAction: (TimelineAction) -> Unit,
) {
    Row(Modifier.fillMaxWidth().heightIn(max = ALL_DAY_MAX_HEIGHT)) {
        Text(
            stringResource(R.string.timeline_all_day),
            modifier = Modifier.width(GUTTER_WIDTH).padding(horizontal = HEADER_PADDING),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            autoSize = fitting(MaterialTheme.typography.labelSmall),
        )
        columns.forEach { column ->
            Column(Modifier.weight(1f).padding(CELL_PADDING)) {
                column.allDay.forEach { event ->
                    EventBlock(event, labels, onAction, Modifier.fillMaxWidth().height(ALL_DAY_EVENT_HEIGHT))
                }
            }
        }
    }
}

@Composable
private fun HourGutter(
    hourHeight: Dp,
    labels: TimelineLabels,
) {
    Column(Modifier.width(GUTTER_WIDTH)) {
        repeat(TimelineGeometry.HOURS_PER_DAY) { hour ->
            Text(
                labels.hour(hour),
                modifier = Modifier.height(hourHeight).padding(horizontal = HEADER_PADDING),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                autoSize = fitting(MaterialTheme.typography.labelSmall),
            )
        }
    }
}

/** Zoom in and out as accessibility actions of the grid. */
@Composable
private fun zoomActions(onAction: (TimelineAction) -> Unit): List<CustomAccessibilityAction> {
    val zoomIn = stringResource(R.string.timeline_zoom_in)
    val zoomOut = stringResource(R.string.timeline_zoom_out)
    val currentOnAction by rememberUpdatedState(onAction)
    return remember(zoomIn, zoomOut) {
        listOf(
            CustomAccessibilityAction(zoomIn) {
                currentOnAction(TimelineAction.ZoomBy(TimelineGeometry.ZOOM_STEP))
                true
            },
            CustomAccessibilityAction(zoomOut) {
                currentOnAction(TimelineAction.ZoomBy(1 / TimelineGeometry.ZOOM_STEP))
                true
            },
        )
    }
}

/** Reports the zoom factor of two-finger pinches; one-finger drags are left to scrolling and drawing. */
private fun Modifier.pinchToZoom(onZoom: (Float) -> Unit): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            do {
                val event = awaitPointerEvent()
                if (event.changes.count { it.pressed } > 1) {
                    val zoom = event.calculateZoom()
                    if (zoom != 1f) {
                        onZoom(zoom)
                        event.changes.forEach { it.consume() }
                    }
                }
            } while (event.changes.any { it.pressed })
        }
    }

@Composable
private fun HeaderLine(
    text: String,
    color: Color,
    isToday: Boolean,
) {
    val style = MaterialTheme.typography.labelMedium
    Text(
        text,
        color = color,
        style = style,
        fontWeight = if (isToday) FontWeight.Bold else null,
        textAlign = TextAlign.Center,
        maxLines = 1,
        autoSize = fitting(style),
    )
}

/**
 * Header, gutter and all-day labels have fixed space: they shrink from [style]'s size to fit instead of being cut off
 * at large font scales (T-1701).
 */
private fun fitting(style: TextStyle): TextAutoSize =
    TextAutoSize.StepBased(minFontSize = MIN_LABEL_SIZE, maxFontSize = style.fontSize)

private val MIN_LABEL_SIZE = 6.sp

/** Space between neighbouring day headers, so shrunk weekday names do not run together. */
private val HEADER_LINE_GAP = 2.dp
