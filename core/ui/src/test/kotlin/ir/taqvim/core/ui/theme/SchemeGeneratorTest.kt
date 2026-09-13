/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.theme

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class SchemeGeneratorTest {
    private val black = 0xFF000000.toInt()
    private val white = 0xFFFFFFFF.toInt()

    @Test
    fun `palettes follow the seed hue`() {
        val seedHue = ColorMath.toLch(SchemeGenerator.DEFAULT_SEED).hue
        val palettes = SchemeGenerator.palettes(SchemeGenerator.DEFAULT_SEED)

        palettes.keys shouldBe PaletteKey.entries.toSet()
        palettes.getValue(PaletteKey.PRIMARY).hue shouldBe seedHue
        palettes.getValue(PaletteKey.NEUTRAL).hue shouldBe seedHue
        palettes.getValue(PaletteKey.TERTIARY).hue shouldBe ((seedHue + 60) % 360 plusOrMinus 1e-9)
        palettes.getValue(PaletteKey.ERROR).hue shouldBe 40.0
        // A grey seed still gets a colorful primary palette.
        SchemeGenerator.palettes(0xFF808080.toInt()).getValue(PaletteKey.PRIMARY).chroma shouldBe 36.0
    }

    @Test
    fun `tones follow the role table`() {
        val light = SchemeGenerator.generate(SchemeGenerator.DEFAULT_SEED, dark = false)
        val dark = SchemeGenerator.generate(SchemeGenerator.DEFAULT_SEED, dark = true)
        val high = SchemeGenerator.generate(SchemeGenerator.DEFAULT_SEED, dark = true, contrast = ContrastLevel.HIGH)

        ColorMath.toLch(light[ColorRole.PRIMARY]).lightness shouldBe (40.0 plusOrMinus 0.5)
        ColorMath.toLch(dark[ColorRole.PRIMARY]).lightness shouldBe (80.0 plusOrMinus 0.5)
        ColorMath.toLch(high[ColorRole.PRIMARY]).lightness shouldBe (90.0 plusOrMinus 0.5)
        light[ColorRole.ON_PRIMARY] shouldBe white
        light[ColorRole.SCRIM] shouldBe black
        high[ColorRole.ON_PRIMARY] shouldBe black
        ColorRole.PRIMARY.tone(dark = false, ContrastLevel.HIGH) shouldBe 25
    }

    @Test
    fun `black schemes draw on pure black and differ from dark schemes only in surfaces`() {
        val dark = SchemeGenerator.generate(SchemeGenerator.DEFAULT_SEED, dark = true)
        val blackScheme = SchemeGenerator.generate(SchemeGenerator.DEFAULT_SEED, dark = false, black = true)

        listOf(ColorRole.BACKGROUND, ColorRole.SURFACE, ColorRole.SURFACE_DIM, ColorRole.SURFACE_CONTAINER_LOWEST)
            .forEach { blackScheme[it] shouldBe black }
        blackScheme[ColorRole.SURFACE_CONTAINER] shouldNotBe dark[ColorRole.SURFACE_CONTAINER]
        blackScheme[ColorRole.PRIMARY] shouldBe dark[ColorRole.PRIMARY]
        blackScheme[ColorRole.ON_SURFACE] shouldBe dark[ColorRole.ON_SURFACE]
    }

    @Test
    fun `a scheme needs one color per role`() {
        shouldThrow<IllegalArgumentException> { SchemeColors(listOf(black)) }
        TonalPalette(0.0, 0.0).tone(100) shouldBe white
    }
}
