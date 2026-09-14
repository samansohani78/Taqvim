/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.about

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import ir.taqvim.core.uitesting.ScreenshotEnvironment
import ir.taqvim.core.uitesting.ScreenshotTheme
import ir.taqvim.core.uitesting.captureScreenshot
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * T-1504 screenshots: the About home in light and dark × LTR (English) and RTL (Persian), plus the license list, the
 * data sources and the diagnostics page. Recorded to `src/test/screenshots/about_<sample>/`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AboutScreenshotTest(
    private val sample: String,
    private val environment: ScreenshotEnvironment,
) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureAbout() {
        val rtl = environment.layoutDirection == LayoutDirection.Rtl
        val state =
            when (sample) {
                "home" -> {
                    AboutUiState(info = AboutFixtures.info.copy(languageCode = environment.localeTag))
                }

                "licenses" -> {
                    AboutUiState(
                        page = AboutPage.LICENSES,
                        canGoBack = true,
                        licenses = LicensesContent.Ready(AboutFixtures.catalog.groups(), 3),
                    )
                }

                "faq" -> {
                    AboutUiState(
                        page = AboutPage.FAQ,
                        canGoBack = true,
                        faq = FaqContent(expanded = persistentSetOf(FaqEntry.ISLAMIC_DATE)),
                    )
                }

                "diagnostics" -> {
                    AboutUiState(
                        page = AboutPage.DIAGNOSTICS,
                        canGoBack = true,
                        diagnostics = diagnosticsContent(AboutFixtures.entries, DiagnosticLevel.DEBUG),
                    )
                }

                else -> {
                    AboutUiState(page = AboutPage.DATA_SOURCES, canGoBack = true)
                }
            }
        composeRule.captureScreenshot("about_$sample", environment) {
            AboutTestTheme(rtl = rtl, dark = environment.theme.isDark) { AboutScreen(state, AboutActions()) }
        }
    }

    companion object {
        private fun environment(
            theme: ScreenshotTheme,
            direction: LayoutDirection,
        ) = ScreenshotEnvironment(theme = theme, layoutDirection = direction)

        private val LIGHT_LTR = environment(ScreenshotTheme.LIGHT, LayoutDirection.Ltr)
        private val DARK_LTR = environment(ScreenshotTheme.DARK, LayoutDirection.Ltr)
        private val LIGHT_RTL = environment(ScreenshotTheme.LIGHT, LayoutDirection.Rtl)
        private val DARK_RTL = environment(ScreenshotTheme.DARK, LayoutDirection.Rtl)

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun parameters(): List<Array<Any>> =
            listOf(LIGHT_LTR, DARK_LTR, LIGHT_RTL, DARK_RTL).map { arrayOf<Any>("home", it) } +
                listOf(LIGHT_LTR, DARK_LTR, LIGHT_RTL, DARK_RTL).map { arrayOf<Any>("faq", it) } +
                listOf(
                    arrayOf<Any>("licenses", LIGHT_RTL),
                    arrayOf<Any>("data_sources", DARK_RTL),
                    arrayOf<Any>("diagnostics", DARK_LTR),
                )
    }
}
