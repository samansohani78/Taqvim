/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import ir.taqvim.core.ui.theme.TaqvimBackground
import ir.taqvim.core.uitesting.LayoutOptions
import ir.taqvim.core.uitesting.LayoutRule
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
 * T-701 screenshot matrix: every component gallery in four environments that together cover light and dark, LTR and
 * RTL, and font scales 1.0, 1.3 and 2.0. Recorded to `src/test/screenshots/component_<name>/`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ComponentScreenshotTest(
    private val component: String,
    private val environment: ScreenshotEnvironment,
) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureComponent() {
        val sample = COMPONENT_SAMPLES.getValue(component)
        composeRule.captureScreenshot("component_$component", environment, layout = layoutOptions()) {
            TestTheme(
                rtl = environment.layoutDirection == LayoutDirection.Rtl,
                dark = environment.theme.isDark,
            ) {
                TaqvimBackground(Modifier.fillMaxSize()) { sample() }
            }
        }
    }

    /**
     * Accepted T-1701 exception: an event chip keeps one line and ellipsizes long event titles; the full title is its
     * spoken label and the event page shows it whole.
     */
    private fun layoutOptions() =
        LayoutOptions(ignored = { component == "event_chip" && it.rule == LayoutRule.TEXT_TRUNCATED })

    companion object {
        private fun environment(
            theme: ScreenshotTheme,
            direction: LayoutDirection,
            fontScale: Float,
        ) = ScreenshotEnvironment(theme = theme, layoutDirection = direction, fontScale = fontScale)

        // T-1701: the large-text pair (Persian RTL and English LTR at font scale 2.0) completes the matrix.
        private val ENVIRONMENTS =
            listOf(
                environment(ScreenshotTheme.LIGHT, LayoutDirection.Ltr, 1f),
                environment(ScreenshotTheme.DARK, LayoutDirection.Ltr, 1.3f),
                environment(ScreenshotTheme.DARK, LayoutDirection.Rtl, 1f),
            ) + ScreenshotMatrix.largeText()

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun parameters(): List<Array<Any>> =
            COMPONENT_SAMPLES.keys.flatMap { name -> ENVIRONMENTS.map { arrayOf<Any>(name, it) } }
    }
}
