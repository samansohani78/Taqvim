/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.times

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import ir.taqvim.core.uitesting.ScreenshotEnvironment
import ir.taqvim.core.uitesting.ScreenshotTheme
import ir.taqvim.core.uitesting.captureScreenshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * T-1100 screenshots: today's times in light and dark × LTR (English) and RTL (Persian), plus the expanded list of
 * another day, a polar day and the missing-place state. Recorded to `src/test/screenshots/times_<sample>/`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TimesScreenshotTest(
    private val sample: String,
    private val environment: ScreenshotEnvironment,
) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureTimes() {
        val rtl = environment.layoutDirection == LayoutDirection.Rtl
        val settings = TimesFixtures.tehran(if (rtl) "fa" else "en")
        val noon = TimesFixtures.at("2026-06-21T12:00", settings)
        val state =
            when (sample) {
                "day" -> TimesStateMapper.map(settings, 0, noon, expanded = false)
                "expanded" -> TimesStateMapper.map(settings, 1, noon, expanded = true)
                "polar" -> TimesStateMapper.map(TimesFixtures.tromso(), 0, noon, expanded = false)
                else -> TimesUiState(TimesContent.NoLocation)
            }
        composeRule.captureScreenshot("times_$sample", environment) {
            TimesTestTheme(rtl = rtl, dark = environment.theme.isDark) { TimesScreen(state, TimesActions()) }
        }
    }

    companion object {
        private fun environment(
            theme: ScreenshotTheme,
            direction: LayoutDirection,
        ) = ScreenshotEnvironment(theme = theme, layoutDirection = direction)

        private val LIGHT_LTR = environment(ScreenshotTheme.LIGHT, LayoutDirection.Ltr)
        private val DARK_RTL = environment(ScreenshotTheme.DARK, LayoutDirection.Rtl)
        private val DAY_ENVIRONMENTS =
            listOf(
                LIGHT_LTR,
                environment(ScreenshotTheme.DARK, LayoutDirection.Ltr),
                environment(ScreenshotTheme.LIGHT, LayoutDirection.Rtl),
                DARK_RTL,
            )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun parameters(): List<Array<Any>> =
            DAY_ENVIRONMENTS.map { arrayOf<Any>("day", it) } +
                listOf(
                    arrayOf<Any>("expanded", DARK_RTL),
                    arrayOf<Any>("polar", LIGHT_LTR),
                    arrayOf<Any>("no_location", LIGHT_LTR),
                )
    }
}
