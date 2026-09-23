/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.component

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import kotlin.math.floor

/**
 * How a [DayCell] sizes its text lines.
 *
 * Shrinking a line to fit costs about seven text layouts per line, because `TextAutoSize.StepBased` binary-searches
 * the font size on every measure pass. A month page has up to 42 cells of up to four lines, and the pager keeps the
 * neighbouring pages composed, so the search dominated the measure phase of a swipe (BUG-2). A month grid gives every
 * cell the same fixed constraints, so the grid works out one font scale per line kind — the day number and the small
 * secondary lines — measures the page's labels at that scale once, and the cells draw at it without searching.
 *
 * [dayScale] and [labelScale] multiply the day number's and the small lines' style sizes; [shrinkToFit] makes every
 * line search its own size instead, which fits any cell and is what a cell outside a grid, or a grid whose labels do
 * not fit even at the smallest size, uses.
 */
@Immutable
public data class DayCellTextFit(
    val dayScale: Float,
    val labelScale: Float,
    val shrinkToFit: Boolean = false,
) {
    public companion object {
        /** Every line at its style's own size; the grid has proved that the longest label of each line fits. */
        public val FULL_SIZE: DayCellTextFit = DayCellTextFit(1f, 1f)

        /** Each line shrinks until it fits its share of the cell (`TextAutoSize`); correct for any cell, and slow. */
        public val SHRINK_TO_FIT: DayCellTextFit = DayCellTextFit(1f, 1f, shrinkToFit = true)
    }
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
     * Fraction of a line's own slot that its text may fill for the grid to draw it at full size. The margin covers the
     * difference between a measured line and the same line inside the cell's column (rounding of the weighted shares,
     * the line height the style adds around the glyphs). A scaled line is confirmed by measuring it instead.
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

    /**
     * An estimate of the largest scale, at most 1, at which each of [lines] fits its slot, in the order of [lines];
     * `null` when the weights leave no slot. A line's width and height grow in proportion to its font size, so the
     * estimate is the smaller of the width and height ratios, with no margin — the size the per-cell search would
     * settle on. The grid confirms it by measuring the labels at that scale.
     */
    fun scales(
        cellWidth: Int,
        cellHeight: Int,
        paddingPx: Int,
        extraHeightPx: Int,
        lines: List<CellLine>,
    ): List<Float>? {
        val innerWidth = (cellWidth - 2 * paddingPx).coerceAtLeast(0)
        val innerHeight = (cellHeight - 2 * paddingPx - extraHeightPx).coerceAtLeast(0)
        val totalWeight = lines.sumOf { it.weight.toDouble() }.toFloat()
        if (totalWeight <= 0f) return null
        return lines.map { line ->
            val slot = innerHeight * line.weight / totalWeight
            val byWidth = if (line.widthPx > 0) innerWidth.toFloat() / line.widthPx else 1f
            val byHeight = if (line.heightPx > 0) slot / line.heightPx else 1f
            minOf(1f, byWidth, byHeight)
        }
    }

    /**
     * Steps a line scale is rounded down to. Every month has a slightly different widest label, so an exact scale would
     * differ from page to page and each page would lay out all its labels anew; on a common step, neighbouring pages
     * share a scale and the grid's text measurer answers their labels from its cache (BUG-2).
     */
    const val SCALE_STEP: Float = 0.02f

    /** [scale] rounded down to a [SCALE_STEP], at most 1. */
    fun snap(scale: Float): Float = minOf(1f, floor(scale / SCALE_STEP + SNAP_EPSILON) * SCALE_STEP)

    /** Keeps an exact step (e.g. 0.9, stored as 0.8999…) from being rounded down to the step below. */
    private const val SNAP_EPSILON = 1e-4f

    /**
     * Whether a line of [widthPx] × [heightPx] fits a slot of [slotWidthPx] × [slotHeightPx] without overflowing,
     * the test the per-cell search applies to each size it tries.
     */
    fun fitsSlot(
        widthPx: Int,
        heightPx: Int,
        slotWidthPx: Float,
        slotHeightPx: Float,
    ): Boolean = widthPx <= slotWidthPx && heightPx <= slotHeightPx
}
