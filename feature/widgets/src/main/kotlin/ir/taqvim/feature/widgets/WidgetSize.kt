/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

/**
 * Responsive layout buckets of widgets (T-1200), named by their cell span. Each bucket's size follows the classic
 * launcher cell rule of 70·n − 30 dp for n cells, so a layout designed for a bucket fits a widget of that span.
 */
enum class WidgetSize(
    val columns: Int,
    val rows: Int,
) {
    SMALL(1, 1),
    WIDE(4, 1),
    MEDIUM(2, 2),
    LARGE(4, 2),
    EXTRA_LARGE(4, 4),
    ;

    val widthDp: Int get() = cellsToDp(columns)
    val heightDp: Int get() = cellsToDp(rows)

    private val area: Int get() = widthDp * heightDp

    private fun fits(
        availableWidthDp: Float,
        availableHeightDp: Float,
    ): Boolean = widthDp <= availableWidthDp && heightDp <= availableHeightDp

    companion object {
        private const val DP_PER_CELL = 70
        private const val CELL_INSET_DP = 30

        /** The width or height in dp of [cells] launcher cells. */
        fun cellsToDp(cells: Int): Int = DP_PER_CELL * cells - CELL_INSET_DP

        /**
         * The bucket to lay out for when the launcher gives the widget [availableWidthDp] × [availableHeightDp]: the
         * largest of [supported] that fits, or the smallest of them when none fits.
         */
        fun forAvailable(
            availableWidthDp: Float,
            availableHeightDp: Float,
            supported: List<WidgetSize>,
        ): WidgetSize {
            require(supported.isNotEmpty()) { "a widget supports at least one size" }
            return supported.filter { it.fits(availableWidthDp, availableHeightDp) }.maxByOrNull { it.area }
                ?: supported.minBy { it.area }
        }
    }
}
