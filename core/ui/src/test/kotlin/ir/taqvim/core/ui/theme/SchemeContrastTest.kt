/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.theme

import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import ir.taqvim.core.testing.PropertyTesting
import ir.taqvim.core.ui.theme.ColorRole.BACKGROUND
import ir.taqvim.core.ui.theme.ColorRole.ERROR
import ir.taqvim.core.ui.theme.ColorRole.ERROR_CONTAINER
import ir.taqvim.core.ui.theme.ColorRole.INVERSE_ON_SURFACE
import ir.taqvim.core.ui.theme.ColorRole.INVERSE_PRIMARY
import ir.taqvim.core.ui.theme.ColorRole.INVERSE_SURFACE
import ir.taqvim.core.ui.theme.ColorRole.ON_BACKGROUND
import ir.taqvim.core.ui.theme.ColorRole.ON_ERROR
import ir.taqvim.core.ui.theme.ColorRole.ON_ERROR_CONTAINER
import ir.taqvim.core.ui.theme.ColorRole.ON_PRIMARY
import ir.taqvim.core.ui.theme.ColorRole.ON_PRIMARY_CONTAINER
import ir.taqvim.core.ui.theme.ColorRole.ON_PRIMARY_FIXED
import ir.taqvim.core.ui.theme.ColorRole.ON_PRIMARY_FIXED_VARIANT
import ir.taqvim.core.ui.theme.ColorRole.ON_SECONDARY
import ir.taqvim.core.ui.theme.ColorRole.ON_SECONDARY_CONTAINER
import ir.taqvim.core.ui.theme.ColorRole.ON_SECONDARY_FIXED
import ir.taqvim.core.ui.theme.ColorRole.ON_SECONDARY_FIXED_VARIANT
import ir.taqvim.core.ui.theme.ColorRole.ON_SURFACE
import ir.taqvim.core.ui.theme.ColorRole.ON_SURFACE_VARIANT
import ir.taqvim.core.ui.theme.ColorRole.ON_TERTIARY
import ir.taqvim.core.ui.theme.ColorRole.ON_TERTIARY_CONTAINER
import ir.taqvim.core.ui.theme.ColorRole.ON_TERTIARY_FIXED
import ir.taqvim.core.ui.theme.ColorRole.ON_TERTIARY_FIXED_VARIANT
import ir.taqvim.core.ui.theme.ColorRole.PRIMARY
import ir.taqvim.core.ui.theme.ColorRole.PRIMARY_CONTAINER
import ir.taqvim.core.ui.theme.ColorRole.PRIMARY_FIXED
import ir.taqvim.core.ui.theme.ColorRole.PRIMARY_FIXED_DIM
import ir.taqvim.core.ui.theme.ColorRole.SECONDARY
import ir.taqvim.core.ui.theme.ColorRole.SECONDARY_CONTAINER
import ir.taqvim.core.ui.theme.ColorRole.SECONDARY_FIXED
import ir.taqvim.core.ui.theme.ColorRole.SECONDARY_FIXED_DIM
import ir.taqvim.core.ui.theme.ColorRole.SURFACE
import ir.taqvim.core.ui.theme.ColorRole.SURFACE_BRIGHT
import ir.taqvim.core.ui.theme.ColorRole.SURFACE_CONTAINER
import ir.taqvim.core.ui.theme.ColorRole.SURFACE_CONTAINER_HIGH
import ir.taqvim.core.ui.theme.ColorRole.SURFACE_CONTAINER_HIGHEST
import ir.taqvim.core.ui.theme.ColorRole.SURFACE_CONTAINER_LOW
import ir.taqvim.core.ui.theme.ColorRole.SURFACE_CONTAINER_LOWEST
import ir.taqvim.core.ui.theme.ColorRole.SURFACE_DIM
import ir.taqvim.core.ui.theme.ColorRole.SURFACE_VARIANT
import ir.taqvim.core.ui.theme.ColorRole.TERTIARY
import ir.taqvim.core.ui.theme.ColorRole.TERTIARY_CONTAINER
import ir.taqvim.core.ui.theme.ColorRole.TERTIARY_FIXED
import ir.taqvim.core.ui.theme.ColorRole.TERTIARY_FIXED_DIM
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/**
 * T-700: every text role keeps WCAG 2.2 contrast — 4.5:1 (§1.4.3) at standard contrast and 7:1 (§1.4.6) at high
 * contrast — against each background it is drawn on, for any seed color and in light, dark and black schemes.
 */
class SchemeContrastTest {
    private val surfaces =
        listOf(
            BACKGROUND,
            SURFACE,
            SURFACE_BRIGHT,
            SURFACE_DIM,
            SURFACE_CONTAINER_LOWEST,
            SURFACE_CONTAINER_LOW,
            SURFACE_CONTAINER,
            SURFACE_CONTAINER_HIGH,
            SURFACE_CONTAINER_HIGHEST,
        )

    /** Foreground text role → the backgrounds it is placed on. */
    private val textPairs: List<Pair<ColorRole, ColorRole>> =
        listOf(ON_SURFACE, ON_SURFACE_VARIANT, PRIMARY, SECONDARY, TERTIARY, ERROR).flatMap { text ->
            surfaces.map { text to it }
        } +
            listOf(
                ON_BACKGROUND to BACKGROUND,
                ON_SURFACE_VARIANT to SURFACE_VARIANT,
                ON_PRIMARY to PRIMARY,
                ON_PRIMARY_CONTAINER to PRIMARY_CONTAINER,
                ON_SECONDARY to SECONDARY,
                ON_SECONDARY_CONTAINER to SECONDARY_CONTAINER,
                ON_TERTIARY to TERTIARY,
                ON_TERTIARY_CONTAINER to TERTIARY_CONTAINER,
                ON_ERROR to ERROR,
                ON_ERROR_CONTAINER to ERROR_CONTAINER,
                INVERSE_ON_SURFACE to INVERSE_SURFACE,
                INVERSE_PRIMARY to INVERSE_SURFACE,
                ON_PRIMARY_FIXED to PRIMARY_FIXED,
                ON_PRIMARY_FIXED to PRIMARY_FIXED_DIM,
                ON_PRIMARY_FIXED_VARIANT to PRIMARY_FIXED,
                ON_PRIMARY_FIXED_VARIANT to PRIMARY_FIXED_DIM,
                ON_SECONDARY_FIXED to SECONDARY_FIXED,
                ON_SECONDARY_FIXED to SECONDARY_FIXED_DIM,
                ON_SECONDARY_FIXED_VARIANT to SECONDARY_FIXED,
                ON_SECONDARY_FIXED_VARIANT to SECONDARY_FIXED_DIM,
                ON_TERTIARY_FIXED to TERTIARY_FIXED,
                ON_TERTIARY_FIXED to TERTIARY_FIXED_DIM,
                ON_TERTIARY_FIXED_VARIANT to TERTIARY_FIXED,
                ON_TERTIARY_FIXED_VARIANT to TERTIARY_FIXED_DIM,
            )

    private enum class Variant(
        val dark: Boolean,
        val black: Boolean,
    ) {
        LIGHT(false, false),
        DARK(true, false),
        BLACK(true, true),
    }

    private fun minimum(contrast: ContrastLevel): Double =
        when (contrast) {
            ContrastLevel.STANDARD -> 4.5
            ContrastLevel.HIGH -> 7.0
        }

    private fun assertTextContrast(
        seed: Int,
        variant: Variant,
        contrast: ContrastLevel,
    ) {
        val scheme = SchemeGenerator.generate(seed, variant.dark, variant.black, contrast)
        textPairs.forEach { (text, background) ->
            withClue("seed=${seed.toUInt().toString(16)} $variant $contrast $text on $background") {
                ColorMath.contrastRatio(scheme[text], scheme[background]) shouldBeGreaterThanOrEqual minimum(contrast)
            }
        }
    }

    @Test
    fun `text roles meet the contrast level for any seed`(): Unit =
        runBlocking {
            checkAll(
                PropertyTesting.iterations,
                Arb.int(),
                Arb.element(Variant.entries),
                Arb.element(ContrastLevel.entries),
            ) { value, variant, contrast ->
                assertTextContrast(value or 0xFF000000.toInt(), variant, contrast)
            }
        }

    @Test
    fun `text roles meet the contrast level for the default seed and extreme seeds`() {
        val seeds = listOf(SchemeGenerator.DEFAULT_SEED, 0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFF00.toInt())
        val variants = Variant.entries.flatMap { variant -> ContrastLevel.entries.map { variant to it } }
        seeds.forEach { seed ->
            variants.forEach { (variant, contrast) -> assertTextContrast(seed, variant, contrast) }
        }
    }
}
