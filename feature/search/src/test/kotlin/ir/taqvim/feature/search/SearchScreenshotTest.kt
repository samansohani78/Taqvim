/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.search

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
 * T-804 screenshots: grouped results with a "go to date" row in light and dark × LTR (English) and RTL (Persian),
 * recent searches, and no results. Recorded to `src/test/screenshots/search_<sample>/`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SearchScreenshotTest(
    private val sample: String,
    private val environment: ScreenshotEnvironment,
) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureSearch() {
        val rtl = environment.layoutDirection == LayoutDirection.Rtl
        val settings = if (rtl) PERSIAN_FA else GREGORIAN_EN
        val query = if (rtl) "ت" else "r"
        val state =
            when (sample) {
                "results" -> SearchUiState(query, content = sampleResults(settings, query))
                "recent" -> SearchUiState(recent = SAMPLE_RECENT)
                else -> SearchUiState("zzzz", content = SearchContent.NoResults)
            }
        composeRule.captureScreenshot("search_$sample", environment) {
            SearchTestTheme(rtl = rtl, dark = environment.theme.isDark) { SearchScreen(state, {}) }
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
                environment(ScreenshotTheme.LIGHT, LayoutDirection.Ltr),
                environment(ScreenshotTheme.DARK, LayoutDirection.Ltr),
                environment(ScreenshotTheme.LIGHT, LayoutDirection.Rtl),
                environment(ScreenshotTheme.DARK, LayoutDirection.Rtl),
            ).map { arrayOf<Any>("results", it) } +
                listOf(
                    arrayOf<Any>("recent", environment(ScreenshotTheme.LIGHT, LayoutDirection.Rtl)),
                    arrayOf<Any>("no_results", environment(ScreenshotTheme.DARK, LayoutDirection.Ltr)),
                ) +
                // T-1701: every screen again in Persian RTL and English LTR at font scale 2.0.
                listOf("results", "recent").flatMap { sample ->
                    ScreenshotMatrix.largeText().map { arrayOf<Any>(sample, it) }
                }
    }
}
