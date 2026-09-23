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
    val measurer = rememberTextMeasurer(cacheSize = LABEL_CACHE_SIZE)
    val probe = rememberCellTextFitProbe(model, measurer)
    MonthGridLayout(
        columns = columns,
        rows = model.rows,
        hasWeekColumn = model.weekNumbers != null,
        textFit = probe,
        modifier = modifier,
        header = { model.weekdayLabels.forEach { WeekdayHeader(it) } },
    ) {
        CompositionLocalProvider(LocalCellTextMeasurer provides measurer) {
            MonthGridBody(model, columns, onDayClick, onDayLongClick, onWeekClick)
        }
    }
}

/** The week numbers and day cells of [MonthGrid], row by row. */
@Composable
private fun MonthGridBody(
    model: MonthGridModel,
    columns: Int,
    onDayClick: (index: Int) -> Unit,
    onDayLongClick: ((index: Int) -> Unit)?,
    onWeekClick: ((row: Int) -> Unit)?,
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

/**
 * Distinct labels the probe measures at most. A calendar page shows up to 31 day numbers and two secondary dates per
 * cell, each running through a month of its own — about 31 + 30 + 31 distinct labels. The cap used to be 64, below
 * that, so every real page skipped the probe and all 42 cells searched their font size on every measure (BUG-2).
 * Labels are short numbers and the measurer caches them across pages, so a page usually measures only its new ones.
 */
private const val MAX_PROBED_LABELS = 160

/** Layout results the probe keeps, so the labels of the months already seen are measured once, not once per page. */
private const val LABEL_CACHE_SIZE = 512

/**
 * Decides once per cell size how the cells of [model] size their lines, by measuring the distinct labels of each line
 * kind (BUG-2): one scale for the day numbers and one for the small lines, confirmed by measuring the page's labels at
 * that scale. [DayCellTextFit.SHRINK_TO_FIT] — every cell searching its own size — is left for the grids whose labels
 * do not fit even at the smallest line size, or that have too many distinct labels to probe.
 */
@Composable
internal fun rememberCellTextFitProbe(
    model: MonthGridModel,
    measurer: TextMeasurer = rememberTextMeasurer(cacheSize = LABEL_CACHE_SIZE),
): (Int, Int) -> DayCellTextFit {
    val dayStyle = MaterialTheme.typography.titleMedium
    val smallStyle = MaterialTheme.typography.labelSmall
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val cellDensity =
        remember(density) { Density(density.density, density.fontScale.coerceAtMost(MAX_CELL_FONT_SCALE)) }
    return remember(model, measurer, dayStyle, smallStyle, cellDensity, direction) {
        val labels = CellLabels.of(model)
        val probe = CellFitProbe(measurer, dayStyle, smallStyle, cellDensity, direction)
        val fit: (Int, Int) -> DayCellTextFit = { cellWidth, cellHeight ->
            if (labels.count > MAX_PROBED_LABELS) {
                DayCellTextFit.SHRINK_TO_FIT
            } else {
                probe.fit(labels, cellWidth, cellHeight)
            }
        }
        fit
    }
}

/** The distinct labels of a grid's lines, and how many small lines its fullest cell has. */
internal class CellLabels(
    val days: List<String>,
    val small: List<String>,
    val smallLines: Int,
    val hasDots: Boolean,
) {
    val count: Int get() = days.size + small.size

    companion object {
        fun of(model: MonthGridModel): CellLabels =
            CellLabels(
                days = model.cells.map { it.dayLabel }.distinct(),
                small =
                    (model.cells.flatMap { it.secondaryLabels } + model.cells.mapNotNull { it.shiftLabel })
                        .distinct(),
                smallLines = model.cells.maxOf { it.secondaryLabels.size + if (it.shiftLabel == null) 0 else 1 },
                hasDots = model.cells.any { it.indicators.isNotEmpty() },
            )
    }
}

/** Measures a grid's labels to find the font scale of each line kind (see [rememberCellTextFitProbe]). */
private class CellFitProbe(
    private val measurer: TextMeasurer,
    private val dayStyle: TextStyle,
    private val smallStyle: TextStyle,
    private val density: Density,
    private val direction: LayoutDirection,
) {
    fun fit(
        labels: CellLabels,
        cellWidth: Int,
        cellHeight: Int,
    ): DayCellTextFit {
        val dayLine = measure(labels.days, dayStyle, 1f, DAY_WEIGHT)
        val smallLine = measure(labels.small, smallStyle, 1f, LABEL_WEIGHT)
        val lines = listOf(dayLine) + List(labels.smallLines) { smallLine }
        val dots = if (labels.hasDots) with(density) { DOTS_HEIGHT.roundToPx() } else 0
        val padding = with(density) { CELL_PADDING.roundToPx() }
        val scales =
            DayCellFit.scales(cellWidth, cellHeight, padding, dots, lines) ?: return DayCellTextFit.SHRINK_TO_FIT
        val slots = Slots(cellWidth, cellHeight, padding, dots, lines.sumOf { it.weight.toDouble() }.toFloat())
        val day = confirmed(labels.days, dayStyle, DAY_WEIGHT, scales.first(), slots)
        val small =
            if (labels.smallLines == 0) 1f else confirmed(labels.small, smallStyle, LABEL_WEIGHT, scales[1], slots)
        return when {
            day == null || small == null -> DayCellTextFit.SHRINK_TO_FIT
            day >= 1f && small >= 1f -> DayCellTextFit.FULL_SIZE
            else -> DayCellTextFit(day, small)
        }
    }

    /**
     * [scale] or a slightly smaller one at which every one of [labels] fits its slot when measured, or `null` when
     * none does above the smallest line size; the ratio is exact only to rounding, so it is checked, not trusted.
     */
    private fun confirmed(
        labels: List<String>,
        style: TextStyle,
        weight: Float,
        scale: Float,
        slots: Slots,
    ): Float? {
        var candidate = DayCellFit.snap(scale)
        repeat(CONFIRM_ATTEMPTS) {
            if (style.fontSize.value * candidate < MIN_LINE_TEXT_SIZE.value) return null
            val line = measure(labels, style, candidate, weight)
            if (DayCellFit.fitsSlot(line.widthPx, line.heightPx, slots.width, slots.heightOf(weight))) return candidate
            candidate = DayCellFit.snap(candidate - DayCellFit.SCALE_STEP)
        }
        return null
    }

    /** The widest and tallest of [labels] at [scale] times [style]'s size, as one line of the cell column. */
    private fun measure(
        labels: List<String>,
        style: TextStyle,
        scale: Float,
        weight: Float,
    ): CellLine {
        val scaled = style.cellLine(scale)
        val measured =
            labels.map { label ->
                measurer
                    .measure(
                        text = label,
                        style = scaled,
                        maxLines = 1,
                        layoutDirection = direction,
                        density = density,
                    ).size
            }
        return CellLine(weight, measured.maxOfOrNull { it.width } ?: 0, measured.maxOfOrNull { it.height } ?: 0)
    }
}

/** The inner width of a cell and the height its column gives a line of a given weight, in pixels. */
private class Slots(
    cellWidth: Int,
    cellHeight: Int,
    padding: Int,
    dots: Int,
    private val totalWeight: Float,
) {
    val width: Float = (cellWidth - 2 * padding).coerceAtLeast(0).toFloat()
    private val height: Float = (cellHeight - 2 * padding - dots).coerceAtLeast(0).toFloat()

    fun heightOf(weight: Float): Float = height * weight / totalWeight
}

/** Measurements of one line kind before the probe gives up and lets the cells search their own sizes. */
private const val CONFIRM_ATTEMPTS = 8

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
