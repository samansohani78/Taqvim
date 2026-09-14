/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.astronomy

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import ir.taqvim.core.uitesting.ScreenshotEnvironment
import ir.taqvim.core.uitesting.ScreenshotMatrix
import ir.taqvim.core.uitesting.ScreenshotTheme
import ir.taqvim.core.uitesting.captureScreenshot
import kotlin.time.Duration.Companion.hours
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * T-1300 screenshots per mode: the Moon view in light and dark × LTR (English) and RTL (Persian), the Earth view in
 * light LTR and the Sun view in dark RTL, plus the missing-place state. Recorded to
 * `src/test/screenshots/astronomy_<sample>/`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AstronomyScreenshotTest(
    private val sample: String,
    private val environment: ScreenshotEnvironment,
) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureAstronomy() {
        val rtl = environment.layoutDirection == LayoutDirection.Rtl
        val settings = AstronomyFixtures.tehran(if (rtl) "fa" else "en")
        val instant = AstronomyFixtures.at("2026-08-28T21:30", settings)
        val sky = AstronomyFixtures.sky(settings, instant, isNow = false)
        val state =
            when (sample) {
                "earth" -> AstronomyUiState(AstronomyMode.EARTH, sky)
                "moon" -> AstronomyUiState(AstronomyMode.MOON, sky)
                "sun" -> AstronomyUiState(AstronomyMode.SUN, AstronomyFixtures.sky(settings, instant - NOON_SHIFT))
                else -> AstronomyUiState(content = AstronomyContent.NoLocation)
            }
        composeRule.captureScreenshot("astronomy_$sample", environment) {
            AstronomyTestTheme(rtl = rtl, dark = environment.theme.isDark) {
                AstronomyScreen(state, AstronomyActions())
            }
        }
    }

    companion object {
        private val NOON_SHIFT = 9.hours

        private fun environment(
            theme: ScreenshotTheme,
            direction: LayoutDirection,
        ) = ScreenshotEnvironment(theme = theme, layoutDirection = direction)

        private val LIGHT_LTR = environment(ScreenshotTheme.LIGHT, LayoutDirection.Ltr)
        private val DARK_RTL = environment(ScreenshotTheme.DARK, LayoutDirection.Rtl)

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun parameters(): List<Array<Any>> =
            listOf(
                LIGHT_LTR,
                environment(ScreenshotTheme.DARK, LayoutDirection.Ltr),
                environment(ScreenshotTheme.LIGHT, LayoutDirection.Rtl),
                DARK_RTL,
            ).map { arrayOf<Any>("moon", it) } +
                listOf(
                    arrayOf<Any>("earth", LIGHT_LTR),
                    arrayOf<Any>("sun", DARK_RTL),
                    arrayOf<Any>("no_location", LIGHT_LTR),
                ) +
                // T-1701: every screen again in Persian RTL and English LTR at font scale 2.0.
                listOf("moon", "earth", "sun").flatMap { sample ->
                    ScreenshotMatrix.largeText().map { arrayOf<Any>(sample, it) }
                }
    }
}
