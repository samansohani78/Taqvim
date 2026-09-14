/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import ir.taqvim.core.uitesting.ScreenshotDevice
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
 * T-803/T-806 screenshots: the calendar screen with its toolbar on a phone (compact, stacked), a fold (medium, two
 * panes) and a tablet (expanded, two panes), in English light LTR and Persian dark RTL. The day is Nowruz 1405 with
 * synthetic events. Recorded to `src/test/screenshots/calendar_adaptive/`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CalendarAdaptiveScreenshotTest(
    private val environment: ScreenshotEnvironment,
    @Suppress("unused") private val name: String,
) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureCalendar() {
        val content = DayDetailsSamples.content(environment.localeTag)
        composeRule.captureScreenshot("calendar_adaptive", environment) {
            CalendarTestTheme(
                rtl = environment.layoutDirection == LayoutDirection.Rtl,
                dark = environment.theme.isDark,
            ) {
                val builder = rememberMonthPageBuilder(content.settings())
                val page = remember(builder) { builder.build(0, content.today, content.selectedDay, null) }
                CalendarScaffold(content, builder, onAction = {}, isTabletop = false) { paneModifier ->
                    MonthPageView(page, onAction = {}, modifier = paneModifier)
                }
            }
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{1}")
        fun parameters(): List<Array<Any>> =
            ScreenshotDevice.entries
                .flatMap { device ->
                    listOf(
                        ScreenshotEnvironment(device = device),
                        ScreenshotEnvironment(
                            device = device,
                            theme = ScreenshotTheme.DARK,
                            layoutDirection = LayoutDirection.Rtl,
                        ),
                    )
                }.plus(ScreenshotMatrix.largeText())
                .map { arrayOf<Any>(it, it.id) }
    }
}
