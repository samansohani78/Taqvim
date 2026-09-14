/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.agenda

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
 * T-901 screenshots: the agenda around today in light and dark × LTR (English, Gregorian first) and RTL (Persian,
 * Persian first), every day of the month list, and the loading state. Recorded to
 * `src/test/screenshots/agenda_<sample>/`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AgendaScreenshotTest(
    private val sample: String,
    private val environment: ScreenshotEnvironment,
) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureAgenda() {
        val rtl = environment.layoutDirection == LayoutDirection.Rtl
        val settings = if (rtl) PERSIAN_FA else GREGORIAN_EN
        val state =
            when (sample) {
                "events" -> AgendaUiState(sampleContent(settings))
                "all_days" -> AgendaUiState(sampleContent(settings, AgendaMode.MONTH_LIST))
                else -> AgendaUiState()
            }
        composeRule.captureScreenshot("agenda_$sample", environment) {
            AgendaTestTheme(rtl = rtl, dark = environment.theme.isDark) { AgendaScreen(state, AgendaScreenActions()) }
        }
    }

    companion object {
        private fun environment(
            theme: ScreenshotTheme,
            direction: LayoutDirection,
        ) = ScreenshotEnvironment(theme = theme, layoutDirection = direction)

        private val LIGHT_LTR = environment(ScreenshotTheme.LIGHT, LayoutDirection.Ltr)

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun parameters(): List<Array<Any>> =
            listOf(
                LIGHT_LTR,
                environment(ScreenshotTheme.DARK, LayoutDirection.Ltr),
                environment(ScreenshotTheme.LIGHT, LayoutDirection.Rtl),
                environment(ScreenshotTheme.DARK, LayoutDirection.Rtl),
            ).map { arrayOf<Any>("events", it) } +
                listOf(
                    arrayOf<Any>("all_days", environment(ScreenshotTheme.LIGHT, LayoutDirection.Rtl)),
                    arrayOf<Any>("loading", LIGHT_LTR),
                ) +
                // T-1701: every screen again in Persian RTL and English LTR at font scale 2.0.
                listOf("events", "all_days").flatMap { sample ->
                    ScreenshotMatrix.largeText().map { arrayOf<Any>(sample, it) }
                }
    }
}
