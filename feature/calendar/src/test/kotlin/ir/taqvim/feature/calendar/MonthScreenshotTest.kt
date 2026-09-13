/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.ui.component.ScreenSurface
import ir.taqvim.core.ui.component.TopBar
import ir.taqvim.core.uitesting.ScreenshotEnvironment
import ir.taqvim.core.uitesting.ScreenshotTheme
import ir.taqvim.core.uitesting.captureScreenshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * T-801 screenshots: the twelve months of 1405 in Persian (light, RTL) and English (light, LTR), and in Nepali (dark,
 * LTR) the twelve Gregorian months from April 2026, because the Bikram Sambat calendar is not available yet (T-105).
 * Events are synthetic: a holiday on the first of each month, a personal event on the 10th, three sources on the 15th.
 * Recorded to `src/test/screenshots/calendar_month_<language>_<month>/`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MonthScreenshotTest(
    private val languageCode: String,
    private val month: Int,
) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureMonth() {
        val sample = MonthSample.of(languageCode)
        val offset = month - 1
        val screen = "calendar_month_${languageCode}_${month.toString().padStart(2, '0')}"
        composeRule.captureScreenshot(screen, sample.environment) {
            CalendarTestTheme(
                rtl = sample.environment.layoutDirection == LayoutDirection.Rtl,
                dark = sample.environment.theme.isDark,
            ) {
                val builder = rememberMonthPageBuilder(sample.settings)
                val page =
                    remember(builder) {
                        builder.build(offset, sample.firstDay, sample.firstDay, sample.events(offset))
                    }
                ScreenSurface(topBar = { TopBar(page.heading.title, subtitle = page.heading.subtitle) }) { padding ->
                    MonthPageView(page, onAction = {}, modifier = Modifier.padding(padding))
                }
            }
        }
    }

    /** A language's calendars, its first shown month ([firstDay] is that month's first day) and its environment. */
    private class MonthSample(
        val settings: CalendarSettings,
        val firstDay: Jdn,
        val environment: ScreenshotEnvironment,
    ) {
        private val calendars = CalendarCalendars(settings)
        private val weekend = requireNotNull(LanguageTable.forCode(settings.languageCode)).weekend

        fun events(offset: Int): List<CalendarDay> {
            val monthStart = calendars.monthStartAt(firstDay, offset)
            return MonthLayout.gridDays(monthStart, settings.weekStart).map { day ->
                val dayOfMonth = (day - monthStart).toInt() + 1
                CalendarDay(day, dayOfMonth == 1, day.weekday() in weekend, eventsOn(dayOfMonth))
            }
        }

        private fun eventsOn(dayOfMonth: Int): List<DayEventItem> =
            when (dayOfMonth) {
                1 -> listOf(item(DayEventKind.OFFICIAL, holiday = true))
                PERSONAL_DAY -> listOf(item(DayEventKind.PERSONAL))
                BUSY_DAY -> BUSY_KINDS.map { item(it) }
                else -> emptyList()
            }

        private fun item(
            kind: DayEventKind,
            holiday: Boolean = false,
        ): DayEventItem = DayEventItem("synthetic-$kind", kind, "Synthetic", holiday)

        companion object {
            const val PERSONAL_DAY = 10
            const val BUSY_DAY = 15
            val BUSY_KINDS = listOf(DayEventKind.OFFICIAL, DayEventKind.DEVICE, DayEventKind.SUBSCRIPTION)

            fun of(code: String): MonthSample {
                val language = requireNotNull(LanguageTable.forCode(code))
                return when (code) {
                    "fa" -> {
                        MonthSample(
                            CalendarSettings(
                                language.calendars,
                                language.weekStart,
                                IslamicVariant.IRAN_OFFICIAL,
                                code,
                            ),
                            gregorian(2026, 3, 21),
                            ScreenshotEnvironment(layoutDirection = LayoutDirection.Rtl),
                        )
                    }

                    "en" -> {
                        MonthSample(
                            CalendarSettings(
                                listOf(CalendarSystem.PERSIAN, CalendarSystem.GREGORIAN),
                                language.weekStart,
                                IslamicVariant.UMM_AL_QURA,
                                code,
                            ),
                            gregorian(2026, 3, 21),
                            ScreenshotEnvironment(layoutDirection = LayoutDirection.Ltr),
                        )
                    }

                    else -> {
                        MonthSample(
                            CalendarSettings(language.calendars, language.weekStart, IslamicVariant.UMM_AL_QURA, code),
                            gregorian(2026, 4, 1),
                            ScreenshotEnvironment(theme = ScreenshotTheme.DARK, localeTag = code),
                        )
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun parameters(): List<Array<Any>> =
            listOf("fa", "en", "ne").flatMap { code -> (1..12).map { arrayOf<Any>(code, it) } }
    }
}
