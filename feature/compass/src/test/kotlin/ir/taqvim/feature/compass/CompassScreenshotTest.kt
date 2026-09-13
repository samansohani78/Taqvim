/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.compass

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.uitesting.ScreenshotEnvironment
import ir.taqvim.core.uitesting.ScreenshotTheme
import ir.taqvim.core.uitesting.captureScreenshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * T-1302/T-1303 screenshots: the compass in light and dark × LTR (English) and RTL (Persian) with the Sun's path, the
 * compass without a place, the flat and upright level and the ruler. Recorded to `src/test/screenshots/<sample>/`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CompassScreenshotTest(
    private val sample: String,
    private val environment: ScreenshotEnvironment,
) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun capture() {
        val rtl = environment.layoutDirection == LayoutDirection.Rtl
        val language = if (rtl) "fa" else "en"
        val numerals = if (rtl) NumeralSystem.PERSIAN else NumeralSystem.LATIN
        composeRule.captureScreenshot(sample, environment) {
            CompassTestTheme(rtl = rtl, dark = environment.theme.isDark) {
                when (sample) {
                    "compass_dial" -> {
                        CompassScreen(
                            CompassFixtures.dial(language, heading = 200.0, showSunPath = true),
                            CompassActions(),
                            animatePath = false,
                        )
                    }

                    "compass_no_place" -> {
                        CompassScreen(
                            CompassFixtures.dial(language, place = null),
                            CompassActions(),
                            animatePath = false,
                        )
                    }

                    "level_flat" -> {
                        LevelScreen(level(Vector3(1.0, -0.6, 9.7), numerals), LevelActions())
                    }

                    "level_upright" -> {
                        LevelScreen(level(Vector3(0.0, 9.8, 0.3), numerals), LevelActions())
                    }

                    else -> {
                        LevelScreen(LevelUiState(tab = LevelTab.RULER, numerals = numerals), LevelActions())
                    }
                }
            }
        }
    }

    private fun level(
        gravity: Vector3,
        numerals: NumeralSystem,
    ): LevelUiState {
        val orientation = requireNotNull(LevelMath.classify(gravity))
        val content = LevelStateMapper.content(LevelSample.Measured(gravity, orientation), LevelCalibration(), numerals)
        return LevelUiState(content = content, numerals = numerals)
    }

    companion object {
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
            ).map { arrayOf<Any>("compass_dial", it) } +
                listOf(
                    arrayOf<Any>("compass_no_place", LIGHT_LTR),
                    arrayOf<Any>("level_flat", LIGHT_LTR),
                    arrayOf<Any>("level_upright", DARK_RTL),
                    arrayOf<Any>("level_ruler", LIGHT_LTR),
                )
    }
}
