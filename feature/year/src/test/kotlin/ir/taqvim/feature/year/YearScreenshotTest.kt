/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.year

import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.uitesting.ScreenshotEnvironment
import ir.taqvim.core.uitesting.ScreenshotMatrix
import ir.taqvim.core.uitesting.ScreenshotTheme
import ir.taqvim.core.uitesting.captureScreenshot
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * T-805 screenshots: 1405 in Persian (RTL) and the Gregorian year 2026 in English (LTR), light and dark, plus the
 * English year selection. Holidays are synthetic (the first day of every third month); weekends follow the language.
 * Recorded to `src/test/screenshots/year_<sample>/`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class YearScreenshotTest(
    private val sample: String,
    private val fontScale: Float,
) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureYear() {
        val persian = sample.startsWith("fa")
        val dark = sample.endsWith("dark")
        val picking = sample.endsWith("picker")
        val settings = if (persian) PERSIAN_FIRST else GREGORIAN_FIRST
        val environment =
            ScreenshotEnvironment(
                theme = if (dark) ScreenshotTheme.DARK else ScreenshotTheme.LIGHT,
                layoutDirection = if (persian) LayoutDirection.Rtl else LayoutDirection.Ltr,
                fontScale = fontScale,
            )
        val content = content(settings, if (persian) PERSIAN_TODAY else GREGORIAN_TODAY).copy(isPickingYear = picking)
        composeRule.captureScreenshot("year_$sample", environment) {
            YearTestTheme(rtl = persian, dark = dark) {
                val builder = rememberYearPageBuilder(settings)
                val page = remember(builder) { builder.build(0, content.year, content.today, content.days) }
                YearFrame(content, builder, onAction = {}) { modifier ->
                    if (picking) {
                        YearPicker(content.year, builder::number, onAction = {}, modifier = modifier)
                    } else {
                        YearPageView(page, content.columns, onAction = {}, modifier = modifier)
                    }
                }
            }
        }
    }

    private fun content(
        settings: YearSettings,
        today: Jdn,
    ): YearContent {
        val calendars = YearCalendars(settings)
        val year = calendars.yearOf(0, today)
        val holidays = calendars.monthStarts(0, year).filterIndexed { month, _ -> month % HOLIDAY_EVERY_MONTHS == 0 }
        val weekend = requireNotNull(LanguageTable.forCode(settings.languageCode)).weekend
        val days = calendars.yearDays(0, year).map { YearDay(it, it in holidays, it.weekday() in weekend) }
        return YearContent(
            today = today,
            calendars = calendars.systems.toImmutableList(),
            calendarIndex = 0,
            anchorDay = today,
            year = year,
            columns = YearZoom.DEFAULT_COLUMNS,
            isPickingYear = false,
            weekStart = settings.weekStart,
            islamicVariant = settings.islamicVariant,
            languageCode = settings.languageCode,
            days = days.toImmutableList(),
        )
    }

    companion object {
        private const val HOLIDAY_EVERY_MONTHS = 3

        /** 21 Farvardin 1405. */
        private val PERSIAN_TODAY = gregorian(2026, 4, 10)
        private val GREGORIAN_TODAY = gregorian(2026, 9, 14)

        private val GREGORIAN_FIRST =
            YearSettings(
                calendars = listOf(CalendarSystem.GREGORIAN, CalendarSystem.PERSIAN),
                weekStart = requireNotNull(LanguageTable.forCode("en")).weekStart,
                islamicVariant = IslamicVariant.UMM_AL_QURA,
                languageCode = "en",
            )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun parameters(): List<Array<Any>> =
            listOf("fa_light", "fa_dark", "en_light", "en_dark", "en_picker").map { arrayOf<Any>(it, 1f) } +
                // T-1701: again at font scale 2.0.
                listOf("fa_light", "en_light").map { arrayOf<Any>(it, ScreenshotMatrix.LARGE_FONT_SCALE) }
    }
}
