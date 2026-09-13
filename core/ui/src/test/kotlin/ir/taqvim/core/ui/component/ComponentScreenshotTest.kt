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
import ir.taqvim.core.uitesting.ScreenshotEnvironment
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
        composeRule.captureScreenshot("component_$component", environment) {
            TestTheme(
                rtl = environment.layoutDirection == LayoutDirection.Rtl,
                dark = environment.theme.isDark,
            ) {
                TaqvimBackground(Modifier.fillMaxSize()) { sample() }
            }
        }
    }

    companion object {
        private fun environment(
            theme: ScreenshotTheme,
            direction: LayoutDirection,
            fontScale: Float,
        ) = ScreenshotEnvironment(theme = theme, layoutDirection = direction, fontScale = fontScale)

        private val ENVIRONMENTS =
            listOf(
                environment(ScreenshotTheme.LIGHT, LayoutDirection.Ltr, 1f),
                environment(ScreenshotTheme.DARK, LayoutDirection.Ltr, 1.3f),
                environment(ScreenshotTheme.LIGHT, LayoutDirection.Rtl, 2f),
                environment(ScreenshotTheme.DARK, LayoutDirection.Rtl, 1f),
            )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun parameters(): List<Array<Any>> =
            COMPONENT_SAMPLES.keys.flatMap { name -> ENVIRONMENTS.map { arrayOf<Any>(name, it) } }
    }
}
