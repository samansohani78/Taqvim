/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.component

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlin.math.roundToInt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * BUG-2: the month grid must draw its cells at full size on a roomy phone cell, so no line pays for the auto-size
 * search, and must keep shrinking wherever a label is close to its slot. Native graphics gives real text metrics: the
 * legacy mode measures every string as the same fixed box, whatever its font size.
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
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
    fun aRoomyCellDrawsAtFullSizeASmallOneAtOneScaleAndOnlyAHopelessOneShrinks() {
        lateinit var probe: (Int, Int) -> DayCellTextFit
        composeRule.setContent { TestTheme { probe = rememberCellTextFitProbe(sampleMonth()) } }
        composeRule.waitForIdle()

        probe(PHONE_CELL_WIDTH, PHONE_CELL_HEIGHT) shouldBe DayCellTextFit.FULL_SIZE
        val small = probe(TINY_CELL, TINY_CELL)
        small.shrinkToFit shouldBe false
        small.labelScale shouldBeLessThan 1f
        probe(HOPELESS_CELL, HOPELESS_CELL) shouldBe DayCellTextFit.SHRINK_TO_FIT
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
                            cell.copy(secondaryLabels = List(CROWDED_LABELS_PER_CELL) { line -> "$line.$i" })
                        },
                )
            }
        lateinit var probe: (Int, Int) -> DayCellTextFit
        composeRule.setContent { TestTheme { probe = rememberCellTextFitProbe(crowded) } }
        composeRule.waitForIdle()

        probe(PHONE_CELL_WIDTH, PHONE_CELL_HEIGHT) shouldBe DayCellTextFit.SHRINK_TO_FIT
    }

    @Test
    fun theScaleOfALineIsTheSmallerOfItsWidthAndHeightRatios() {
        // 90 px inner width and 90 px inner height; the day line gets 60 px and the label 30 px (weights 2 and 1).
        val lines =
            listOf(
                CellLine(DAY_WEIGHT, widthPx = 40, heightPx = 40),
                CellLine(LABEL_WEIGHT, widthPx = 30, heightPx = 38),
            )
        val scales = checkNotNull(DayCellFit.scales(CELL, CELL, PADDING, 0, lines))

        scales[0] shouldBe 1f
        scales[1] shouldBe (30f / 38 plusOrMinus 0.001f)
        DayCellFit.scales(CELL, CELL, PADDING, 0, listOf(CellLine(0f, 1, 1))) shouldBe null
    }

    @Test
    fun scalesSnapDownToACommonStepSoNeighbouringPagesShareOne() {
        DayCellFit.snap(0.7063f) shouldBe (0.70f plusOrMinus 0.0001f)
        DayCellFit.snap(0.7199f) shouldBe (0.70f plusOrMinus 0.0001f)
        DayCellFit.snap(0.9f) shouldBe (0.90f plusOrMinus 0.0001f)
        DayCellFit.snap(1.3f) shouldBe 1f
    }

    @Test
    @Config(qualifiers = "xxhdpi")
    fun aRealPhoneGridScalesItsSmallLinesOnceInsteadOfShrinkingEveryCell() {
        // The calendar's cells are square (six rows of seven fill the pager's width) and carry two secondary dates
        // and event dots: at 58 dp the small lines do not fit at full size, which used to send all 42 cells into the
        // per-cell auto-size search on every page (BUG-2). The probe must answer with one fixed scale instead.
        lateinit var probe: (Int, Int) -> DayCellTextFit
        composeRule.setContent { TestTheme { probe = rememberCellTextFitProbe(phoneMonth()) } }
        composeRule.waitForIdle()
        val cell = (PHONE_CELL_DP * composeRule.density.density).roundToInt()

        val fit = probe(cell, cell)

        fit.shrinkToFit shouldBe false
        fit.dayScale shouldBe 1f
        fit.labelScale shouldBeLessThan 1f
        fit.labelScale shouldBeGreaterThan 0.5f
        DayCellFit.snap(fit.labelScale) shouldBe fit.labelScale
    }

    @Test
    @Config(qualifiers = "xxhdpi")
    fun aGridCellDrawsItsLinesAndDotsInOneNode() {
        // Six rows of seven 58 dp cells, as the calendar lays them out on a phone.
        composeRule.setContent {
            TestTheme {
                MonthGrid(phoneMonth(), onDayClick = {}, modifier = Modifier.width(406.dp).height(372.dp))
            }
        }
        val cell = composeRule.onNodeWithContentDescription("Day 2")

        // The day, its two secondary dates and its two dots are drawn, not composed as text nodes.
        cell.onChildren().assertCountEquals(0)
        val pixels = cell.captureToImage().toPixelMap()
        val background = pixels[1, 1]
        val drawn =
            (0 until pixels.height).sumOf { y ->
                (0 until pixels.width).count { x -> pixels[x, y] != background }
            }
        drawn shouldBeGreaterThan pixels.width * pixels.height / DRAWN_FRACTION
    }

    private companion object {
        const val CELL = 100
        const val PADDING = 5

        /** A 1080 px wide phone: seven columns of ~150 px and a five-row grid in the page body. */
        const val PHONE_CELL_WIDTH = 150
        const val PHONE_CELL_HEIGHT = 220
        const val TINY_CELL = 40

        /** A cell with a day, two secondary dates and dots colours well over this share of its pixels. */
        const val DRAWN_FRACTION = 50

        /** Too small for any label at the smallest line size: only the per-cell search can fit it. */
        const val HOPELESS_CELL = 12

        /** Five distinct labels in each of the 35 cells: more than the probe measures. */
        const val CROWDED_LABELS_PER_CELL = 5

        /** A day cell of the calendar on a 1080 px, 420 dpi phone: 154 px square. */
        const val PHONE_CELL_DP = 58f
    }
}

/** A 6-week month as the calendar builds it: every cell has two secondary dates and some have event dots. */
private fun phoneMonth(): MonthGridModel =
    MonthGridModel(
        weekdayLabels = listOf("Sa", "Su", "Mo", "Tu", "We", "Th", "Fr"),
        cells =
            List(42) { index ->
                val day = index % 31 + 1
                DayCellModel(
                    dayLabel = "$day",
                    contentDescription = "Day $index",
                    secondaryLabels = listOf("${(day + 11) % 30 + 1}", "${(day + 21) % 31 + 1}"),
                    indicators = List(index % 3) { Color(0xFF3F8F5A) },
                )
            },
    )
