/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * One day of a month grid, fully prepared by the caller (T-801 builds these off the main thread):
 * - [dayLabel] in the app's digits and [secondaryLabels] (days in other calendars);
 * - [indicators]: colors of the day's events (at most three dots are drawn) and an optional [shiftLabel];
 * - [contentDescription]: the spoken summary of the date and its events (T-1700);
 * - [isHoliday] covers official holidays and weekend days; [inCurrentMonth] is false for leading/trailing days.
 */
@Immutable
public data class DayCellModel(
    public val dayLabel: String,
    public val contentDescription: String,
    public val secondaryLabels: List<String> = emptyList(),
    public val indicators: List<Color> = emptyList(),
    public val shiftLabel: String? = null,
    public val isToday: Boolean = false,
    public val isSelected: Boolean = false,
    public val isHoliday: Boolean = false,
    public val inCurrentMonth: Boolean = true,
)

/** How the day number of a [DayCellModel] is colored, in order of precedence. */
internal enum class DayTone {
    OUTSIDE_MONTH,
    SELECTED,
    HOLIDAY,
    NORMAL,
    ;

    companion object {
        fun of(model: DayCellModel): DayTone =
            when {
                !model.inCurrentMonth -> OUTSIDE_MONTH
                model.isSelected -> SELECTED
                model.isHoliday -> HOLIDAY
                else -> NORMAL
            }
    }
}

/** Maximum number of event dots under a day number. */
internal const val MAX_INDICATORS = 3

private const val OUTSIDE_MONTH_ALPHA = 0.45f

/** Opacity of the Material pressed state layer over a pressed day. */
internal const val PRESSED_ALPHA = 0.10f

/**
 * Largest font scale applied inside a day cell. A month grid has a fixed cell size per screen, so larger user font
 * scales are capped here instead of clipping the day and its secondary dates (T-1701); the cell's spoken summary and
 * the day details panel carry the same information at the user's full font scale.
 */
internal const val MAX_CELL_FONT_SCALE = 1.3f
internal const val DAY_WEIGHT = 2f
internal const val LABEL_WEIGHT = 1f

/** Smallest size a cell line shrinks to; the grid's probe lets the cells search below none of its scales. */
internal val MIN_LINE_TEXT_SIZE = 5.sp
private val CELL_SHAPE = RoundedCornerShape(12.dp)
internal val TODAY_BORDER = 2.dp
internal val DOT_SIZE = 5.dp
internal val DOT_TOP_PADDING = 2.dp
internal val DOT_GAP = 2.dp

/** Inset of the cell's column on every side; the grid's fit probe subtracts it from the cell (`DayCellFit`). */
internal val CELL_PADDING = 2.dp

/** Height the indicator dots take under the lines, which the fit probe leaves out of the lines' share. */
internal val DOTS_HEIGHT = DOT_SIZE + DOT_TOP_PADDING

internal fun DayTone.color(colors: ColorScheme): Color =
    when (this) {
        DayTone.OUTSIDE_MONTH -> colors.onSurface.copy(alpha = OUTSIDE_MONTH_ALPHA)
        DayTone.SELECTED -> colors.onPrimaryContainer
        DayTone.HOLIDAY -> colors.error
        DayTone.NORMAL -> colors.onSurface
    }

/**
 * A month-grid day (T-701): selected days are filled, today is outlined, holidays use the holiday color. Accessibility
 * services read [DayCellModel.contentDescription] and the selection state; [longClickLabel] names the long-press action
 * (e.g. "new event").
 */
@Composable
public fun DayCell(
    model: DayCellModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    longClickLabel: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    val selectedFill = if (model.isSelected) Modifier.background(colors.primaryContainer) else Modifier
    val todayRing = if (model.isToday) Modifier.border(TODAY_BORDER, colors.primary, CELL_SHAPE) else Modifier
    val cell =
        modifier
            .clip(CELL_SHAPE)
            .dayCellInput(onClick, onLongClick, longClickLabel, colors.onSurface.copy(alpha = PRESSED_ALPHA))
            .clearAndSetSemantics {
                contentDescription = model.contentDescription
                selected = model.isSelected
            }.then(selectedFill)
            .then(todayRing)
            .padding(CELL_PADDING)
    val density = LocalDensity.current
    val cellDensity = Density(density.density, density.fontScale.coerceAtMost(MAX_CELL_FONT_SCALE))
    val fit = LocalDayCellTextFit.current
    val measurer = LocalCellTextMeasurer.current
    if (measurer != null && !fit.shrinkToFit) {
        val (dayStyle, smallStyle) = cellStyles(fit)
        CellLines(
            CellDrawing(cellTexts(model, dayStyle, smallStyle, colors), model.indicators),
            measurer,
            cellDensity,
            cell,
        )
    } else {
        CellColumn(model, fit, cellDensity, colors, cell)
    }
}

/** The lines of a cell outside a grid, or of a grid whose labels fit no probed scale: each line searches its size. */
@Composable
private fun CellColumn(
    model: DayCellModel,
    fit: DayCellTextFit,
    cellDensity: Density,
    colors: ColorScheme,
    modifier: Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CompositionLocalProvider(LocalDensity provides cellDensity) {
            val dayStyle = MaterialTheme.typography.titleMedium
            val smallStyle = MaterialTheme.typography.labelSmall
            val shrink = fit.shrinkToFit
            FittedLine(model.dayLabel, dayStyle, DayTone.of(model).color(colors), DAY_WEIGHT, fit.dayScale, shrink)
            model.secondaryLabels.forEach {
                FittedLine(it, smallStyle, colors.onSurfaceVariant, LABEL_WEIGHT, fit.labelScale, shrink)
            }
            model.shiftLabel?.let { FittedLine(it, smallStyle, colors.tertiary, LABEL_WEIGHT, fit.labelScale, shrink) }
            if (model.indicators.isNotEmpty()) IndicatorDots(model.indicators)
        }
    }
}

/** The text lines of [model] in the order and colors a cell draws them. */
internal fun cellTexts(
    model: DayCellModel,
    dayStyle: TextStyle,
    smallStyle: TextStyle,
    colors: ColorScheme,
): List<CellText> =
    buildList {
        add(CellText(model.dayLabel, dayStyle, DayTone.of(model).color(colors)))
        model.secondaryLabels.forEach { add(CellText(it, smallStyle, colors.onSurfaceVariant)) }
        model.shiftLabel?.let { add(CellText(it, smallStyle, colors.tertiary)) }
    }

/**
 * One line of a day cell that is never cut off: it takes at most its [weight] share of the cell height.
 *
 * Inside a month grid the line draws at [scale] times [style]'s size, a scale the grid has measured to fit every label
 * of the page, so it is laid out once (BUG-2). With [shrinkToFit] — a cell outside a grid, or labels that fit at no
 * probed scale — it shrinks from [style]'s size until it fits, which costs about seven layouts but always fits (T-1701).
 */
@Composable
private fun ColumnScope.FittedLine(
    text: String,
    style: TextStyle,
    color: Color,
    weight: Float,
    scale: Float,
    shrinkToFit: Boolean,
) {
    // A fixed sp line height would not shrink with the font, so the line takes the font's own height.
    val lineStyle = style.cellLine(scale)
    Text(
        text,
        modifier = Modifier.weight(weight, fill = false),
        style = lineStyle,
        color = color,
        maxLines = 1,
        autoSize =
            if (shrinkToFit) {
                TextAutoSize.StepBased(
                    minFontSize = MIN_LINE_TEXT_SIZE,
                    maxFontSize = style.fontSize,
                )
            } else {
                null
            },
    )
}

@Composable
internal fun IndicatorDots(colors: List<Color>) {
    Row(Modifier.padding(top = DOT_TOP_PADDING), horizontalArrangement = Arrangement.spacedBy(DOT_GAP)) {
        colors.take(MAX_INDICATORS).forEach { color ->
            Box(Modifier.size(DOT_SIZE).clip(CircleShape).background(color))
        }
    }
}
