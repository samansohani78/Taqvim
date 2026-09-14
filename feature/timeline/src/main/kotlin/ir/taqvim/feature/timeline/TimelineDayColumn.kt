/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ir.taqvim.core.model.Jdn
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableList

private val MIN_EVENT_HEIGHT = 18.dp
private val EVENT_GAP = 1.dp
private val EVENT_PADDING = 2.dp
private val RESIZE_HANDLE_HEIGHT = 12.dp
private val CORNER = RoundedCornerShape(4.dp)
private val LINE_WIDTH = 1.dp
private val NOW_LINE_WIDTH = 2.dp
private val DRAFT_BORDER = 2.dp
private const val DRAFT_ALPHA = 0.16f
private const val DASH_ON = 6f
private const val DASH_OFF = 4f

/** Test tag of the day column of [day]. */
internal fun dayTag(day: Jdn): String = "timeline_day_${day.value}"

/** Test tag of the box of the new event being drawn and of its resize handle. */
internal const val DRAFT_TAG = "timeline_draft"
internal const val RESIZE_TAG = "timeline_draft_resize"

/**
 * One day of the grid: hour and prayer lines, timed events placed by [IntervalColoring], the now line on today, a long
 * press and drag to draw a new event, and the box of that event.
 */
@Composable
internal fun DayTimeColumn(
    column: TimelineColumn,
    content: TimelineContent,
    hourHeight: Dp,
    labels: TimelineLabels,
    onAction: (TimelineAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val hourPx = with(LocalDensity.current) { hourHeight.toPx() }
    val nowMinute = content.now.minute.takeIf { content.now.day == column.jdn }
    val currentOnAction by rememberUpdatedState(onAction)
    val description = labels.column(column, content.now)
    Box(
        modifier
            .height(hourHeight * TimelineGeometry.HOURS_PER_DAY)
            .testTag(dayTag(column.jdn))
            .semantics { contentDescription = description }
            .drawWithContent {
                drawHourLines(hourPx, colors.outlineVariant)
                column.prayerLines.forEach { drawDashedLine(it.minute, hourPx, colors.tertiary) }
                drawContent()
                nowMinute?.let { drawTimeLine(it, hourPx, colors.error, NOW_LINE_WIDTH.toPx()) }
            }.draftGesture(column.jdn, hourPx) { currentOnAction(it) },
    ) {
        TimedEventsLayout(column.timed, hourHeight) { placed -> EventBlock(placed.event, labels, onAction) }
        content.draft?.takeIf { it.day == column.jdn }?.let { DraftBox(it, hourHeight, labels, onAction) }
    }
}

/** One event: title on its source color, opened on click, described with its times. */
@Composable
internal fun EventBlock(
    event: TimelineEvent,
    labels: TimelineLabels,
    onAction: (TimelineAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val (background, foreground) =
        when {
            event.isHoliday -> colors.errorContainer to colors.onErrorContainer
            event.kind == TimelineEventKind.OFFICIAL -> colors.tertiaryContainer to colors.onTertiaryContainer
            event.kind == TimelineEventKind.PERSONAL -> colors.primaryContainer to colors.onPrimaryContainer
            else -> colors.secondaryContainer to colors.onSecondaryContainer
        }
    val description = labels.event(event)
    Box(
        modifier
            .clip(CORNER)
            .background(background)
            .clickable { onAction(TimelineAction.OpenEvent(event.id, event.kind)) }
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(horizontal = EVENT_PADDING * 2, vertical = EVENT_PADDING),
    ) {
        Text(
            event.title,
            color = foreground,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Places each timed event in its column; every child is measured once with fixed constraints. */
@Composable
private fun TimedEventsLayout(
    events: ImmutableList<PlacedEvent>,
    hourHeight: Dp,
    item: @Composable (PlacedEvent) -> Unit,
) {
    Layout(content = { events.forEach { item(it) } }, modifier = Modifier.fillMaxSize()) { measurables, constraints ->
        val width = constraints.maxWidth
        val hourPx = hourHeight.toPx()
        val gap = EVENT_GAP.roundToPx()
        val tops = events.map { TimelineGeometry.offsetOf(it.event.startMinute, hourPx).roundToInt() }
        val placeables =
            measurables.mapIndexed { index, measurable ->
                val placed = events[index]
                val bottom = TimelineGeometry.offsetOf(placed.event.endMinute, hourPx).roundToInt()
                val height = (bottom - tops[index] - gap).coerceAtLeast(MIN_EVENT_HEIGHT.roundToPx())
                measurable.measure(Constraints.fixed((width / placed.columns - gap).coerceAtLeast(0), height))
            }
        layout(width, constraints.maxHeight) {
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(width * events[index].column / events[index].columns, tops[index])
            }
        }
    }
}

/** The new event: dragged to move, its bottom handle dragged to resize; keys and accessibility actions do both. */
@Composable
private fun DraftBox(
    draft: TimelineDraft,
    hourHeight: Dp,
    labels: TimelineLabels,
    onAction: (TimelineAction) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val hourPx = with(LocalDensity.current) { hourHeight.toPx() }
    val stepPx = TimelineGeometry.offsetOf(TimelineGeometry.SNAP_MINUTES, hourPx)
    val height = hourHeight * (draft.lengthMinutes.toFloat() / TimelineGeometry.MINUTES_PER_HOUR)
    val focusRequester = remember { FocusRequester() }
    val currentOnAction by rememberUpdatedState(onAction)
    val actions = draftActions(onAction)
    val description = labels.draft(draft)
    // The box moves under the finger while it is dragged, so drags are measured in root coordinates.
    val boxPosition = remember { LivePosition() }
    Box(
        Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, TimelineGeometry.offsetOf(draft.startMinute, hourPx).roundToInt()) }
            .height(height)
            .testTag(DRAFT_TAG)
            .onGloballyPositioned { boxPosition.coordinates = it }
            .background(colors.primary.copy(alpha = DRAFT_ALPHA), CORNER)
            .border(DRAFT_BORDER, colors.primary, CORNER)
            .stepDrag(stepPx, boxPosition::rootTop) { currentOnAction(TimelineAction.MoveDraft(it)) }
            .onKeyEvent { event -> draftKey(event)?.let { currentOnAction(it) } != null }
            .focusRequester(focusRequester)
            .focusable()
            .semantics {
                contentDescription = description
                customActions = actions
            },
    ) {
        val handleModifier = Modifier.align(Alignment.BottomCenter).height(minOf(RESIZE_HANDLE_HEIGHT, height / 2))
        ResizeHandle(stepPx, handleModifier) {
            currentOnAction(TimelineAction.ResizeDraft(it))
        }
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

/** The bottom edge of the new event; dragging it changes the end in [stepPx] steps. */
@Composable
private fun ResizeHandle(
    stepPx: Float,
    modifier: Modifier,
    onSteps: (Int) -> Unit,
) {
    val handlePosition = remember { LivePosition() }
    Box(
        modifier
            .fillMaxWidth()
            .testTag(RESIZE_TAG)
            .onGloballyPositioned { handlePosition.coordinates = it }
            .background(MaterialTheme.colorScheme.primary.copy(alpha = DRAFT_ALPHA * 2))
            .stepDrag(stepPx, handlePosition::rootTop, onSteps),
    )
}

/** Earlier, later, longer and shorter as accessibility actions of the new event. */
@Composable
private fun draftActions(onAction: (TimelineAction) -> Unit): List<CustomAccessibilityAction> {
    val earlier = stringResource(R.string.timeline_draft_earlier)
    val later = stringResource(R.string.timeline_draft_later)
    val longer = stringResource(R.string.timeline_draft_longer)
    val shorter = stringResource(R.string.timeline_draft_shorter)
    val currentOnAction by rememberUpdatedState(onAction)
    return remember(earlier, later, longer, shorter) {
        listOf(
            earlier to TimelineAction.MoveDraft(-1),
            later to TimelineAction.MoveDraft(1),
            longer to TimelineAction.ResizeDraft(1),
            shorter to TimelineAction.ResizeDraft(-1),
        ).map { (label, action) ->
            CustomAccessibilityAction(label) {
                currentOnAction(action)
                true
            }
        }
    }
}

/** Arrow keys move (with Shift: resize) by a step; Enter creates and Escape cancels. */
private fun draftKey(event: KeyEvent): TimelineAction? {
    if (event.type != KeyEventType.KeyDown) return null
    return when (event.key) {
        Key.DirectionUp -> if (event.isShiftPressed) TimelineAction.ResizeDraft(-1) else TimelineAction.MoveDraft(-1)
        Key.DirectionDown -> if (event.isShiftPressed) TimelineAction.ResizeDraft(1) else TimelineAction.MoveDraft(1)
        Key.Enter, Key.NumPadEnter -> TimelineAction.ConfirmDraft
        Key.Escape -> TimelineAction.CancelDraft
        else -> null
    }
}

/** A long press then drag on [day] draws the box of a new event. */
private fun Modifier.draftGesture(
    day: Jdn,
    hourPx: Float,
    onAction: (TimelineAction) -> Unit,
): Modifier =
    pointerInput(day, hourPx) {
        var anchor = 0
        detectDragGesturesAfterLongPress(
            onDragStart = { offset ->
                anchor = TimelineGeometry.minuteAt(offset.y, hourPx)
                onAction(TimelineAction.SetDraft(day, anchor, anchor))
            },
            onDrag = { change, _ ->
                change.consume()
                onAction(TimelineAction.SetDraft(day, anchor, TimelineGeometry.minuteAt(change.position.y, hourPx)))
            },
        )
    }

/**
 * The layout coordinates of an element, read when a pointer event arrives: its position then matches the pointer's
 * local position, which a position copied in a callback may not yet.
 */
private class LivePosition {
    var coordinates: LayoutCoordinates? = null

    fun rootTop(): Float = coordinates?.takeIf { it.isAttached }?.positionInRoot()?.y ?: 0f
}

/**
 * Vertical drags reported in whole steps of [stepPx] pixels. The element moves as the steps are applied, so the drag is
 * measured from where it started in root coordinates; [rootTop] is the element's current top in the root.
 */
private fun Modifier.stepDrag(
    stepPx: Float,
    rootTop: () -> Float,
    onSteps: (Int) -> Unit,
): Modifier =
    pointerInput(stepPx) {
        var startY = 0f
        var reported = 0
        detectVerticalDragGestures(
            onDragStart = { offset ->
                startY = offset.y + rootTop()
                reported = 0
            },
        ) { change, _ ->
            change.consume()
            val steps = ((change.position.y + rootTop() - startY) / stepPx).toInt()
            if (steps != reported) {
                onSteps(steps - reported)
                reported = steps
            }
        }
    }

private fun DrawScope.drawHourLines(
    hourPx: Float,
    color: Color,
) {
    for (hour in 1 until TimelineGeometry.HOURS_PER_DAY) {
        val y = hour * hourPx
        drawLine(color, Offset(0f, y), Offset(size.width, y), LINE_WIDTH.toPx())
    }
}

private fun DrawScope.drawDashedLine(
    minute: Int,
    hourPx: Float,
    color: Color,
) {
    val y = TimelineGeometry.offsetOf(minute, hourPx)
    val dashes = PathEffect.dashPathEffect(floatArrayOf(DASH_ON * density, DASH_OFF * density))
    drawLine(color, Offset(0f, y), Offset(size.width, y), LINE_WIDTH.toPx(), pathEffect = dashes)
}

private fun DrawScope.drawTimeLine(
    minute: Int,
    hourPx: Float,
    color: Color,
    width: Float,
) {
    val y = TimelineGeometry.offsetOf(minute, hourPx)
    drawLine(color, Offset(0f, y), Offset(size.width, y), width)
    // The dot marks the start of the day column: its right edge in right-to-left layouts.
    val start = if (layoutDirection == LayoutDirection.Rtl) size.width else 0f
    drawCircle(color, radius = width * 2, center = Offset(start, y))
}
