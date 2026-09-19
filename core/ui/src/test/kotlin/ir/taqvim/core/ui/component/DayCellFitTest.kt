/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.component

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * BUG-2: the month grid must draw its cells at full size on a roomy phone cell, so no line pays for the auto-size
 * search, and must keep shrinking wherever a label is close to its slot.
 */
@RunWith(AndroidJUnit4::class)
class DayCellFitTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aLineThatFillsItsSlotDoesNotFitAtFullSize() {
        val roomy = listOf(CellLine(DAY_WEIGHT, widthPx = 30, heightPx = 40))
        DayCellFit.fitsAtFullSize(CELL, CELL, PADDING, extraHeightPx = 0, lines = roomy) shouldBe true

        val tooWide = listOf(CellLine(DAY_WEIGHT, widthPx = CELL - 2 * PADDING, heightPx = 40))
        DayCellFit.fitsAtFullSize(CELL, CELL, PADDING, extraHeightPx = 0, lines = tooWide) shouldBe false

        val tooTall = listOf(CellLine(DAY_WEIGHT, widthPx = 30, heightPx = CELL - 2 * PADDING))
        DayCellFit.fitsAtFullSize(CELL, CELL, PADDING, extraHeightPx = 0, lines = tooTall) shouldBe false
    }

    @Test
    fun linesShareTheCellHeightByWeightAfterTheDotsAndPadding() {
        // Three lines of weights 2, 1 and 1 in 160 px of content: the day gets 80 px and each label 40 px.
        val lines =
            listOf(
                CellLine(DAY_WEIGHT, widthPx = 10, heightPx = 75),
                CellLine(LABEL_WEIGHT, widthPx = 10, heightPx = 37),
                CellLine(LABEL_WEIGHT, widthPx = 10, heightPx = 37),
            )
        DayCellFit.fitsAtFullSize(CELL, cellHeight = 160 + 2 * PADDING, PADDING, 0, lines) shouldBe true

        // The same lines lose their margin once the indicator dots take 20 px of the same height.
        DayCellFit.fitsAtFullSize(CELL, cellHeight = 160 + 2 * PADDING, PADDING, 20, lines) shouldBe false
    }

    @Test
    fun emptyLinesFitAndAWeightlessLineDoesNot() {
        DayCellFit.fitsAtFullSize(CELL, CELL, PADDING, 0, emptyList()) shouldBe true
        DayCellFit.fitsAtFullSize(CELL, CELL, PADDING, 0, listOf(CellLine(0f, 1, 1))) shouldBe false
    }

    @Test
    fun aPhoneSizedCellDrawsAtFullSizeAndATinyOneShrinks() {
        lateinit var probe: (Int, Int) -> DayCellTextFit
        composeRule.setContent { TestTheme { probe = rememberCellTextFitProbe(sampleMonth()) } }
        composeRule.waitForIdle()

        probe(PHONE_CELL_WIDTH, PHONE_CELL_HEIGHT) shouldBe DayCellTextFit.FULL_SIZE
        probe(TINY_CELL, TINY_CELL) shouldBe DayCellTextFit.SHRINK_TO_FIT
    }

    @Test
    fun aMonthWithMoreDistinctLabelsThanTheProbeMeasuresKeepsShrinking() {
        val crowded =
            sampleMonth().let { month ->
                month.copy(
                    cells =
                        month.cells.mapIndexed {
                            i,
                            cell,
                            ->
                            cell.copy(secondaryLabels = listOf("l$i", "m$i"))
                        },
                )
            }
        lateinit var probe: (Int, Int) -> DayCellTextFit
        composeRule.setContent { TestTheme { probe = rememberCellTextFitProbe(crowded) } }
        composeRule.waitForIdle()

        probe(PHONE_CELL_WIDTH, PHONE_CELL_HEIGHT) shouldBe DayCellTextFit.SHRINK_TO_FIT
    }

    private companion object {
        const val CELL = 100
        const val PADDING = 5

        /** A 1080 px wide phone: seven columns of ~150 px and a five-row grid in the page body. */
        const val PHONE_CELL_WIDTH = 150
        const val PHONE_CELL_HEIGHT = 220
        const val TINY_CELL = 40
    }
}
