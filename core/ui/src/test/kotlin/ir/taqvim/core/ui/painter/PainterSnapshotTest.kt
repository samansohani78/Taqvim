/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.painter

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import ir.taqvim.core.ui.component.SunArcModel
import ir.taqvim.core.uitesting.SCREENSHOT_DIRECTORY
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** T-702 bitmap snapshots, recorded to `src/test/screenshots/painter_<kind>/`. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PainterSnapshotTest {
    private val light = PainterSamples.palette(dark = false)
    private val dark = PainterSamples.palette(dark = true)

    private fun Bitmap.capture(name: String) {
        captureRoboImage("$SCREENSHOT_DIRECTORY/$name.png")
    }

    @Test
    fun monthPersianRtlLight() {
        MonthBitmapPainter(light, PainterEnvironment(DENSITY, rtl = true))
            .paint(PainterSamples.farvardin1405(), 720, 640)
            .capture("painter_month/fa_rtl_light")
    }

    @Test
    fun monthEnglishLtrDark() {
        MonthBitmapPainter(dark, PainterEnvironment(DENSITY))
            .paint(PainterSamples.march2026(), 720, 720)
            .capture("painter_month/en_ltr_dark")
    }

    @Test
    fun sunArcs() {
        val model = SunArcModel(0.35f, "06:10", "19:40", "sun")
        SunArcBitmapPainter(light, PainterEnvironment(DENSITY)).paint(model, 600, 240).capture("painter_sun/ltr_light")
        SunArcBitmapPainter(dark, PainterEnvironment(DENSITY, rtl = true))
            .paint(model.copy(progress = 0.7f), 600, 240)
            .capture("painter_sun/rtl_dark")
        SunArcBitmapPainter(light, PainterEnvironment(DENSITY))
            .paint(model.copy(progress = null), 600, 240)
            .capture("painter_sun/night_light")
    }

    @Test
    fun moonPhases() {
        val painter = MoonBitmapPainter(dark, PainterEnvironment(DENSITY))
        painter.paint(MoonBitmapModel(0f, waxing = true), MOON, MOON).capture("painter_moon/new")
        painter.paint(MoonBitmapModel(0.5f, waxing = true), MOON, MOON).capture("painter_moon/first_quarter")
        painter.paint(MoonBitmapModel(1f, waxing = true), MOON, MOON).capture("painter_moon/full")
        painter.paint(MoonBitmapModel(0.2f, waxing = false), MOON, MOON).capture("painter_moon/waning_crescent")
    }

    @Test
    fun mapThumbnail() {
        MapThumbnailPainter(light, PainterEnvironment(DENSITY))
            .paint(PainterSamples.map(), 480, 320)
            .capture("painter_map/light")
    }

    private companion object {
        const val DENSITY = 2f
        const val MOON = 240
    }
}
