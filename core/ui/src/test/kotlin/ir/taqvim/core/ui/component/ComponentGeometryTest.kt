/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.component

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class ComponentGeometryTest {
    /** Esfand has 30 days in 1403 and 29 in 1404; every other month has 31 (enough for the picker arithmetic). */
    private val daysInMonth: (Int, Int) -> Int = { year, month ->
        if (month != 12) {
            31
        } else if (year == 1403) {
            30
        } else {
            29
        }
    }

    private fun cell(
        selected: Boolean = false,
        holiday: Boolean = false,
        inMonth: Boolean = true,
    ) = DayCellModel("1", "day", isSelected = selected, isHoliday = holiday, inCurrentMonth = inMonth)

    @Test
    fun `wheel index arithmetic clamps to the range`() {
        WheelMath.indexOf(1, 1..31) shouldBe 0
        WheelMath.indexOf(40, 1..31) shouldBe 30
        WheelMath.valueAt(-5, 1..31) shouldBe 1
        WheelMath.valueAt(100, 1..31) shouldBe 31
        WheelMath.centeredIndex(3, 10, 48, 31) shouldBe 3
        WheelMath.centeredIndex(3, 24, 48, 31) shouldBe 4
        WheelMath.centeredIndex(30, 40, 48, 31) shouldBe 30
        WheelMath.centeredIndex(0, 0, 0, 0) shouldBe 0
        WheelMath.valueForProgress(Float.NaN, 1400..1410) shouldBe 1400
        WheelMath.valueForProgress(12.4f, 1..31) shouldBe 12
        WheelMath.valueForProgress(99f, 1..31) shouldBe 31
    }

    @Test
    fun `wheel windows list the blocks around the value in any range`(): Unit =
        runBlocking {
            WheelMath.window(5, 1..31) shouldBe 1..31
            WheelMath.window(1405, -999_999_000..999_999_000) shouldBe 0..2_999
            WheelMath.window(Int.MAX_VALUE, Int.MIN_VALUE..Int.MAX_VALUE) shouldBe 2_147_482_352..Int.MAX_VALUE
            WheelMath.window(Int.MIN_VALUE, Int.MIN_VALUE..Int.MAX_VALUE) shouldBe
                Int.MIN_VALUE..(Int.MIN_VALUE + 1_999)
            WheelMath.window(-7, 1..31) shouldBe 1..31
            checkAll(PropertyTesting.iterations, Arb.int(), Arb.int(), Arb.int()) { a, b, value ->
                val range = minOf(a, b)..maxOf(a, b)
                val window = WheelMath.window(value, range)
                val shown = value.coerceIn(range.first, range.last)
                (shown in window) shouldBe true
                (window.first >= range.first && window.last <= range.last) shouldBe true
                ((window.last.toLong() - window.first + 1) <= 3L * WheelMath.WINDOW_STEP) shouldBe true
                // Neighbouring values of the same block share the window, so the list does not move under the finger.
                val blockStart =
                    shown - Math.floorMod(shown.toLong() - range.first, WheelMath.WINDOW_STEP.toLong()).toInt()
                WheelMath.window(blockStart, range) shouldBe window
            }
        }

    @Test
    fun `date selection clamps the day to the month`() {
        val esfand = DateSelection(1403, 12, 30)
        esfand.withYear(1404, daysInMonth) shouldBe DateSelection(1404, 12, 29)
        DateSelection(1404, 12, 29).withMonth(1, daysInMonth) shouldBe DateSelection(1404, 1, 29)
        esfand.withDay(45, daysInMonth) shouldBe DateSelection(1403, 12, 30)
        esfand.withDay(0, daysInMonth) shouldBe DateSelection(1403, 12, 1)
        esfand.withDay(5) { _, _ -> 0 } shouldBe DateSelection(1403, 12, 1)
    }

    @Test
    fun `date picker model validates its calendar`() {
        val labels = DatePickerLabels("Pick", "Year", "Month", "Day", "OK", "Cancel")
        val model =
            DatePickerModel(DateSelection(1405, 1, 1), 1400..1410, listOf("A", "B"), daysInMonth, Int::toString, labels)
        model.monthName(0) shouldBe "A"
        model.monthName(2) shouldBe "B"
        model.monthName(13) shouldBe "B"
        shouldThrow<IllegalArgumentException> { model.copy(monthNames = emptyList()) }
        shouldThrow<IllegalArgumentException> { model.copy(years = IntRange.EMPTY) }
    }

    @Test
    fun `month grid model requires whole rows and one week number per row`() {
        val labels = List(7) { "D$it" }
        val cells = List(35) { cell() }
        MonthGridModel(labels, cells).rows shouldBe 5
        MonthGridModel(labels, cells, List(5) { WeekNumberModel("$it", "week $it") }).rows shouldBe 5
        shouldThrow<IllegalArgumentException> { MonthGridModel(emptyList(), cells) }
        shouldThrow<IllegalArgumentException> { MonthGridModel(labels, cells.drop(1)) }
        shouldThrow<IllegalArgumentException> { MonthGridModel(labels, emptyList()) }
        shouldThrow<IllegalArgumentException> { MonthGridModel(labels, cells, listOf(WeekNumberModel("1", "week 1"))) }
    }

    @Test
    fun `column edges fill the width without gaps`(): Unit =
        runBlocking {
            MonthGridGeometry.columnEdges(700, 0, 7) shouldBe (0..7).map { it * 100 }
            MonthGridGeometry.columnEdges(703, 3, 7) shouldBe (0..7).map { 3 + it * 100 }
            MonthGridGeometry.columnEdges(10, 20, 7) shouldBe List(8) { 20 }
            checkAll(PropertyTesting.iterations, Arb.int(0..4000), Arb.int(0..60), Arb.int(1..10)) { width, lead, n ->
                val edges = MonthGridGeometry.columnEdges(width, lead, n)
                edges.size shouldBe n + 1
                (edges.last() - edges.first()) shouldBe (width - lead).coerceAtLeast(0)
                val widths = edges.zipWithNext { a, b -> b - a }
                (widths.max() - widths.min()) shouldBeLessThanOrEqual 1
            }
        }

    @Test
    fun `rows fill a bounded height or are square`() {
        MonthGridGeometry.rowHeight(null, 30, 5, 50, 40) shouldBe 50
        MonthGridGeometry.rowHeight(null, 30, 5, 20, 40) shouldBe 40
        MonthGridGeometry.rowHeight(330, 30, 5, 50, 40) shouldBe 60
        MonthGridGeometry.rowHeight(100, 30, 5, 50, 40) shouldBe 40
        MonthGridGeometry.rowHeight(10, 30, 5, 50, 0) shouldBe 0
    }

    @Test
    fun `body slots put the week number first in each row`() {
        val edges = MonthGridGeometry.columnEdges(382, 32, 7)
        MonthGridGeometry.bodySlot(0, 8, true, edges) shouldBe (0 to 32)
        MonthGridGeometry.bodySlot(1, 8, true, edges) shouldBe (32 to 50)
        MonthGridGeometry.bodySlot(9, 8, true, edges) shouldBe (32 to 50)
        MonthGridGeometry.bodySlot(15, 8, true, edges) shouldBe (332 to 50)
        val plain = MonthGridGeometry.columnEdges(350, 0, 7)
        MonthGridGeometry.bodySlot(10, 7, false, plain) shouldBe (150 to 50)
    }

    @Test
    fun `day tone precedence`() {
        DayTone.of(cell(selected = true, holiday = true, inMonth = false)) shouldBe DayTone.OUTSIDE_MONTH
        DayTone.of(cell(selected = true, holiday = true)) shouldBe DayTone.SELECTED
        DayTone.of(cell(holiday = true)) shouldBe DayTone.HOLIDAY
        DayTone.of(cell()) shouldBe DayTone.NORMAL
    }

    @Test
    fun `moon terminator follows the illuminated fraction`() {
        unitFraction(Float.NaN) shouldBe 0f
        unitFraction(-1f) shouldBe 0f
        unitFraction(2f) shouldBe 1f
        MoonGeometry.terminatorScale(0f) shouldBe 1f
        MoonGeometry.terminatorScale(0.25f) shouldBe (0.5f plusOrMinus 1e-6f)
        MoonGeometry.terminatorScale(0.5f) shouldBe 0f
        MoonGeometry.terminatorScale(1f) shouldBe 1f
        MoonGeometry.terminatorSweep(0.25f) shouldBe -180f
        MoonGeometry.terminatorSweep(0.5f) shouldBe 180f
        MoonGeometry.terminatorSweep(0.9f) shouldBe 180f
    }

    @Test
    fun `sun rises at the start edge and sets at the end edge`() {
        SunArcGeometry.isAboveHorizon(null) shouldBe false
        SunArcGeometry.isAboveHorizon(Float.NaN) shouldBe false
        SunArcGeometry.isAboveHorizon(-0.1f) shouldBe false
        SunArcGeometry.isAboveHorizon(0f) shouldBe true
        SunArcGeometry.isAboveHorizon(1f) shouldBe true
        SunArcGeometry.isAboveHorizon(1.1f) shouldBe false
        val rise = SunArcGeometry.sunPosition(0f, 200f, 100f, 80f, rtl = false)
        rise.x shouldBe (0f plusOrMinus 1e-3f)
        rise.y shouldBe (100f plusOrMinus 1e-3f)
        val noon = SunArcGeometry.sunPosition(0.5f, 200f, 100f, 80f, rtl = false)
        noon.x shouldBe (100f plusOrMinus 1e-3f)
        noon.y shouldBe (20f plusOrMinus 1e-3f)
        SunArcGeometry.sunPosition(1f, 200f, 100f, 80f, rtl = false).x shouldBe (200f plusOrMinus 1e-3f)
        SunArcGeometry.sunPosition(0f, 200f, 100f, 80f, rtl = true).x shouldBe (200f plusOrMinus 1e-3f)
        val morning = SunArcGeometry.sunPosition(0.25f, 200f, 100f, 80f, rtl = false)
        SunArcGeometry.sunPosition(0.25f, 200f, 100f, 80f, rtl = true).x shouldBe (200f - morning.x plusOrMinus 1e-3f)
        SunArcGeometry.travelledArc(0.5f, rtl = false) shouldBe (180f to 90f)
        SunArcGeometry.travelledArc(0.5f, rtl = true) shouldBe (0f to -90f)
    }

    @Test
    fun `progress ring sweeps clockwise in LTR and counter-clockwise in RTL`() {
        ProgressRingGeometry.START_ANGLE shouldBe -90f
        ProgressRingGeometry.sweep(0.25f, rtl = false) shouldBe 90f
        ProgressRingGeometry.sweep(0.25f, rtl = true) shouldBe -90f
        ProgressRingGeometry.sweep(2f, rtl = false) shouldBe 360f
        ProgressRingGeometry.sweep(Float.NaN, rtl = false) shouldBe 0f
    }
}
