/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isNotSelected
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import ir.taqvim.core.i18n.DateFormatter
import ir.taqvim.core.i18n.DateStyle
import ir.taqvim.core.i18n.LanguageTable
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** T-802 UI: each day-details tab, the source tooltip with its citation, and a content description on every chip. */
@RunWith(RobolectricTestRunner::class)
class DayDetailsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val resources = RuntimeEnvironment.getApplication().resources
    private val english = requireNotNull(LanguageTable.forCode("en"))
    private val actions = mutableListOf<CalendarAction>()

    /** Shows the panel; tab and source-tooltip actions update its state as the view model does. */
    private fun show(initial: CalendarContent) {
        composeRule.setContent {
            var content by remember { mutableStateOf(initial) }
            CalendarTestTheme {
                DayDetailsPanel(content, onAction = { action ->
                    actions += action
                    content =
                        when (action) {
                            is CalendarAction.ShowEventSource -> content.copy(sourceEvent = action.event)
                            CalendarAction.DismissEventSource -> content.copy(sourceEvent = null)
                            is CalendarAction.SelectTab -> content.copy(selectedTab = action.tab)
                            else -> content
                        }
                })
            }
        }
    }

    @Test
    fun `the Calendars tab shows the day in every calendar with its week, season, Sun and Moon`() {
        val content = DayDetailsSamples.content()
        val overview = requireNotNull(content.overview)
        show(content)

        content.selectedDates.forEach { date ->
            composeRule
                .onNode(hasText(DateFormatter.format(date, content.selectedDay.weekday(), english, DateStyle.LONG)))
                .assertExists()
        }
        composeRule.onNode(hasText(resources.getString(R.string.calendar_today))).assertExists()
        composeRule.onNode(hasText(weekText(resources, overview, english))).assertExists()
        composeRule.onNode(hasText(seasonText(resources, overview, english))).assertExists()
        composeRule.onNode(hasContentDescription(skyText(resources, overview, english))).assertExists()
        seasonText(resources, overview, english) shouldBe "Spring: day 1 of 93"
        weekText(resources, overview, english) shouldBe "Day 1 of the week, week 1 of the year"
    }

    @Test
    fun `every event chip has a content description with its source`() {
        show(DayDetailsSamples.content(tab = DayDetailsTab.EVENTS))

        DayDetailsSamples.events.forEach { event ->
            composeRule
                .onNode(hasContentDescription(chipDescription(resources, event)) and hasClickAction())
                .assertExists()
        }
        composeRule
            .onAllNodes(hasClickAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
            .assertCountEquals(DayDetailsSamples.events.size)
        chipDescription(resources, DayDetailsSamples.official) shouldBe "Nowruz, Official calendar of Iran, Holiday"
        chipDescription(resources, DayDetailsSamples.device) shouldBe "Meeting, Device calendar"
        chipDescription(resources, DayDetailsSamples.subscription) shouldBe "Talk, Subscribed calendar"
    }

    @Test
    fun `an official event shows its source and citation, which opens, and closes again`() {
        show(DayDetailsSamples.content(tab = DayDetailsTab.EVENTS))
        val citation = DayDetailsSamples.citation

        composeRule.onNode(hasContentDescription(chipDescription(resources, DayDetailsSamples.official))).performClick()
        composeRule
            .onNodeWithText(resources.getString(R.string.calendar_citation_page, citation.title, "3"))
            .assertExists()
        composeRule.onNodeWithText(resources.getString(R.string.calendar_open_source)).performClick()
        actions shouldContain CalendarAction.OpenCitation(citation.url)

        composeRule.onNodeWithText(resources.getString(R.string.calendar_close)).performClick()
        composeRule.onNodeWithText(resources.getString(R.string.calendar_open_source)).assertDoesNotExist()

        composeRule.onNode(hasContentDescription(chipDescription(resources, DayDetailsSamples.uncited))).performClick()
        composeRule.onNodeWithText(resources.getString(R.string.calendar_no_citation)).assertExists()
        composeRule.onNodeWithText(resources.getString(R.string.calendar_open_source)).assertDoesNotExist()
    }

    @Test
    fun `personal, device and subscribed events open instead of showing a source`() {
        show(DayDetailsSamples.content(tab = DayDetailsTab.EVENTS))

        listOf(DayDetailsSamples.personal, DayDetailsSamples.device, DayDetailsSamples.subscription).forEach { event ->
            composeRule.onNode(hasContentDescription(chipDescription(resources, event))).performClick()
            actions shouldContain CalendarAction.OpenEvent(event)
        }
        actions.filterIsInstance<CalendarAction.ShowEventSource>() shouldBe emptyList()
    }

    @Test
    fun `a day without events offers to add one`() {
        show(DayDetailsSamples.content(tab = DayDetailsTab.EVENTS, events = emptyList()))

        composeRule.onNodeWithText(resources.getString(R.string.calendar_no_events)).assertExists()
        composeRule.onNodeWithText(resources.getString(R.string.calendar_add_event)).performClick()
        actions shouldContain CalendarAction.CreateEvent(DayDetailsSamples.nowruz)
    }

    @Test
    fun `the Times tab names the place and method and announces the next time as selected`() {
        val content = DayDetailsSamples.content(tab = DayDetailsTab.TIMES)
        show(content)
        val method = resources.getString(R.string.calendar_method_tehran)

        composeRule.onNode(hasText(resources.getString(R.string.calendar_times_place, "Tehran", method))).assertExists()
        composeRule.onNode(hasText(resources.getString(R.string.calendar_time_dhuhr)) and isSelected()).assertExists()
        composeRule.onNode(hasText(resources.getString(R.string.calendar_time_fajr)) and isNotSelected()).assertExists()
        val times = requireNotNull((content.times as? DayTimesState.Ready)?.times)
        val sunrise = localized(requireNotNull(times.entries[1].time), english)
        val sunset = localized(requireNotNull(times.entries[4].time), english)
        composeRule
            .onNodeWithContentDescription(resources.getString(R.string.calendar_sun_path, sunrise, sunset))
            .assertExists()
    }

    @Test
    fun `without a place the Times tab asks for a city`() {
        show(DayDetailsSamples.content(tab = DayDetailsTab.TIMES, place = null))

        composeRule.onNodeWithText(resources.getString(R.string.calendar_no_place_title)).assertExists()
        composeRule.onNodeWithText(resources.getString(R.string.calendar_no_place_message)).assertExists()
    }

    @Test
    fun `tabs switch and loading content is announced`() {
        val loading =
            DayDetailsSamples.content().copy(overview = null, dayDetails = null, times = DayTimesState.Loading)
        show(loading)

        composeRule.onNodeWithContentDescription(resources.getString(R.string.calendar_details_loading)).assertExists()
        composeRule.onNodeWithText(resources.getString(R.string.calendar_tab_events)).performClick()
        composeRule.onNodeWithContentDescription(resources.getString(R.string.calendar_details_loading)).assertExists()
        composeRule.onNodeWithText(resources.getString(R.string.calendar_tab_times)).performClick()
        composeRule.onNodeWithContentDescription(resources.getString(R.string.calendar_times_loading)).assertExists()
        actions shouldContain CalendarAction.SelectTab(DayDetailsTab.TIMES)
        actions shouldNotContain CalendarAction.SelectTab(DayDetailsTab.CALENDARS)
    }

    @Test
    fun `the calendar screen shows the selected day's events under the month`() {
        val days = FakeDaySource()
        val today = gregorian(2026, 4, 10)
        val viewModel =
            CalendarViewModel(
                FakeSettingsSource(PERSIAN_FIRST.copy(languageCode = "en")),
                FakeTodaySource(today),
                days,
                days,
                SearchEventsUseCase(FakeSearchSource(emptyMap())),
                FakePlaceSource(TEHRAN),
                FakeNowSource(TEST_NOW),
            )
        composeRule.setContent { CalendarTestTheme { CalendarRoute(viewModel = viewModel) } }
        val eventsTab = hasText(resources.getString(R.string.calendar_tab_events))
        composeRule.waitUntil(TIMEOUT_MILLIS) { composeRule.onAllNodes(eventsTab).fetchSemanticsNodes().isNotEmpty() }

        composeRule.onNode(eventsTab).performScrollTo().performClick()
        val chip = hasContentDescription(chipDescription(resources, FakeDaySource.eventOn(today)))
        composeRule.waitUntil(TIMEOUT_MILLIS) { composeRule.onAllNodes(chip).fetchSemanticsNodes().isNotEmpty() }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
    }
}
