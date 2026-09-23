/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp

/** Test tag of [MonthDayList], so a test can tell which presentation of a [MonthGridModel] is on screen. */
public const val MONTH_DAY_LIST_TAG: String = "month_day_list"

private val ROW_SHAPE = RoundedCornerShape(8.dp)
private val ROW_MIN_HEIGHT = 48.dp
private val ROW_HORIZONTAL_PADDING = 16.dp
private val ROW_VERTICAL_PADDING = 8.dp
private val ROW_CONTENT_GAP = 12.dp
private val WEEK_ROW_VERTICAL_PADDING = 4.dp

/**
 * A month of [DayCellModel]s as a full-width list, one row per day (R10, T-1701): the alternative to [MonthGrid]
 * above [MonthDisplayMode.forFontScale]'s cap, where a fixed-size grid cell cannot fit the requested font scale.
 * Every row draws at the caller's real density — nothing here caps the font scale — so a long label wraps to a
 * second line instead of clipping (T-1701's `LayoutAudit`); rows grow to fit their content, so nothing is cut off by
 * the window's height either.
 *
 * Scrolls itself in a `LazyColumn` when [modifier] gives it a bounded height (a pane of its own, as in the two-pane
 * and tabletop layouts); with an unbounded height — nested in an ancestor that already scrolls, as the stacked phone
 * layout wraps the whole month-and-details column (`CalendarPanes`) — a `LazyColumn` cannot measure itself (Compose
 * disallows a scrollable given infinite constraints), so a plain `Column` is used instead and the ancestor scrolls
 * it. A month page is at most a few dozen rows, cheap either way (unlike [MonthGrid]'s BUG-2 grid, not ported here).
 *
 * Takes the same [model] and callbacks as [MonthGrid]: [onDayClick] and [onDayLongClick] receive the cell index in
 * [MonthGridModel.cells], and [onWeekClick] the row of a tapped week number, shown as a header above its seven days
 * so the grid's week-to-timeline shortcut stays reachable at every font scale.
 */
@Composable
public fun MonthDayList(
    model: MonthGridModel,
    onDayClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    onDayLongClick: ((index: Int) -> Unit)? = null,
    onWeekClick: ((row: Int) -> Unit)? = null,
) {
    val columns = model.weekdayLabels.size
    val colors = MaterialTheme.colorScheme
    // The test tag stays on the actual scrolling node (or the plain Column), not on this probe box, so a test's
    // `performScrollToIndex` finds the node that actually carries `ScrollToIndex` semantics.
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (constraints.hasBoundedHeight) {
            LazyColumn(Modifier.fillMaxWidth().testTag(MONTH_DAY_LIST_TAG)) {
                monthDayListItems(model, columns, colors, onDayClick, onDayLongClick, onWeekClick)
            }
        } else {
            Column(Modifier.fillMaxWidth().testTag(MONTH_DAY_LIST_TAG)) {
                MonthDayListRows(model, columns, colors, onDayClick, onDayLongClick, onWeekClick)
            }
        }
    }
}

/** [MonthDayList]'s rows as `LazyColumn` items, for a bounded height. */
private fun LazyListScope.monthDayListItems(
    model: MonthGridModel,
    columns: Int,
    colors: ColorScheme,
    onDayClick: (index: Int) -> Unit,
    onDayLongClick: ((index: Int) -> Unit)?,
    onWeekClick: ((row: Int) -> Unit)?,
) {
    model.cells.chunked(columns).forEachIndexed { row, week ->
        model.weekNumbers?.let { numbers ->
            item(key = "week_$row") {
                WeekNumberRow(numbers[row], onWeekClick?.let { click -> { click(row) } })
            }
        }
        itemsIndexed(week, key = { column, _ -> row * columns + column }) { column, cell ->
            val index = row * columns + column
            DayListRow(
                cell,
                colors = colors,
                onClick = { onDayClick(index) },
                onLongClick = onDayLongClick?.let { click -> { click(index) } },
                longClickLabel = model.longClickLabel,
            )
        }
    }
}

/** [MonthDayList]'s rows in a plain, non-lazy `Column`, for an unbounded height (an ancestor already scrolls). */
@Composable
private fun MonthDayListRows(
    model: MonthGridModel,
    columns: Int,
    colors: ColorScheme,
    onDayClick: (index: Int) -> Unit,
    onDayLongClick: ((index: Int) -> Unit)?,
    onWeekClick: ((row: Int) -> Unit)?,
) {
    model.cells.chunked(columns).forEachIndexed { row, week ->
        model.weekNumbers?.let { numbers ->
            key("week_$row") {
                WeekNumberRow(numbers[row], onWeekClick?.let { click -> { click(row) } })
            }
        }
        week.forEachIndexed { column, cell ->
            val index = row * columns + column
            key(index) {
                DayListRow(
                    cell,
                    colors = colors,
                    onClick = { onDayClick(index) },
                    onLongClick = onDayLongClick?.let { click -> { click(index) } },
                    longClickLabel = model.longClickLabel,
                )
            }
        }
    }
}

/**
 * One day of [MonthDayList]: [model]'s day number and secondary dates from [cellTexts] (same order and colors as
 * [DayCell]), its event dots, and the today ring / selected fill [DayCell] also draws. At least [ROW_MIN_HEIGHT] so
 * the row is a full accessible touch target even when its text is short.
 */
@Composable
private fun DayListRow(
    model: DayCellModel,
    colors: ColorScheme,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    longClickLabel: String?,
) {
    val selectedFill = if (model.isSelected) Modifier.background(colors.primaryContainer) else Modifier
    val todayRing = if (model.isToday) Modifier.border(TODAY_BORDER, colors.primary, ROW_SHAPE) else Modifier
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(ROW_SHAPE)
                .dayCellInput(onClick, onLongClick, longClickLabel, colors.onSurface.copy(alpha = PRESSED_ALPHA))
                .clearAndSetSemantics {
                    contentDescription = model.contentDescription
                    selected = model.isSelected
                }.then(selectedFill)
                .then(todayRing)
                // heightIn must be the innermost modifier (right before this Row's own content) so its minimum
                // applies to the whole padded row, not just the content padding is reducing space for.
                .padding(horizontal = ROW_HORIZONTAL_PADDING, vertical = ROW_VERTICAL_PADDING)
                .heightIn(min = ROW_MIN_HEIGHT),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ROW_CONTENT_GAP),
    ) {
        val dayStyle = MaterialTheme.typography.titleMedium
        val smallStyle = MaterialTheme.typography.labelSmall
        Column(Modifier.weight(1f)) {
            cellTexts(model, dayStyle, smallStyle, colors).forEach { line ->
                Text(line.text, style = line.style, color = line.color)
            }
        }
        if (model.indicators.isNotEmpty()) IndicatorDots(model.indicators)
    }
}

/**
 * The week-number header row of [MonthDayList], shown above the seven days of its week: [model]'s spoken form
 * matches [MonthGrid]'s week-number column, and [onClick] (when given) opens the timeline exactly as tapping that
 * column does, so the shortcut is not lost when the grid switches to a list (T-1701).
 */
@Composable
private fun WeekNumberRow(
    model: WeekNumberModel,
    onClick: (() -> Unit)?,
) {
    val clickable = if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)
    val minHeight = if (onClick == null) 0.dp else ROW_MIN_HEIGHT
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(clickable)
                .clearAndSetSemantics { contentDescription = model.contentDescription }
                .padding(horizontal = ROW_HORIZONTAL_PADDING, vertical = WEEK_ROW_VERTICAL_PADDING)
                .heightIn(min = minHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(model.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
    }
}
