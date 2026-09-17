/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.painter

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.core.testing.TimingTest
import ir.taqvim.core.ui.component.DayCellModel
import ir.taqvim.core.ui.component.MonthGridModel
import ir.taqvim.core.ui.component.SunArcModel
import ir.taqvim.core.ui.painter.PainterSamples.countNear
import ir.taqvim.core.ui.painter.PainterSamples.near
import kotlin.system.measureNanoTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** T-702: what the painters draw, checked on pixels, plus the month rendering budget. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PainterRenderingTest {
    private val light = PainterSamples.palette(dark = false)
    private val transparent = PainterSamples.palette(dark = false, opaque = false)

    private fun environment(rtl: Boolean = false) = PainterEnvironment(density = 2f, rtl = rtl)

    @Test
    fun `the selected day mirrors to the other half in RTL`() {
        // Day 26 of Farvardin 1405 is in the fifth of seven columns.
        val model = PainterSamples.farvardin1405()
        val ltr = MonthBitmapPainter(light, environment()).paint(model, WIDTH, HEIGHT)
        val rtl = MonthBitmapPainter(light, environment(rtl = true)).paint(model, WIDTH, HEIGHT)
        val left = 0 until WIDTH / 2
        val right = WIDTH / 2 until WIDTH
        assertTrue(ltr.countNear(light.primary, right) > ltr.countNear(light.primary, left) + MIN_DISC_PIXELS)
        assertTrue(rtl.countNear(light.primary, left) > rtl.countNear(light.primary, right) + MIN_DISC_PIXELS)
        assertTrue(ltr.countNear(light.error) > 0)
        assertEquals(light.background, ltr.getPixel(0, 0))
    }

    @Test
    fun `transparent palettes leave the background untouched and every cell variant draws`() {
        val cells =
            listOf(
                DayCellModel("", "blank"),
                DayCellModel("9", "today outside", isToday = true, inCurrentMonth = false),
                DayCellModel("8", "busy", indicators = List(5) { androidx.compose.ui.graphics.Color.Red }),
                DayCellModel("7", "holiday", secondaryLabels = listOf("1", "2"), isHoliday = true),
            )
        val model = MonthGridModel(listOf("a", "b"), cells)
        val bitmap = MonthBitmapPainter(transparent, environment(rtl = true)).paint(model, 200, 200)
        assertEquals(0, Color.alpha(bitmap.getPixel(0, 0)))
        assertTrue(bitmap.countNear(Color.RED) > 0)
        val weeks = MonthBitmapPainter(light, environment()).paint(PainterSamples.march2026(), WIDTH, HEIGHT)
        assertTrue(weeks.countNear(light.outline) > 0)
    }

    @Test
    fun `bitmap sizes and environments are validated`() {
        val painter = MoonBitmapPainter(light, environment())
        val moon = MoonBitmapModel(0.5f, waxing = true)
        assertThrows(IllegalArgumentException::class.java) { painter.paint(moon, 0, 10) }
        assertThrows(IllegalArgumentException::class.java) { painter.paint(moon, 10, 0) }
        assertThrows(IllegalArgumentException::class.java) { PainterEnvironment(density = 0f) }
        assertThrows(IllegalArgumentException::class.java) { PainterEnvironment(density = 1f, fontScale = 0f) }
        assertEquals(20f, PainterEnvironment(density = 2f).px(10f))
        assertEquals(30f, PainterEnvironment(density = 2f, fontScale = 1.5f).textPx(10f))
    }

    @Test
    fun `moon phases light the correct side`() {
        fun sides(model: MoonBitmapModel): Pair<Int, Int> {
            val bitmap = MoonBitmapPainter(transparent, environment()).paint(model, MOON, MOON)
            return bitmap.getPixel(MOON / 2 - SIDE, MOON / 2) to bitmap.getPixel(MOON / 2 + SIDE, MOON / 2)
        }
        val shadow = light.surfaceHighest
        val lit = light.tertiary
        assertSides(sides(MoonBitmapModel(0f, waxing = true)), shadow, shadow)
        assertSides(sides(MoonBitmapModel(1f, waxing = true)), lit, lit)
        assertSides(sides(MoonBitmapModel(0.5f, waxing = true)), shadow, lit)
        assertSides(sides(MoonBitmapModel(0.5f, waxing = false)), lit, shadow)
        assertSides(sides(MoonBitmapModel(0.5f, waxing = true, rotationDegrees = 180f)), lit, shadow)
        val corner = MoonBitmapPainter(light, environment()).paint(MoonBitmapModel(1f, waxing = true), MOON, MOON)
        assertEquals(light.background, corner.getPixel(0, 0))
        val clear = MoonBitmapPainter(transparent, environment()).paint(MoonBitmapModel(1f, true), MOON, MOON)
        assertEquals(0, Color.alpha(clear.getPixel(0, 0)))
    }

    private fun assertSides(
        actual: Pair<Int, Int>,
        left: Int,
        right: Int,
    ) {
        assertTrue("left ${Integer.toHexString(actual.first)}", near(actual.first, left))
        assertTrue("right ${Integer.toHexString(actual.second)}", near(actual.second, right))
    }

    @Test
    fun `the sun is drawn on its side of the arc only while it is up`() {
        fun sun(
            progress: Float?,
            rtl: Boolean,
            palette: PainterPalette = light,
        ) = SunArcBitmapPainter(palette, environment(rtl))
            .paint(SunArcModel(progress, "06:10", "19:40", "sun"), 400, 200)
        val left = 0 until 200
        val right = 200 until 400
        assertEquals(0, sun(null, rtl = false).countNear(light.tertiary))
        assertEquals(0, sun(1.5f, rtl = false).countNear(light.tertiary))
        val morningLtr = sun(0.25f, rtl = false)
        assertTrue(morningLtr.countNear(light.tertiary, left) > morningLtr.countNear(light.tertiary, right))
        val morningRtl = sun(0.25f, rtl = true, palette = transparent)
        assertTrue(morningRtl.countNear(light.tertiary, right) > morningRtl.countNear(light.tertiary, left))
        assertTrue(morningLtr.countNear(light.onSurfaceVariant) > 0)
    }

    @Test
    fun `map thumbnails show water, land and the marker`() {
        val bitmap = MapThumbnailPainter(light, environment()).paint(PainterSamples.map(), 240, 160)
        // padding 8 px: x = 8 + f × 224, y = 8 + f × 144
        assertTrue(near(bitmap.getPixel(1, 1), light.surfaceHighest))
        assertTrue(near(bitmap.getPixel(131, 72), light.primary))
        assertTrue(near(bitmap.getPixel(86, 80), light.land))
        assertTrue(bitmap.countNear(light.outline) > 0)
        val empty = MapThumbnailPainter(light, environment()).paint(MapThumbnailModel(emptyList()), 40, 40)
        assertEquals(0, empty.countNear(light.primary))
    }

    @Test
    @Category(TimingTest::class)
    fun `a month bitmap renders within the 8 ms budget`() {
        val painter = MonthBitmapPainter(light, environment(rtl = true))
        val model = PainterSamples.farvardin1405()
        repeat(WARM_UP) { painter.paint(model, WIDTH, HEIGHT) }
        val best = (1..RUNS).minOf { measureNanoTime { painter.paint(model, WIDTH, HEIGHT) } }
        println("T-702 month bitmap ${WIDTH}x$HEIGHT best of $RUNS: ${best / NANOS_PER_MILLI} ms")
        assertTrue("best ${best / NANOS_PER_MILLI} ms", best < TimingTest.budget(BUDGET_NANOS))
    }

    private companion object {
        const val WIDTH = 720
        const val HEIGHT = 640
        const val MOON = 200
        const val SIDE = 50
        const val MIN_DISC_PIXELS = 200
        const val WARM_UP = 10
        const val RUNS = 20
        const val BUDGET_NANOS = 8_000_000L
        const val NANOS_PER_MILLI = 1_000_000.0
    }
}
