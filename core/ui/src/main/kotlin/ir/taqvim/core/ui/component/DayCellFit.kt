/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.component

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf

/**
 * How a [DayCell] sizes its text lines.
 *
 * Shrinking a line to fit costs about seven text layouts per line, because `TextAutoSize.StepBased` binary-searches
 * the font size on every measure pass. A month page has up to 42 cells of up to four lines, and the pager keeps the
 * neighbouring pages composed, so the search dominated the measure phase of a swipe (BUG-2). A month grid gives every
 * cell the same fixed constraints, so the grid can measure the longest label of each line once and, when everything
 * fits at its own size, tell the cells to skip the search.
 */
@Immutable
public enum class DayCellTextFit {
    /** Draw every line at its style's own size; the grid has proved that the longest label of each line fits. */
    FULL_SIZE,

    /** Shrink each line until it fits its share of the cell (`TextAutoSize`); correct for any cell, and slower. */
    SHRINK_TO_FIT,
}

/** The fit a [DayCell] uses; [MonthGrid] sets it per page, and a cell outside a grid shrinks, which always fits. */
public val LocalDayCellTextFit: androidx.compose.runtime.ProvidableCompositionLocal<DayCellTextFit> =
    compositionLocalOf { DayCellTextFit.SHRINK_TO_FIT }

/** One measured line of the widest cell content: its [weight] in the cell column and its size in pixels. */
@Immutable
internal data class CellLine(
    val weight: Float,
    val widthPx: Int,
    val heightPx: Int,
)

/** Pure arithmetic of the cell fit probe, in pixels. */
internal object DayCellFit {
    /**
     * Fraction of a line's own slot that its text may fill for the grid to skip the search. The margin covers the
     * difference between a measured line and the same line inside the cell's column (rounding of the weighted shares,
     * the line height the style adds around the glyphs), so a cell that is anywhere near its limit keeps shrinking.
     */
    private const val SAFETY = 0.95f

    /**
     * Whether every [lines] entry fits its slot at full size inside a cell of [cellWidth] × [cellHeight] pixels with
     * [paddingPx] on each side and [extraHeightPx] taken by the indicator dots. Heights share what is left of the cell
     * in proportion to their weights, as the cell's column does; an empty list fits.
     */
    fun fitsAtFullSize(
        cellWidth: Int,
        cellHeight: Int,
        paddingPx: Int,
        extraHeightPx: Int,
        lines: List<CellLine>,
    ): Boolean {
        if (lines.isEmpty()) return true
        val innerWidth = (cellWidth - 2 * paddingPx).coerceAtLeast(0)
        val innerHeight = (cellHeight - 2 * paddingPx - extraHeightPx).coerceAtLeast(0)
        val totalWeight = lines.sumOf { it.weight.toDouble() }.toFloat()
        if (totalWeight <= 0f) return false
        return lines.all { line ->
            val slot = innerHeight * line.weight / totalWeight
            line.widthPx <= innerWidth * SAFETY && line.heightPx <= slot * SAFETY
        }
    }
}
