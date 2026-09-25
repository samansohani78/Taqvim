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
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.i18n.DateFormatter
import ir.taqvim.core.i18n.DateStyle
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import kotlinx.coroutines.Dispatchers
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
                // The screen's state is computed off the main thread in production (BUG-2); Robolectric's clock is
                // virtual, so a real background dispatcher would never deliver within `waitUntil`'s timeout.
                calculationDispatcher = Dispatchers.Unconfined,
            )
        composeRule.setContent {
            CalendarTestTheme { CalendarRoute(navigation = navigation, viewModel = viewModel) }
        }
    }

    /**
     * Pages to the month [monthsFromFarvardin1405] months from today's, through the pager's own scroll-to-index
     * action rather than a swipe.
     *
     * The 35 flings this replaces were the only reason the test was load-sensitive: a fling has to settle before the
     * next assertion, and on a CPU-starved machine it does not, which failed CI (run 35909594358) with a 10 s
     * `ComposeTimeoutException` and made the test unable to judge the change it was guarding. One real swipe is kept,
     * in `swiping the pager moves to the next and previous month`, so the gesture path is still covered.
     */
    private fun goToMonth(monthsFromFarvardin1405: Int) {
        composeRule.onNodeWithTag(MONTH_PAGER_TAG).performScrollToIndex(MonthPages.pageOf(monthsFromFarvardin1405))
    }

    private fun awaitNode(
        matcher: SemanticsMatcher,
        timeoutMillis: Long = TIMEOUT_MILLIS,
    ) {
        composeRule.waitUntil(timeoutMillis) { composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
    }

    /**
     * Waits for the month [monthsFromFarvardin1405] months from today's to be shown: its title in the header **and**
     * a day cell of that month in the grid.
     *
     * The title alone is not enough. It comes from the screen's state, so it appears as soon as the pager reports the
     * new page, even if that page never builds its days — a fault planted in `MonthPageSlot` (days built only for the
     * current month) left the old assertion passing while eleven of twelve pages rendered empty.
     */
    private fun awaitMonthShown(monthsFromFarvardin1405: Int) {
        awaitNode(hasText(title(monthsFromFarvardin1405)))
        awaitNode(hasContentDescription(longDate(midMonth(monthsFromFarvardin1405)), substring = true))
    }

    /** The 15th of the month [monthsFromFarvardin1405] months from today's, a day every month's page shows. */
    private fun midMonth(monthsFromFarvardin1405: Int): Jdn {
        val index = Math.floorMod(monthsFromFarvardin1405, MONTHS)
        val year = 1405 + Math.floorDiv(monthsFromFarvardin1405, MONTHS)
        return PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, year, index + 1, 15))
    }

    private fun title(monthsFromFarvardin1405: Int): String {
        val index = Math.floorMod(monthsFromFarvardin1405, MONTHS)
        val year = 1405 + Math.floorDiv(monthsFromFarvardin1405, MONTHS)
        return "${persianMonths[index]} $year"
    }

    private fun longDate(day: Jdn): String =
        DateFormatter.format(PersianCalendarSystem.fromJdn(day), day.weekday(), english, DateStyle.LONG)

    @Test
    fun `paging twelve months either way shows each month's title`() {
        show()
        awaitNode(hasText(title(0)))

        (1..MONTHS).forEach { month -> goToMonth(month).also { awaitMonthShown(month) } }
        (MONTHS - 1 downTo -MONTHS).forEach { month -> goToMonth(month).also { awaitMonthShown(month) } }
    }

    @Test
    fun `swiping the pager moves to the next and previous month`() {
        show()
        awaitNode(hasText(title(0)))

        // A fling has to settle before the new month can be asserted, and on a CI runner that is dexing the rest of
        // the build it can take far longer than the deterministic cases need: this timed out at 10 s on main@7b12385
        // while every other case passed. The assertion is unchanged, so a page that never arrives still fails.
        composeRule.onNodeWithTag(MONTH_PAGER_TAG).performTouchInput { swipeLeft() }
        awaitNode(hasText(title(1)), GESTURE_TIMEOUT_MILLIS)
        composeRule.onNodeWithTag(MONTH_PAGER_TAG).performTouchInput { swipeRight() }
        awaitNode(hasText(title(0)), GESTURE_TIMEOUT_MILLIS)
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

        /** A settling fling needs more room than a state assertion; see the swipe test. */
        const val GESTURE_TIMEOUT_MILLIS = 60_000L
    }
}
