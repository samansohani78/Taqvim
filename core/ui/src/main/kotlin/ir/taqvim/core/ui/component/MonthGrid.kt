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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
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

/** Subcomposition slots of [MonthGridLayout]: the weekday header row, then the cells it sizes. */
private enum class GridSlot { HEADER, BODY }

/**
 * The grid layout (T-701): [header] holds [columns] children, [body] holds per row an optional week-number child and
 * [columns] cells. Every child is measured exactly once per pass with fixed constraints; placement mirrors in RTL.
 *
 * The body is subcomposed after the header, because the cells' text fit depends on the cell size, which is only known
 * once the header has been measured: [textFit] is asked for it and its answer reaches the cells through
 * [LocalDayCellTextFit] (BUG-2).
 */
@Composable
internal fun MonthGridLayout(
    columns: Int,
    rows: Int,
    hasWeekColumn: Boolean,
    textFit: (cellWidth: Int, cellHeight: Int) -> DayCellTextFit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
    body: @Composable () -> Unit,
) {
    SubcomposeLayout(modifier) { constraints ->
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else (MIN_CELL_HEIGHT * columns).roundToPx()
        val leading = if (hasWeekColumn) minOf(WEEK_COLUMN_WIDTH.roundToPx(), width / (columns + 1)) else 0
        val edges = MonthGridGeometry.columnEdges(width, leading, columns)
        val headers =
            subcompose(GridSlot.HEADER, header).mapIndexed { i, measurable ->
                measurable.measure(Constraints.fixedWidth(edges[i + 1] - edges[i]))
            }
        val headerHeight = headers.maxOfOrNull { it.height } ?: 0
        val bounded = if (constraints.hasBoundedHeight) constraints.maxHeight else null
        val cellWidth = edges[1] - edges[0]
        val rowHeight =
            MonthGridGeometry.rowHeight(bounded, headerHeight, rows, cellWidth, MIN_CELL_HEIGHT.roundToPx())
        val perRow = columns + if (hasWeekColumn) 1 else 0
        val fit = textFit(cellWidth, rowHeight)
        val cells = subcompose(GridSlot.BODY) { CompositionLocalProvider(LocalDayCellTextFit provides fit, body) }
        val placeables =
            cells.mapIndexed { i, child ->
                child.measure(
                    Constraints.fixed(MonthGridGeometry.bodySlot(i, perRow, hasWeekColumn, edges).second, rowHeight),
                )
            }
        val height = (headerHeight + rowHeight * rows).coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) {
            headers.forEachIndexed { i, placeable -> placeable.placeRelative(edges[i], 0) }
            placeables.forEachIndexed { i, placeable ->
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
    val probe = rememberCellTextFitProbe(model)
    MonthGridLayout(
        columns = columns,
        rows = model.rows,
        hasWeekColumn = model.weekNumbers != null,
        textFit = probe,
        modifier = modifier,
        header = { model.weekdayLabels.forEach { WeekdayHeader(it) } },
    ) {
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

/**
 * Labels of one line kind are measured at most this many times. A month has at most 31 day numbers and two secondary
 * dates per cell; a model with more distinct labels than this keeps shrinking rather than pay for the probe.
 */
private const val MAX_PROBED_LABELS = 64

/** Layout results the probe keeps, so the labels of the months already seen are measured once, not once per page. */
private const val LABEL_CACHE_SIZE = 256

/**
 * Decides once per cell size whether the cells of [model] can draw their lines at full size, by measuring the
 * distinct label of each line kind (BUG-2). Returning [DayCellTextFit.SHRINK_TO_FIT] is always safe; full size is
 * reported only when every label sits well inside its slot ([DayCellFit]).
 */
@Composable
internal fun rememberCellTextFitProbe(model: MonthGridModel): (Int, Int) -> DayCellTextFit {
    val measurer = rememberTextMeasurer(cacheSize = LABEL_CACHE_SIZE)
    val dayStyle = MaterialTheme.typography.titleMedium
    val smallStyle = MaterialTheme.typography.labelSmall
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val cellDensity =
        remember(density) { Density(density.density, density.fontScale.coerceAtMost(MAX_CELL_FONT_SCALE)) }
    return remember(model, measurer, dayStyle, smallStyle, cellDensity, direction) {
        { cellWidth: Int, cellHeight: Int ->
            val days = model.cells.map { it.dayLabel }.distinct()
            val small =
                (model.cells.flatMap { it.secondaryLabels } + model.cells.mapNotNull { it.shiftLabel })
                    .distinct()
            if (days.size + small.size > MAX_PROBED_LABELS) {
                DayCellTextFit.SHRINK_TO_FIT
            } else {
                val widest = model.cells.maxOf { it.secondaryLabels.size + if (it.shiftLabel == null) 0 else 1 }
                val lines =
                    buildList {
                        add(measurer.line(days, dayStyle, cellDensity, direction, DAY_WEIGHT))
                        repeat(widest) { add(measurer.line(small, smallStyle, cellDensity, direction, LABEL_WEIGHT)) }
                    }
                val dots =
                    if (model.cells.any { it.indicators.isNotEmpty() }) {
                        with(
                            cellDensity,
                        ) { DOTS_HEIGHT.roundToPx() }
                    } else {
                        0
                    }
                val padding = with(cellDensity) { CELL_PADDING.roundToPx() }
                if (DayCellFit.fitsAtFullSize(cellWidth, cellHeight, padding, dots, lines)) {
                    DayCellTextFit.FULL_SIZE
                } else {
                    DayCellTextFit.SHRINK_TO_FIT
                }
            }
        }
    }
}

/** The widest and tallest of [labels] at [style]'s own size, as one line of the cell column. */
private fun TextMeasurer.line(
    labels: List<String>,
    style: TextStyle,
    density: Density,
    direction: LayoutDirection,
    weight: Float,
): CellLine {
    val measured =
        labels.map { label ->
            measure(
                text = label,
                style = style.copy(lineHeight = TextUnit.Unspecified),
                maxLines = 1,
                layoutDirection = direction,
                density = density,
            ).size
        }
    return CellLine(weight, measured.maxOfOrNull { it.width } ?: 0, measured.maxOfOrNull { it.height } ?: 0)
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
