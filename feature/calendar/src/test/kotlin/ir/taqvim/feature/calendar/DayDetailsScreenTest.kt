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
import ir.taqvim.core.calendar.DateOrigin
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.i18n.DateFormatter
import ir.taqvim.core.i18n.DateStyle
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.ui.R as SharedR
import kotlinx.collections.immutable.toImmutableList
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
        composeRule.onNode(hasText(resources.getString(R.string.calendar_origin_computed))).assertExists()
    }

    @Test
    fun `an Islamic date from an official override is labelled and other dates carry no label`() {
        showIslamicOrigin(DateOrigin.OFFICIAL_OVERRIDE, R.string.calendar_origin_official)
    }

    @Test
    fun `an Islamic date from a printed calendar is labelled`() {
        showIslamicOrigin(DateOrigin.PUBLISHED_CALENDAR, R.string.calendar_origin_published)
    }

    private fun showIslamicOrigin(
        origin: DateOrigin,
        label: Int,
    ) {
        val content = DayDetailsSamples.content()
        val islamic = content.selectedDates.indexOfFirst { it.system == CalendarSystem.ISLAMIC }
        val origins = content.selectedDates.indices.map { if (it == islamic) origin else DateOrigin.COMPUTED }
        show(content.copy(selectedOrigins = origins.toImmutableList()))
        composeRule.onNode(hasText(resources.getString(label))).assertExists()
        composeRule.onAllNodes(hasText(resources.getString(R.string.calendar_origin_computed))).assertCountEquals(0)
    }

    private fun variantRows(step: Int) =
        IslamicVariant.entries
            .mapIndexed { index, variant ->
                IslamicVariantDate(
                    variant = variant,
                    date = CalendarDate(CalendarSystem.ISLAMIC, 1447, 4, 1 + index * step),
                    origin = DateOrigin.COMPUTED,
                    isPreferred = index == 0,
                )
            }.toImmutableList()

    @Test
    fun `a date every Islamic method agrees on offers nothing to expand`() {
        // Five identical rows would be noise, so the row exists only where there is a question to answer.
        show(DayDetailsSamples.content().copy(islamicVariantDates = variantRows(step = 0)))

        composeRule
            .onAllNodes(hasText(resources.getString(R.string.calendar_variants_differ)))
            .assertCountEquals(0)
    }

    @Test
    fun `disagreeing Islamic methods are offered, and revealed only when asked for`() {
        show(DayDetailsSamples.content().copy(islamicVariantDates = variantRows(step = 1)))
        val differ = resources.getString(R.string.calendar_variants_differ)
        val ummAlQura = resources.getString(SharedR.string.shared_islamic_variant_umm_al_qura)

        // Offered, but collapsed: progressive disclosure, not a denser screen.
        composeRule.onNode(hasText(differ)).assertExists()
        composeRule.onAllNodes(hasText(ummAlQura)).assertCountEquals(0)

        composeRule.onNode(hasText(differ)).performClick()

        composeRule.onNode(hasText(ummAlQura)).assertExists()
        composeRule.onNode(hasText(resources.getString(R.string.calendar_variants_hide))).assertExists()
    }

    @Test
    fun `every event chip has a content description with its source`() {
        show(DayDetailsSamples.content(tab = DayDetailsTab.EVENTS))

        DayDetailsSamples.events.forEach { event ->
            composeRule
                .onNode(hasContentDescription(chipDescription(resources, event)) and hasClickAction())
                .assertExists()
        }
        // The chips plus the panel's one share button (T-802). Counting them keeps the original guarantee — that no
        // chip is missing a description and nothing else in the tab is silently clickable — rather than loosening it
        // to "at least"; a new interactive element has to be accounted for here deliberately.
        composeRule
            .onAllNodes(hasClickAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
            .assertCountEquals(DayDetailsSamples.events.size + 1)
        composeRule
            .onNode(hasContentDescription(resources.getString(R.string.calendar_share_day)) and hasClickAction())
            .assertExists()
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
    fun `an official event in the Islamic calendar says where its date comes from`() {
        val eid =
            DayDetailsSamples.official.copy(
                id = "af.holiday.eid-al-adha.1",
                title = "Eid al-Adha",
                source = EventSource.AFGHANISTAN_OFFICIAL,
                dateOrigin = DateOrigin.COMPUTED,
            )
        sourceBody(resources, eid) shouldBe "Eid al-Adha\nDate: Computed"
        sourceBody(resources, eid.copy(dateOrigin = DateOrigin.OFFICIAL_OVERRIDE)) shouldBe
            "Eid al-Adha\nDate: Official announced date"
        sourceBody(resources, DayDetailsSamples.official) shouldBe DayDetailsSamples.official.title
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
                FakeDisplayStore(),
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
