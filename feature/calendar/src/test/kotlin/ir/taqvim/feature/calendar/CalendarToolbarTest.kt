/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.i18n.DateFormatter
import ir.taqvim.core.i18n.DateStyle
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** T-803 UI: toolbar actions (today, search) and every menu entry, through the route and its view model. */
@RunWith(RobolectricTestRunner::class)
class CalendarToolbarTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val resources = RuntimeEnvironment.getApplication().resources
    private val english = requireNotNull(LanguageTable.forCode("en"))

    /** 21 Farvardin 1405. */
    private val today = gregorian(2026, 4, 10)
    private val otherDay = gregorian(2026, 4, 15)
    private val settings = FakeSettingsSource(PERSIAN_FIRST.copy(languageCode = "en"))
    private val store = FakeDisplayStore(settings)
    private val printed = mutableListOf<String>()

    private fun show(navigation: CalendarNavigation = CalendarNavigation()) {
        val days = FakeDaySource()
        val viewModel =
            CalendarViewModel(
                settings,
                FakeTodaySource(today),
                days,
                days,
                SearchEventsUseCase(FakeSearchSource(emptyMap())),
                FakePlaceSource(null),
                FakeNowSource(TEST_NOW),
                store,
            )
        composeRule.setContent {
            CalendarTestTheme {
                CalendarRoute(
                    navigation = navigation,
                    printer = { _, html, _ -> printed += html },
                    viewModel = viewModel,
                )
            }
        }
    }

    private fun string(
        @StringRes id: Int,
    ): String = resources.getString(id)

    private fun awaitNode(matcher: SemanticsMatcher) {
        composeRule.waitUntil(TIMEOUT_MILLIS) { composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun awaitGone(matcher: SemanticsMatcher) {
        composeRule.waitUntil(TIMEOUT_MILLIS) { composeRule.onAllNodes(matcher).fetchSemanticsNodes().isEmpty() }
    }

    private fun longDate(day: Jdn): SemanticsMatcher =
        hasContentDescription(
            DateFormatter.format(PersianCalendarSystem.fromJdn(day), day.weekday(), english, DateStyle.LONG),
            substring = true,
        )

    private fun openMenuItem(
        @StringRes label: Int,
    ) {
        val more = string(R.string.calendar_action_more)
        awaitNode(hasContentDescription(more))
        composeRule.onNodeWithContentDescription(more).performClick()
        awaitNode(hasText(string(label)))
        composeRule.onNodeWithText(string(label)).performClick()
    }

    private fun selectOtherDay() {
        awaitNode(longDate(otherDay))
        composeRule.onAllNodes(longDate(otherDay)).onFirst().performClick()
        awaitNode(longDate(otherDay) and isSelected())
    }

    @Test
    fun `the today action brings the selection back to today`() {
        show()
        selectOtherDay()

        composeRule.onNodeWithContentDescription(string(R.string.calendar_today)).performClick()
        awaitNode(longDate(today) and isSelected())
    }

    @Test
    fun `search, shift work and planetary hours open their screens`() {
        var searches = 0
        var shiftWork = 0
        var planetaryHours: Jdn? = null
        show(
            CalendarNavigation(
                onOpenSearch = { searches++ },
                onOpenShiftWork = { shiftWork++ },
                onOpenPlanetaryHours = { planetaryHours = it },
            ),
        )
        awaitNode(hasContentDescription(string(R.string.calendar_action_search)))

        composeRule.onNodeWithContentDescription(string(R.string.calendar_action_search)).performClick()
        composeRule.waitUntil(TIMEOUT_MILLIS) { searches == 1 }
        openMenuItem(R.string.calendar_menu_shift_work)
        composeRule.waitUntil(TIMEOUT_MILLIS) { shiftWork == 1 }
        openMenuItem(R.string.calendar_menu_planetary_hours)
        composeRule.waitUntil(TIMEOUT_MILLIS) { planetaryHours == today }
    }

    @Test
    fun `print month hands the shown month to the printer`() {
        show()
        openMenuItem(R.string.calendar_menu_print_month)

        composeRule.waitUntil(TIMEOUT_MILLIS) { printed.isNotEmpty() }
        printed.single() shouldContain "<h1>${requireNotNull(english.monthNames.persian).first()} 1405</h1>"
    }

    @Test
    fun `week numbers are stored, shown and checked in the menu`() {
        show()
        openMenuItem(R.string.calendar_menu_week_numbers)

        awaitNode(hasContentDescription("Week 2"))
        store.weekNumbers shouldBe listOf(true)
        composeRule.onNodeWithContentDescription(string(R.string.calendar_action_more)).performClick()
        awaitNode(
            hasText(string(R.string.calendar_menu_week_numbers)) and
                SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On),
        )
    }

    @Test
    fun `the secondary calendar is chosen among the available calendars`() {
        show()
        openMenuItem(R.string.calendar_menu_secondary_calendar)
        val option = SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)
        awaitNode(hasText(string(R.string.calendar_system_islamic)) and option)
        composeRule.onNode(hasText(string(R.string.calendar_system_gregorian)) and isSelected()).assertExists()

        composeRule.onNode(hasText(string(R.string.calendar_system_islamic)) and option).performClick()
        composeRule.waitUntil(TIMEOUT_MILLIS) { store.secondaries == listOf(CalendarSystem.ISLAMIC) }
        awaitGone(hasText(string(R.string.calendar_menu_secondary_calendar)))
    }

    @Test
    fun `go to date opens the picker at the selected day and confirming selects it`() {
        show()
        selectOtherDay()
        openMenuItem(R.string.calendar_menu_pick_date)

        awaitNode(hasText(string(R.string.calendar_picker_confirm)))
        composeRule.onNodeWithText(string(R.string.calendar_picker_confirm)).performClick()
        awaitGone(hasText(string(R.string.calendar_picker_confirm)))
        awaitNode(longDate(otherDay) and isSelected())
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
    }
}
