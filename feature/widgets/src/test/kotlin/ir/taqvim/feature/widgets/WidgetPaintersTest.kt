/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.toArgb
import androidx.glance.material3.ColorProviders
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import ir.taqvim.core.ui.painter.MoonBitmapModel
import java.util.Locale
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** T-1200 bitmap seam over the T-702 painters: theme colors, widget scale, direction and a painted bitmap. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetPaintersTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val scheme = lightColorScheme()

    @Test
    fun thePaletteComesFromTheGlanceThemeWithATransparentBackground() {
        val palette = WidgetPainters.palette(context, ColorProviders(scheme))

        palette.background shouldBe 0
        palette.primary shouldBe scheme.primary.toArgb()
        palette.onSurface shouldBe scheme.onSurface.toArgb()
        palette.surfaceHighest shouldBe scheme.surfaceVariant.toArgb()
        palette.outlineVariant shouldBe scheme.outline.toArgb()
        palette.land shouldBe scheme.secondaryContainer.toArgb()
        palette.error shouldBe scheme.error.toArgb()
        WidgetPainters.palette(context, ColorProviders(darkColorScheme())).onSurface shouldBe
            darkColorScheme().onSurface.toArgb()
    }

    @Test
    fun theEnvironmentFollowsDensityWidgetScaleAndDirection() {
        val resources = context.resources
        val large = WidgetPainters.environment(context, WidgetConfig(scalePercent = 150))
        large.density shouldBe resources.displayMetrics.density
        large.fontScale shouldBe (resources.configuration.fontScale * 1.5f plusOrMinus 0.0001f)
        large.rtl.shouldBeFalse()

        val persian = Configuration(resources.configuration).apply { setLayoutDirection(Locale.forLanguageTag("fa")) }
        WidgetPainters.environment(context.createConfigurationContext(persian), WidgetConfig()).rtl.shouldBeTrue()
    }

    @Test
    fun paintersDrawBitmapsAtTheWidgetPixelSize() {
        val painters = WidgetPainters.of(context, ColorProviders(scheme), WidgetConfig())
        val moon = painters.moon.paint(MoonBitmapModel(illuminatedFraction = 1f, waxing = true), 64, 48)

        moon.width shouldBe 64
        moon.height shouldBe 48
        Color.alpha(moon.getPixel(0, 0)) shouldBe 0
        Color.alpha(moon.getPixel(32, 24)) shouldBeGreaterThan 0
        (painters.month === painters.month).shouldBeTrue()
        (painters.sunArc === painters.sunArc).shouldBeTrue()
    }
}
