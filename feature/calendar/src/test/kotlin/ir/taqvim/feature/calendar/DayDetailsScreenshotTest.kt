/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ir.taqvim.core.ui.component.ScreenSurface
import ir.taqvim.core.ui.component.TopBar
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
 * T-802 screenshots: each day-details tab for Nowruz 1405 in light and dark, English (LTR) and Persian (RTL). Events
 * are synthetic (the official event shows its source tooltip); times are for Tehran at 10:30. Recorded to
 * `src/test/screenshots/calendar_day_<tab>/`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DayDetailsScreenshotTest(
    private val tab: DayDetailsTab,
    private val environment: ScreenshotEnvironment,
    @Suppress("unused") private val name: String,
) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureDayDetails() {
        val sourceEvent = DayDetailsSamples.official.takeIf { tab == DayDetailsTab.EVENTS }
        val content = DayDetailsSamples.content(environment.localeTag, tab, sourceEvent = sourceEvent)
        composeRule.captureScreenshot("calendar_day_${tab.name.lowercase()}", environment) {
            CalendarTestTheme(
                rtl = environment.layoutDirection == LayoutDirection.Rtl,
                dark = environment.theme.isDark,
            ) {
                // The app shows the panel in a scrolling column (CalendarScaffold), so it is captured in one too.
                ScreenSurface(topBar = { TopBar(stringResource(R.string.calendar_title)) }) { padding ->
                    Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
                        DayDetailsPanel(content, onAction = {}, modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{2}")
        fun parameters(): List<Array<Any>> {
            val environments =
                listOf(
                    ScreenshotEnvironment(),
                    ScreenshotEnvironment(theme = ScreenshotTheme.DARK),
                    ScreenshotEnvironment(layoutDirection = LayoutDirection.Rtl),
                    ScreenshotEnvironment(theme = ScreenshotTheme.DARK, layoutDirection = LayoutDirection.Rtl),
                ) + ScreenshotMatrix.largeText()
            return DayDetailsTab.entries.flatMap { tab ->
                environments.map { arrayOf<Any>(tab, it, "${tab.name.lowercase()}_${it.id}") }
            }
        }
    }
}
