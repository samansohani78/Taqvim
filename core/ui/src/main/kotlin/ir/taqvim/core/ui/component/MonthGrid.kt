/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

/** A week-number label of a [MonthGrid] row and its spoken form. */
@Immutable
public data class WeekNumberModel(
    public val label: String,
    public val contentDescription: String,
)

/**
 * A month page: [weekdayLabels] (one per column, in the week order of the locale), [cells] row by row (a multiple of
 * the column count, including leading/trailing days), optional [weekNumbers] (one per row) and the spoken name of the
 * day long-press action.
 */
@Immutable
public data class MonthGridModel(
    public val weekdayLabels: List<String>,
    public val cells: List<DayCellModel>,
    public val weekNumbers: List<WeekNumberModel>? = null,
    public val longClickLabel: String? = null,
) {
    init {
        require(weekdayLabels.isNotEmpty()) { "A month grid needs at least one column" }
        require(cells.isNotEmpty() && cells.size % weekdayLabels.size == 0) {
            "Cells must fill whole rows of ${weekdayLabels.size}"
        }
        require(weekNumbers == null || weekNumbers.size == rows) { "One week number per row is required" }
    }

    /** Number of week rows. */
    public val rows: Int
        get() = cells.size / weekdayLabels.size
}

/** Pure placement arithmetic of the month grid layout, in pixels. */
internal object MonthGridGeometry {
    /** Column edges: `count + 1` offsets filling [width] after a leading column of [leadingWidth], without gaps. */
    fun columnEdges(
        width: Int,
        leadingWidth: Int,
        count: Int,
    ): List<Int> {
        val available = (width - leadingWidth).coerceAtLeast(0).toLong()
        return (0..count).map { leadingWidth + (available * it / count).toInt() }
    }

    /**
     * Row height: rows share [availableHeight] below the header when the height is bounded; otherwise cells are square.
     * Never below [minHeight].
     */
    fun rowHeight(
        availableHeight: Int?,
        headerHeight: Int,
        rows: Int,
        cellWidth: Int,
        minHeight: Int,
    ): Int {
        val natural =
            if (availableHeight == null) cellWidth else (availableHeight - headerHeight).coerceAtLeast(0) / rows
        return maxOf(natural, minHeight)
    }

    /** Horizontal slot (x, width) of body child [index] in rows of [perRow] children, the first being a week number. */
    fun bodySlot(
        index: Int,
        perRow: Int,
        hasWeekColumn: Boolean,
        edges: List<Int>,
    ): Pair<Int, Int> {
        val column = index % perRow - if (hasWeekColumn) 1 else 0
        return if (column < 0) 0 to edges.first() else edges[column] to edges[column + 1] - edges[column]
    }
}

private val WEEK_COLUMN_WIDTH = 32.dp
private val MIN_CELL_HEIGHT = 40.dp

/**
 * The grid layout (T-701): [columns] header children, then per row an optional week-number child and [columns] cells.
 * Every child is measured exactly once per pass with fixed constraints; placement mirrors in RTL.
 */
@Composable
internal fun MonthGridLayout(
    columns: Int,
    rows: Int,
    hasWeekColumn: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content, modifier) { measurables, constraints ->
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else (MIN_CELL_HEIGHT * columns).roundToPx()
        val leading = if (hasWeekColumn) minOf(WEEK_COLUMN_WIDTH.roundToPx(), width / (columns + 1)) else 0
        val edges = MonthGridGeometry.columnEdges(width, leading, columns)
        val headers =
            measurables.take(columns).mapIndexed { i, header ->
                header.measure(Constraints.fixedWidth(edges[i + 1] - edges[i]))
            }
        val headerHeight = headers.maxOfOrNull { it.height } ?: 0
        val bounded = if (constraints.hasBoundedHeight) constraints.maxHeight else null
        val rowHeight =
            MonthGridGeometry.rowHeight(bounded, headerHeight, rows, edges[1] - edges[0], MIN_CELL_HEIGHT.roundToPx())
        val perRow = columns + if (hasWeekColumn) 1 else 0
        val body =
            measurables.drop(columns).mapIndexed { i, child ->
                child.measure(
                    Constraints.fixed(MonthGridGeometry.bodySlot(i, perRow, hasWeekColumn, edges).second, rowHeight),
                )
            }
        val height = (headerHeight + rowHeight * rows).coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) {
            headers.forEachIndexed { i, placeable -> placeable.placeRelative(edges[i], 0) }
            body.forEachIndexed { i, placeable ->
                val x = MonthGridGeometry.bodySlot(i, perRow, hasWeekColumn, edges).first
                placeable.placeRelative(x, headerHeight + (i / perRow) * rowHeight)
            }
        }
    }
}

/**
 * A month of [DayCell]s under weekday headers (T-701). [onDayClick] and [onDayLongClick] receive the cell index in
 * [MonthGridModel.cells]; [onWeekClick] receives the row of a tapped week number (e.g. to open the timeline).
 */
@Composable
public fun MonthGrid(
    model: MonthGridModel,
    onDayClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    onDayLongClick: ((index: Int) -> Unit)? = null,
    onWeekClick: ((row: Int) -> Unit)? = null,
) {
    val columns = model.weekdayLabels.size
    MonthGridLayout(columns, model.rows, model.weekNumbers != null, modifier) {
        model.weekdayLabels.forEach { WeekdayHeader(it) }
        model.cells.chunked(columns).forEachIndexed { row, week ->
            model.weekNumbers?.let { numbers -> WeekNumber(numbers[row], onWeekClick?.let { { it(row) } }) }
            week.forEachIndexed { column, cell ->
                val index = row * columns + column
                key(index) {
                    DayCell(
                        cell,
                        onClick = { onDayClick(index) },
                        onLongClick = onDayLongClick?.let { { it(index) } },
                        longClickLabel = model.longClickLabel,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekdayHeader(label: String) {
    Text(
        label,
        modifier = Modifier.padding(vertical = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

@Composable
private fun WeekNumber(
    model: WeekNumberModel,
    onClick: (() -> Unit)?,
) {
    val clickable = if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)
    Box(
        clickable.clearAndSetSemantics { contentDescription = model.contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Text(model.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}
