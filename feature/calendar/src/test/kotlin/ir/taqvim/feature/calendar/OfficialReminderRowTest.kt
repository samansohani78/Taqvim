/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.matchers.shouldBe
import ir.taqvim.core.i18n.LanguageTable
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** T-1002 UI: the reminder chips under an official event's source show what is on and report each change. */
@RunWith(RobolectricTestRunner::class)
class OfficialReminderRowTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val resources = RuntimeEnvironment.getApplication().resources
    private val english = requireNotNull(LanguageTable.forCode("en"))
    private val persian = requireNotNull(LanguageTable.forCode("fa"))
    private val actions = mutableListOf<CalendarAction>()

    @Test
    fun `chips show the enabled lead times and toggle them`() {
        val content =
            DayDetailsSamples.content(
                tab = DayDetailsTab.EVENTS,
                sourceEvent = DayDetailsSamples.official,
                officialReminders = OfficialReminderChoices(DayDetailsSamples.official.id, persistentSetOf(1)),
            )
        composeRule.setContent { CalendarTestTheme { DayDetailsPanel(content, onAction = { actions += it }) } }

        composeRule.onNodeWithText("Remind me").assertExists()
        composeRule.onNodeWithText("On the day").assertIsNotSelected()
        composeRule
            .onNodeWithText("1 day before")
            .assertIsSelected()
            .performClick()
        composeRule
            .onNodeWithText("3 days before")
            .assertIsNotSelected()
            .performClick()

        actions shouldBe
            listOf(
                CalendarAction.ToggleOfficialReminder(1, enabled = false),
                CalendarAction.ToggleOfficialReminder(3, enabled = true),
            )
    }

    @Test
    fun `choices of another event are not shown and labels use the language's digits`() {
        val content =
            DayDetailsSamples.content(
                tab = DayDetailsTab.EVENTS,
                sourceEvent = DayDetailsSamples.official,
                officialReminders = OfficialReminderChoices("another.event", persistentSetOf(0)),
            )
        composeRule.setContent { CalendarTestTheme { DayDetailsPanel(content, onAction = { actions += it }) } }

        composeRule.onNodeWithText("Remind me").assertDoesNotExist()
        reminderLabel(resources, 0, english) shouldBe "On the day"
        reminderLabel(resources, 7, english) shouldBe "7 days before"
        reminderLabel(resources, 3, persian) shouldBe "3 days before".replace("3", "۳")
    }
}
