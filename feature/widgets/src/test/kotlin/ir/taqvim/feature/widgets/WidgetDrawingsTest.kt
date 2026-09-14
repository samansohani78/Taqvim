/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.material3.ColorProviders
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.Weekday
import kotlin.time.TimeSource
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.datetime.LocalDate
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** T-1206 and T-1209: painter models of the drawn widgets, their bitmap sizes and the T-702 painters drawing them. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetDrawingsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val holiday = LocalDate(2026, 9, 15)
    private val month =
        WidgetCalendarBuilder.month(
            WidgetCalendarInputs(
                LocalDate(2026, 9, 13).toJdn(),
                requireNotNull(LanguageTable.forCode("en")),
                PersianCalendarSystem,
                GregorianCalendarSystem,
                Weekday.SATURDAY,
            ),
            0,
        ) { jdn ->
            if (jdn == holiday.toJdn()) {
                WidgetDayFacts(true, false, listOf(WidgetEventLine("Dentist", isHoliday = false)))
            } else {
                WidgetDayFacts.NONE
            }
        }
    private val painters = WidgetPainters.of(context, ColorProviders(lightColorScheme()), WidgetConfig())

    @Test
    fun theMonthModelFollowsTheWidgetsParts() {
        val all = WidgetDrawings.monthModel(month, WidgetConfig.defaultFor(WidgetKind.MONTH_BITMAP), ComposeColor.Red)
        all.weekdayLabels shouldBe month.weekdayLabels
        all.cells.size shouldBe WidgetCalendarBuilder.GRID_DAYS
        val marked = all.cells[month.days.indexOfFirst { it.date == holiday }]
        marked.isHoliday shouldBe true
        marked.indicators shouldBe listOf(ComposeColor.Red)
        marked.secondaryLabels shouldBe listOf("15")
        all.cells.count { it.isToday } shouldBe 1
        all.cells.count { it.inCurrentMonth } shouldBe 31

        val plain = WidgetDrawings.monthModel(month, WidgetConfig(contents = persistentSetOf()), ComposeColor.Red)
        plain.cells.none { it.isHoliday } shouldBe true
        plain.cells.flatMap { it.indicators }.shouldBeEmpty()
        plain.cells.flatMap { it.secondaryLabels }.shouldBeEmpty()
    }

    @Test
    fun bitmapsFillTheWidgetInsideTheFrameLeavingRoomForText() {
        WidgetDrawings.pixels(DpSize(250.dp, 250.dp), 2f, 22f) shouldBe (468 to 424)
        WidgetDrawings.pixels(DpSize(10.dp, 10.dp), 3f, 22f) shouldBe (1 to 1)
    }

    @Test
    fun thePaintersDrawTheMonthAndTheSunPath() {
        val (width, height) = WidgetDrawings.pixels(DpSize(250.dp, 250.dp), 2f, 22f)
        val grid =
            painters.month.paint(
                WidgetDrawings.monthModel(month, WidgetConfig(), ComposeColor.Red),
                width,
                height,
            )
        grid.width shouldBe width
        grid.height shouldBe height
        painted(grid) shouldBeGreaterThan 0

        val sun = WidgetSun("06:38", "19:12", 0.5f)
        val arc = painters.sunArc.paint(WidgetDrawings.sunModel(sun, "Sunrise 06:38, sunset 19:12"), 200, 120)
        painted(arc) shouldBeGreaterThan 0
    }

    @Test
    fun theMonthBitmapRendersQuickly() {
        val model = WidgetDrawings.monthModel(month, WidgetConfig.defaultFor(WidgetKind.MONTH_BITMAP), ComposeColor.Red)
        repeat(WARM_UP) { painters.month.paint(model, BITMAP_PX, BITMAP_PX) }
        val best =
            (1..RUNS).minOf {
                val mark = TimeSource.Monotonic.markNow()
                painters.month.paint(model, BITMAP_PX, BITMAP_PX)
                mark.elapsedNow().inWholeMilliseconds
            }
        // Best of several runs, so a busy machine does not fail it; the budget is generous for Robolectric's graphics.
        best shouldBeLessThan BUDGET_MS
    }

    private fun painted(bitmap: Bitmap): Int {
        var count = 0
        for (x in 0 until bitmap.width step STRIDE) {
            for (y in 0 until bitmap.height step STRIDE) {
                if (Color.alpha(bitmap.getPixel(x, y)) > 0) count++
            }
        }
        return count
    }

    private companion object {
        const val WARM_UP = 2
        const val RUNS = 5
        const val BITMAP_PX = 600
        const val BUDGET_MS = 250L
        const val STRIDE = 4
    }
}
