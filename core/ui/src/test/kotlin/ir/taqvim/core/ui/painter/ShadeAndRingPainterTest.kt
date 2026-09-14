/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.painter

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.core.ui.painter.PainterSamples.countNear
import ir.taqvim.core.ui.painter.PainterSamples.near
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** T-1211 map shade and T-1212 progress ring bitmaps, checked on pixels. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShadeAndRingPainterTest {
    private val light = PainterSamples.palette(dark = false)
    private val transparent = PainterSamples.palette(dark = false, opaque = false)
    private val environment = PainterEnvironment(density = 2f)

    @Test
    fun `the shade darkens only its dark cells and keeps the marker on top`() {
        // Two columns: the left half clear, the right half fully dark; padding 8 px.
        val shade = MapShade(columns = 2, rows = 1, darkness = listOf(0f, 1f))
        val marker = NormalizedPoint(0.75f, 0.5f)
        val bitmap =
            MapThumbnailPainter(light, environment).paint(MapThumbnailModel(emptyList(), marker, shade), 200, 100)

        assertTrue(near(bitmap.getPixel(40, 20), light.surfaceHighest))
        assertFalse(near(bitmap.getPixel(160, 20), light.surfaceHighest))
        assertTrue(Color.red(bitmap.getPixel(160, 20)) < Color.red(bitmap.getPixel(40, 20)))
        assertTrue(near(bitmap.getPixel(146, 50), light.primary))
    }

    @Test
    fun `shades need one value per cell`() {
        assertThrows(IllegalArgumentException::class.java) { MapShade(2, 2, listOf(0f)) }
        assertThrows(IllegalArgumentException::class.java) { MapShade(0, 1, emptyList()) }
    }

    @Test
    fun `the ring fills its share clockwise in LTR and counter-clockwise in RTL`() {
        fun ring(
            progress: Float,
            rtl: Boolean = false,
        ) = ProgressRingBitmapPainter(transparent, PainterEnvironment(density = 2f, rtl = rtl))
            .paint(ProgressRingBitmapModel(progress), 100, 100)

        val quarter = ring(0.25f)
        val right = 50 until 100
        val left = 0 until 50
        assertTrue(quarter.countNear(light.primary, right) > quarter.countNear(light.primary, left))
        val quarterRtl = ring(0.25f, rtl = true)
        assertTrue(quarterRtl.countNear(light.primary, left) > quarterRtl.countNear(light.primary, right))
        assertEquals(0, ring(0f).countNear(light.primary))
        assertTrue(ring(0f).countNear(light.outlineVariant) > 0)
        assertEquals(0, Color.alpha(ring(1f).getPixel(50, 50)))
        assertTrue(ring(1f).countNear(light.primary, left) > 0)
    }
}
