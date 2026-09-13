/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.theme

import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import ir.taqvim.core.testing.PropertyTesting
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class ColorMathTest {
    private val white = 0xFFFFFFFF.toInt()
    private val black = 0xFF000000.toInt()

    private fun channelDistance(
        first: Int,
        second: Int,
    ): Int = listOf(16, 8, 0).maxOf { shift -> abs((first shr shift and 0xFF) - (second shr shift and 0xFF)) }

    @Test
    fun `WCAG luminance and contrast of reference colors`() {
        ColorMath.relativeLuminance(white) shouldBe (1.0 plusOrMinus 1e-4)
        ColorMath.relativeLuminance(black) shouldBe 0.0
        ColorMath.contrastRatio(white, black) shouldBe (21.0 plusOrMinus 1e-3)
        ColorMath.contrastRatio(black, white) shouldBe (21.0 plusOrMinus 1e-3)
        // #767676 is the lightest grey that meets 4.5:1 on white.
        ColorMath.contrastRatio(0xFF767676.toInt(), white) shouldBe (4.54 plusOrMinus 0.01)
        ColorMath.contrastRatio(0xFF777777.toInt(), white) shouldBeLessThan 4.5
        // A very dark channel uses the linear segment of the sRGB transfer function.
        ColorMath.relativeLuminance(0xFF0A0000.toInt()) shouldBe (0.2126 * (10 / 255.0) / 12.92 plusOrMinus 1e-9)
    }

    @Test
    fun `CIE LCh of neutral and primary colors`() {
        ColorMath.toLch(white).lightness shouldBe (100.0 plusOrMinus 0.01)
        ColorMath.toLch(white).chroma shouldBe (0.0 plusOrMinus 0.05)
        ColorMath.toLch(black).lightness shouldBe (0.0 plusOrMinus 1e-9)
        ColorMath.toLch(0xFF777777.toInt()).lightness shouldBe (50.0 plusOrMinus 0.5)
        ColorMath.toLch(0xFFFF0000.toInt()).lightness shouldBe (53.2 plusOrMinus 0.2)
        ColorMath.toLch(0xFFFF0000.toInt()).hue shouldBe (40.0 plusOrMinus 0.5)
        ColorMath.toLch(0xFF0000FF.toInt()).hue shouldBe (306.3 plusOrMinus 0.5)
        ColorMath.toLch(0xFF010101.toInt()).lightness shouldBe (0.27 plusOrMinus 0.01)
    }

    @Test
    fun `sRGB colors survive a round trip through LCh`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.int()) { value ->
                val argb = value or 0xFF000000.toInt()
                channelDistance(ColorMath.fromLch(ColorMath.toLch(argb)), argb) shouldBeLessThanOrEqual 1
            }
        }

    @Test
    fun `out-of-gamut requests keep lightness and hue and lose chroma`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.int(1..99), Arb.int(0..359)) { tone, hue ->
                val color = ColorMath.fromLch(Lch(tone.toDouble(), 250.0, hue.toDouble()))
                val lch = ColorMath.toLch(color)
                lch.lightness shouldBe (tone.toDouble() plusOrMinus 0.5)
                lch.chroma shouldBeLessThan 250.0
            }
        }

    @Test
    fun `lightness outside 0 to 100 is clamped`() {
        ColorMath.fromLch(Lch(-5.0, 0.0, 0.0)) shouldBe black
        ColorMath.fromLch(Lch(120.0, 0.0, 0.0)) shouldBe white
    }
}
