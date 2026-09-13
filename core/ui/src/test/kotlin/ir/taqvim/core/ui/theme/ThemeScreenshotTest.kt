/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ui.theme

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import ir.taqvim.core.i18n.TextDirection
import ir.taqvim.core.uitesting.ScreenshotEnvironment
import ir.taqvim.core.uitesting.ScreenshotTheme
import ir.taqvim.core.uitesting.captureScreenshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/** T-700 screenshot matrix: six themes × LTR/RTL, recorded to `src/test/screenshots/theme_<name>/`. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ThemeScreenshotTest(
    private val case: Case,
) {
    /** One theme of the matrix in one layout direction. */
    data class Case(
        val name: String,
        val settings: ThemeSettings,
        val systemTheme: ScreenshotTheme,
        val direction: LayoutDirection,
    ) {
        override fun toString(): String = "${name}_${direction.name.lowercase()}"
    }

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureTheme() {
        val environment = ScreenshotEnvironment(theme = case.systemTheme, layoutDirection = case.direction)
        val textDirection = if (case.direction == LayoutDirection.Rtl) TextDirection.RTL else TextDirection.LTR
        composeRule.captureScreenshot("theme_${case.name}", environment) {
            TaqvimTheme(case.settings, textDirection) { ThemeSample() }
        }
    }

    companion object {
        private val THEMES =
            listOf(
                Triple("light", ThemeSettings(mode = ThemeMode.LIGHT, dynamicColor = false), ScreenshotTheme.LIGHT),
                Triple("dark", ThemeSettings(mode = ThemeMode.DARK, dynamicColor = false), ScreenshotTheme.DARK),
                Triple("black", ThemeSettings(mode = ThemeMode.BLACK, dynamicColor = false), ScreenshotTheme.BLACK),
                Triple("dynamic", ThemeSettings(mode = ThemeMode.SYSTEM), ScreenshotTheme.LIGHT),
                Triple(
                    "custom_seed",
                    ThemeSettings(
                        dynamicColor = false,
                        seedColor = 0xFF6A4FB3.toInt(),
                        gradient = true,
                        boldText = true,
                    ),
                    ScreenshotTheme.DARK,
                ),
                Triple(
                    "high_contrast",
                    ThemeSettings(mode = ThemeMode.LIGHT, dynamicColor = false, highContrast = true),
                    ScreenshotTheme.LIGHT,
                ),
            )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun parameters(): List<Array<Any>> =
            THEMES.flatMap { (name, settings, system) ->
                listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).map { arrayOf<Any>(Case(name, settings, system, it)) }
            }
    }
}
