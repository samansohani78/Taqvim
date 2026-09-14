/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.i18n.DateFormatter
import ir.taqvim.core.i18n.DateStyle
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.Jdn
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** T-801 UI: month swipes and their titles, selection, long press → event editor, week number → timeline. */
@RunWith(RobolectricTestRunner::class)
class CalendarScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val english = requireNotNull(LanguageTable.forCode("en"))
    private val persianMonths = requireNotNull(english.monthNames.persian)

    /** 21 Farvardin 1405. */
    private val today = gregorian(2026, 4, 10)
    private val settings = PERSIAN_FIRST.copy(languageCode = "en")

    private fun show(
        settings: CalendarSettings = this.settings,
        navigation: CalendarNavigation = CalendarNavigation(),
    ) {
        val days = FakeDaySource()
        val viewModel =
            CalendarViewModel(
                FakeSettingsSource(settings),
                FakeTodaySource(today),
                days,
                days,
                SearchEventsUseCase(FakeSearchSource(emptyMap())),
                FakePlaceSource(null),
                FakeNowSource(TEST_NOW),
                FakeDisplayStore(),
            )
        composeRule.setContent {
            CalendarTestTheme { CalendarRoute(navigation = navigation, viewModel = viewModel) }
        }
    }

    private fun awaitNode(matcher: SemanticsMatcher) {
        composeRule.waitUntil(TIMEOUT_MILLIS) { composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun title(monthsFromFarvardin1405: Int): String {
        val index = Math.floorMod(monthsFromFarvardin1405, MONTHS)
        val year = 1405 + Math.floorDiv(monthsFromFarvardin1405, MONTHS)
        return "${persianMonths[index]} $year"
    }

    private fun longDate(day: Jdn): String =
        DateFormatter.format(PersianCalendarSystem.fromJdn(day), day.weekday(), english, DateStyle.LONG)

    @Test
    fun `swiping twelve months either way shows each month's title`() {
        show()
        awaitNode(hasText(title(0)))

        (1..MONTHS).forEach { month ->
            composeRule.onNodeWithTag(MONTH_PAGER_TAG).performTouchInput { swipeLeft() }
            awaitNode(hasText(title(month)))
        }
        (MONTHS - 1 downTo -MONTHS).forEach { month ->
            composeRule.onNodeWithTag(MONTH_PAGER_TAG).performTouchInput { swipeRight() }
            awaitNode(hasText(title(month)))
        }
    }

    @Test
    fun `tapping a day selects it`() {
        show()
        val day = gregorian(2026, 4, 15)
        awaitNode(hasContentDescription(longDate(day), substring = true))

        composeRule.onAllNodes(hasContentDescription(longDate(day), substring = true)).onFirst().performClick()
        awaitNode(hasContentDescription(longDate(day), substring = true) and isSelected())
    }

    @Test
    fun `long-pressing a day opens the event editor on that day`() {
        var editorDay: Jdn? = null
        show(navigation = CalendarNavigation(onOpenEventEditor = { editorDay = it }))
        val day = gregorian(2026, 4, 15)
        awaitNode(hasContentDescription(longDate(day), substring = true))

        composeRule.onAllNodes(hasContentDescription(longDate(day), substring = true)).onFirst().performTouchInput {
            longClick()
        }
        composeRule.waitUntil(TIMEOUT_MILLIS) { editorDay == day }
    }

    @Test
    fun `tapping a week number opens the timeline at the first day of the week`() {
        var timelineDay: Jdn? = null
        show(settings.copy(showWeekNumbers = true), CalendarNavigation(onOpenTimeline = { timelineDay = it }))
        // Week 1 also labels the last row of the pre-composed Esfand page; week 2 is only on Farvardin's page.
        val secondWeek = hasContentDescription("Week 2")
        awaitNode(secondWeek)

        composeRule.onAllNodes(secondWeek).onFirst().performClick()
        composeRule.waitUntil(TIMEOUT_MILLIS) { timelineDay == gregorian(2026, 3, 28) }
    }

    @Test
    fun `a progress indicator is shown until today and the preferences load`() {
        composeRule.setContent { CalendarTestTheme { CalendarScreen(CalendarUiState(), onAction = {}) } }

        composeRule
            .onNodeWithContentDescription(RuntimeEnvironment.getApplication().getString(R.string.calendar_loading))
            .assertExists()
    }

    private companion object {
        const val MONTHS = 12
        const val TIMEOUT_MILLIS = 10_000L
    }
}
