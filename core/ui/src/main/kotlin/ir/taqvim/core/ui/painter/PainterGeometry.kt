/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.painter

import ir.taqvim.core.ui.component.MonthGridGeometry
import ir.taqvim.core.ui.component.MonthGridModel
import ir.taqvim.core.ui.component.unitFraction

private const val RGB_MASK = 0x00FFFFFF
private const val ALPHA_SHIFT = 24
private const val ALPHA_MAX = 0xFF

/** An axis-aligned rectangle in bitmap pixels. */
internal data class PixelRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float
        get() = right - left

    val height: Float
        get() = bottom - top

    val centerX: Float
        get() = (left + right) / 2

    val centerY: Float
        get() = (top + bottom) / 2
}

/**
 * Placement of a [MonthGridModel] in a [width] × [height] bitmap: a header row of [headerHeight] pixels, then rows that
 * share the rest; columns fill the width without gaps after a week-number column of up to [weekColumnWidth] pixels on
 * the start edge. Uses the T-701 [MonthGridGeometry] and mirrors horizontally in RTL.
 */
internal class MonthBitmapLayout(
    private val width: Int,
    height: Int,
    model: MonthGridModel,
    weekColumnWidth: Int,
    headerHeight: Int,
    private val rtl: Boolean,
) {
    private val edges: List<Int>

    /** Height of the weekday header row. */
    val header: Int = headerHeight.coerceIn(0, height)

    /** Height of each week row. */
    val rowHeight: Int

    init {
        val columns = model.weekdayLabels.size
        val leading = if (model.weekNumbers == null) 0 else minOf(weekColumnWidth, width / (columns + 1))
        edges = MonthGridGeometry.columnEdges(width, leading, columns)
        rowHeight = MonthGridGeometry.rowHeight(height, header, model.rows, edges[1] - edges[0], 0)
    }

    /** The header of [column]. */
    fun headerRect(column: Int): PixelRect = horizontal(edges[column], edges[column + 1], 0, header)

    /** The cell in [column] of [row]. */
    fun cellRect(
        column: Int,
        row: Int,
    ): PixelRect {
        val top = header + row * rowHeight
        return horizontal(edges[column], edges[column + 1], top, top + rowHeight)
    }

    /** The week number of [row]. */
    fun weekRect(row: Int): PixelRect {
        val top = header + row * rowHeight
        return horizontal(0, edges.first(), top, top + rowHeight)
    }

    private fun horizontal(
        start: Int,
        end: Int,
        top: Int,
        bottom: Int,
    ): PixelRect =
        if (rtl) {
            PixelRect((width - end).toFloat(), top.toFloat(), (width - start).toFloat(), bottom.toFloat())
        } else {
            PixelRect(start.toFloat(), top.toFloat(), end.toFloat(), bottom.toFloat())
        }
}

/** Event dots under a day number. */
internal object IndicatorGeometry {
    /** Most dots drawn for one day. */
    const val MAX_DOTS: Int = 3

    /** Horizontal centers of up to [MAX_DOTS] dots of [radius] separated by [gap], centered on [centerX]. */
    fun dotCenters(
        count: Int,
        centerX: Float,
        radius: Float,
        gap: Float,
    ): List<Float> {
        val shown = count.coerceIn(0, MAX_DOTS)
        val step = 2 * radius + gap
        val first = centerX - step * (shown - 1) / 2
        return List(shown) { first + it * step }
    }
}

/** Text sizing. */
internal object TextFit {
    /** [desired] size, reduced proportionally when a text [measuredWidth] wide at that size exceeds [maxWidth]. */
    fun size(
        desired: Float,
        measuredWidth: Float,
        maxWidth: Float,
    ): Float =
        if (measuredWidth <= maxWidth || measuredWidth <= 0f) {
            desired
        } else {
            desired * maxWidth.coerceAtLeast(0f) / measuredWidth
        }
}

/** Placement of caller-projected map geometry. */
internal object MapProjection {
    /** Pixel position of [point] in a [width] × [height] bitmap with [padding] on every side; clamped to the area. */
    fun toPixel(
        point: NormalizedPoint,
        width: Int,
        height: Int,
        padding: Float,
    ): Pair<Float, Float> {
        val usableWidth = (width - 2 * padding).coerceAtLeast(0f)
        val usableHeight = (height - 2 * padding).coerceAtLeast(0f)
        return padding + unitFraction(point.x) * usableWidth to padding + unitFraction(point.y) * usableHeight
    }
}

/** Color arithmetic on `0xAARRGGBB` ints. */
internal object PainterColors {
    /** [color] with its alpha replaced by [alpha] (0‥255). */
    fun withAlpha(
        color: Int,
        alpha: Int,
    ): Int = (color and RGB_MASK) or (alpha.coerceIn(0, ALPHA_MAX) shl ALPHA_SHIFT)
}
