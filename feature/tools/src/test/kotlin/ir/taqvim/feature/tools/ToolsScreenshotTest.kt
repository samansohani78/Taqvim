/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import ir.taqvim.core.uitesting.ScreenshotEnvironment
import ir.taqvim.core.uitesting.ScreenshotMatrix
import ir.taqvim.core.uitesting.ScreenshotTheme
import ir.taqvim.core.uitesting.captureScreenshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * T-1400 screenshots: the converter in light and dark × LTR (English) and RTL (Persian), and one environment each for
 * the distance, duration, time-zone and QR tools. Recorded to `src/test/screenshots/tools_<sample>/`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ToolsScreenshotTest(
    private val sample: String,
    private val environment: ScreenshotEnvironment,
) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureTools() {
        val rtl = environment.layoutDirection == LayoutDirection.Rtl
        val settings = ToolsFixtures.settings(if (rtl) "fa" else "en")
        val state =
            when (sample) {
                "converter" -> {
                    ToolsFixtures.state(ToolsTab.CONVERTER, settings = settings)
                }

                "distance" -> {
                    val inputs = ToolsInputs(distanceFrom = "1405/1/1", distanceTo = "1405/6/22")
                    ToolsFixtures.state(ToolsTab.DISTANCE, inputs, settings)
                }

                "duration" -> {
                    ToolsFixtures.state(ToolsTab.DURATION, ToolsInputs(duration = "1d 2h + 30m"), settings)
                }

                "zones" -> {
                    ToolsFixtures.state(ToolsTab.TIME_ZONES, settings = settings)
                }

                else -> {
                    ToolsFixtures.state(ToolsTab.QR, ToolsInputs(qr = "https://example.com"), settings)
                }
            }
        composeRule.captureScreenshot("tools_$sample", environment) {
            ToolsTestTheme(rtl = rtl, dark = environment.theme.isDark) { ToolsScreen(state, ToolsActions()) }
        }
    }

    companion object {
        private fun environment(
            theme: ScreenshotTheme,
            direction: LayoutDirection,
        ) = ScreenshotEnvironment(theme = theme, layoutDirection = direction)

        private val LIGHT_LTR = environment(ScreenshotTheme.LIGHT, LayoutDirection.Ltr)
        private val DARK_RTL = environment(ScreenshotTheme.DARK, LayoutDirection.Rtl)
        private val LIGHT_RTL = environment(ScreenshotTheme.LIGHT, LayoutDirection.Rtl)
        private val CONVERTER_ENVIRONMENTS =
            listOf(LIGHT_LTR, environment(ScreenshotTheme.DARK, LayoutDirection.Ltr), LIGHT_RTL, DARK_RTL)

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun parameters(): List<Array<Any>> =
            CONVERTER_ENVIRONMENTS.map { arrayOf<Any>("converter", it) } +
                listOf(
                    arrayOf<Any>("distance", LIGHT_RTL),
                    arrayOf<Any>("duration", DARK_RTL),
                    arrayOf<Any>("zones", LIGHT_LTR),
                    arrayOf<Any>("qr", LIGHT_LTR),
                ) +
                // T-1701: every screen again in Persian RTL and English LTR at font scale 2.0.
                listOf("converter", "distance", "duration", "zones", "qr").flatMap { sample ->
                    ScreenshotMatrix.largeText().map { arrayOf<Any>(sample, it) }
                }
    }
}
