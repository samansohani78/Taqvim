/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.painter

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import ir.taqvim.core.testing.PropertyTesting
import ir.taqvim.core.ui.component.DayCellModel
import ir.taqvim.core.ui.component.MonthGridModel
import ir.taqvim.core.ui.component.WeekNumberModel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class PainterGeometryTest {
    private fun model(
        columns: Int,
        rows: Int,
        weeks: Boolean,
    ) = MonthGridModel(
        weekdayLabels = List(columns) { "d$it" },
        cells = List(columns * rows) { DayCellModel("$it", "Day $it") },
        weekNumbers = if (weeks) List(rows) { WeekNumberModel("$it", "Week $it") } else null,
    )

    @Test
    fun `month cells tile every row without gaps and mirror in RTL`(): Unit =
        runBlocking {
            checkAll(
                PropertyTesting.iterations,
                Arb.int(40..1600),
                Arb.int(40..1600),
                Arb.int(1..8),
                Arb.int(1..6),
                Arb.boolean(),
            ) { width, height, columns, rows, weeks ->
                val grid = model(columns, rows, weeks)
                val ltr = MonthBitmapLayout(width, height, grid, 48, 30, rtl = false)
                val rtl = MonthBitmapLayout(width, height, grid, 48, 30, rtl = true)
                repeat(rows) { row ->
                    val cells = (0 until columns).map { ltr.cellRect(it, row) }
                    ltr.weekRect(row).left shouldBe 0f
                    cells.first().left shouldBe ltr.weekRect(row).right
                    cells.last().right shouldBe width.toFloat()
                    cells.zipWithNext().forEach { (start, end) -> start.right shouldBe end.left }
                    cells.forEachIndexed { column, cell ->
                        val mirrored = PixelRect(width - cell.right, cell.top, width - cell.left, cell.bottom)
                        rtl.cellRect(column, row) shouldBe mirrored
                    }
                    rtl.weekRect(row).right shouldBe width.toFloat()
                }
                ltr.headerRect(0).top shouldBe 0f
                ltr.cellRect(0, 0).top shouldBe ltr.header.toFloat()
                ltr.header + rows * ltr.rowHeight shouldBeLessThanOrEqual height
            }
        }

    @Test
    fun `week numbers take no width without them and at most a column's share with them`() {
        MonthBitmapLayout(700, 400, model(7, 5, weeks = false), 48, 30, rtl = false).weekRect(0).width shouldBe 0f
        MonthBitmapLayout(700, 400, model(7, 5, weeks = true), 48, 30, rtl = false).weekRect(0).width shouldBe 48f
        MonthBitmapLayout(160, 400, model(7, 5, weeks = true), 48, 30, rtl = false).weekRect(0).width shouldBe 20f
        MonthBitmapLayout(700, 20, model(7, 5, weeks = false), 48, 30, rtl = false).header shouldBe 20
    }

    @Test
    fun `rectangle centers and sizes`() {
        val rect = PixelRect(10f, 20f, 50f, 100f)
        rect.width shouldBe 40f
        rect.height shouldBe 80f
        rect.centerX shouldBe 30f
        rect.centerY shouldBe 60f
    }

    @Test
    fun `event dots are centered, evenly spaced and at most three`() {
        IndicatorGeometry.dotCenters(0, 50f, 2f, 2f) shouldBe emptyList()
        IndicatorGeometry.dotCenters(-1, 50f, 2f, 2f) shouldBe emptyList()
        IndicatorGeometry.dotCenters(1, 50f, 2f, 2f) shouldBe listOf(50f)
        IndicatorGeometry.dotCenters(2, 50f, 2f, 2f) shouldBe listOf(47f, 53f)
        IndicatorGeometry.dotCenters(7, 50f, 2f, 2f) shouldBe listOf(44f, 50f, 56f)
    }

    @Test
    fun `text shrinks only when it does not fit`() {
        TextFit.size(14f, 30f, 40f) shouldBe 14f
        TextFit.size(14f, 80f, 40f) shouldBe 7f
        TextFit.size(14f, 0f, -5f) shouldBe 14f
        TextFit.size(14f, 10f, -5f) shouldBe 0f
    }

    @Test
    fun `map points are placed inside the padding and clamped`() {
        MapProjection.toPixel(NormalizedPoint(0f, 0f), 200, 100, 10f) shouldBe (10f to 10f)
        MapProjection.toPixel(NormalizedPoint(1f, 1f), 200, 100, 10f) shouldBe (190f to 90f)
        MapProjection.toPixel(NormalizedPoint(Float.NaN, 2f), 200, 100, 10f) shouldBe (10f to 90f)
        MapProjection.toPixel(NormalizedPoint(0.5f, 0.5f), 10, 10, 20f) shouldBe (20f to 20f)
        shouldThrow<IllegalArgumentException> { MapShape(listOf(NormalizedPoint(0f, 0f)), closed = false) }
    }

    @Test
    fun `alpha replacement keeps the color and clamps the alpha`() {
        PainterColors.withAlpha(0xFF336699.toInt(), 0x61) shouldBe 0x61336699
        PainterColors.withAlpha(0x00336699, 300) shouldBe 0xFF336699.toInt()
        PainterColors.withAlpha(0xFF336699.toInt(), -4) shouldBe 0x00336699
    }
}
