/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.component

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/** R10, T-1701: the month page keeps its grid up to the cap [MonthGrid] itself uses, and switches above it. */
class MonthDisplayModeTest {
    @Test
    fun `the grid is kept at and below its font-scale cap`() {
        MonthDisplayMode.forFontScale(1.0f) shouldBe MonthDisplayMode.GRID
        MonthDisplayMode.forFontScale(MAX_CELL_FONT_SCALE) shouldBe MonthDisplayMode.GRID
    }

    @Test
    fun `the list is chosen above the grid's font-scale cap`() {
        MonthDisplayMode.forFontScale(1.75f) shouldBe MonthDisplayMode.LIST
        MonthDisplayMode.forFontScale(2.0f) shouldBe MonthDisplayMode.LIST
    }

    @Test
    fun `a scale just past the cap already switches to the list`() {
        val justAbove = MAX_CELL_FONT_SCALE + MIN_STEP
        MonthDisplayMode.forFontScale(justAbove) shouldBe MonthDisplayMode.LIST
    }

    private companion object {
        const val MIN_STEP = 0.001f
    }
}
