/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.component

/**
 * Which month presentation shows a [MonthGridModel] at a given system font scale (R10, T-1701).
 *
 * [MonthGrid] gives every cell of a page the same fixed size and, at or below [MAX_CELL_FONT_SCALE], proves that its
 * text fits at that size (BUG-2); a system font scale above that cap cannot fit a phone's fixed-size cells, so the
 * caller must switch to [MonthDayList] instead, which honours the full requested scale in a scrollable full-width
 * list. [MonthGrid] and [MonthDayList] take the same model and callbacks, so a caller only needs to choose between
 * them; neither component makes the choice itself.
 */
public enum class MonthDisplayMode {
    /** [MonthGrid], unchanged from before T-1701. */
    GRID,

    /** [MonthDayList], one full-width row per day, for a font scale [MonthGrid] cannot fit. */
    LIST,
    ;

    public companion object {
        /**
         * [GRID] at or below [MAX_CELL_FONT_SCALE] (the same cap [MonthGrid] and [DayCell] already apply inside a
         * grid); [LIST] above it, where a fixed-size grid cell would either clip the day's text or force a scale the
         * user did not ask for.
         */
        public fun forFontScale(fontScale: Float): MonthDisplayMode =
            if (fontScale > MAX_CELL_FONT_SCALE) LIST else GRID
    }
}
