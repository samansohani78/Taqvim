/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.component

import kotlin.math.roundToInt

/** Pure index arithmetic of [NumberWheel]. */
internal object WheelMath {
    /** Index into [range] of [value], clamped to the range. */
    fun indexOf(
        value: Int,
        range: IntRange,
    ): Int = value.coerceIn(range.first, range.last) - range.first

    /** Value of [range] at [index], clamped to the range. */
    fun valueAt(
        index: Int,
        range: IntRange,
    ): Int = (range.first + index).coerceIn(range.first, range.last)

    /**
     * Index of the item nearest the selection band when the list shows item [firstIndex] scrolled by [offsetPx] of an
     * item of [itemPx] pixels; the band is one item below the top content padding.
     */
    fun centeredIndex(
        firstIndex: Int,
        offsetPx: Int,
        itemPx: Int,
        count: Int,
    ): Int {
        val next = if (itemPx > 0 && offsetPx * 2 >= itemPx) 1 else 0
        return (firstIndex + next).coerceIn(0, (count - 1).coerceAtLeast(0))
    }

    /** The value [progress] (a TalkBack/semantics target) selects in [range]. */
    fun valueForProgress(
        progress: Float,
        range: IntRange,
    ): Int = if (progress.isNaN()) range.first else progress.roundToInt().coerceIn(range.first, range.last)
}
