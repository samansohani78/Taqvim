/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

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
 * T-1101 screenshots: athan settings with Fajr 10 minutes early and Maghrib on, light/dark × LTR (English) / RTL
 * (Persian); the RTL light sample also shows the exact alarm warning. Recorded to
 * `src/test/screenshots/athan_<sample>/`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AthanSettingsScreenshotTest(
    private val sample: String,
    private val environment: ScreenshotEnvironment,
) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureAthanSettings() {
        val rtl = environment.layoutDirection == LayoutDirection.Rtl
        val language = if (rtl) LocationFixtures.persian else LocationFixtures.english
        val inputs =
            AthanScreenInputs(
                data = AthanSettingsData(language, AthanFixtures.someOn),
                exactAlarmsAllowed = sample != "blocked",
                previewing = false,
                soundUnreadable = false,
                volumeDraft = null,
            )
        val state = AthanStateMapper.state(inputs)
        composeRule.captureScreenshot("athan_$sample", environment) {
            LocationTestTheme(rtl = rtl, dark = environment.theme.isDark) {
                AthanSettingsScreen(state, AthanSettingsActions())
            }
        }
    }

    companion object {
        private fun environment(
            theme: ScreenshotTheme,
            direction: LayoutDirection,
        ) = ScreenshotEnvironment(theme = theme, layoutDirection = direction)

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun parameters(): List<Array<Any>> =
            listOf(
                arrayOf<Any>("prayers", environment(ScreenshotTheme.LIGHT, LayoutDirection.Ltr)),
                arrayOf<Any>("prayers", environment(ScreenshotTheme.DARK, LayoutDirection.Rtl)),
                arrayOf<Any>("blocked", environment(ScreenshotTheme.LIGHT, LayoutDirection.Rtl)),
                arrayOf<Any>("prayers", environment(ScreenshotTheme.DARK, LayoutDirection.Ltr)),
            )
    }
}
