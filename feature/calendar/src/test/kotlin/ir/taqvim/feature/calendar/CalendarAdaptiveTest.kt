/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
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
import org.robolectric.annotation.Config

/** T-806 UI: two panes on a tablet, stacked panes on a phone, and the state kept while the window is resized. */
@RunWith(RobolectricTestRunner::class)
class CalendarAdaptiveTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val resources = RuntimeEnvironment.getApplication().resources
    private val english = requireNotNull(LanguageTable.forCode("en"))

    /** 21 Farvardin 1405. */
    private val today = gregorian(2026, 4, 10)
    private val otherDay = gregorian(2026, 4, 15)

    private fun viewModel(): CalendarViewModel {
        val days = FakeDaySource()
        return CalendarViewModel(
            FakeSettingsSource(PERSIAN_FIRST.copy(languageCode = "en")),
            FakeTodaySource(today),
            days,
            days,
            SearchEventsUseCase(FakeSearchSource(emptyMap())),
            FakePlaceSource(null),
            FakeNowSource(TEST_NOW),
            FakeDisplayStore(),
        )
    }

    private fun awaitNode(matcher: SemanticsMatcher) {
        composeRule.waitUntil(TIMEOUT_MILLIS) { composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun longDate(day: Jdn): SemanticsMatcher =
        hasContentDescription(
            DateFormatter.format(PersianCalendarSystem.fromJdn(day), day.weekday(), english, DateStyle.LONG),
            substring = true,
        )

    private fun paneBounds(): Pair<Rect, Rect> {
        awaitNode(hasTestTag(CALENDAR_DETAILS_PANE_TAG))
        val month = composeRule.onNodeWithTag(CALENDAR_MONTH_PANE_TAG).getBoundsInRoot()
        val details = composeRule.onNodeWithTag(CALENDAR_DETAILS_PANE_TAG).getBoundsInRoot()
        return Rect(month.left.value, month.top.value, month.right.value, month.bottom.value) to
            Rect(details.left.value, details.top.value, details.right.value, details.bottom.value)
    }

    private fun assertSideBySide() {
        val (month, details) = paneBounds()
        details.left shouldBeGreaterThanOrEqualTo month.right
    }

    private fun assertStacked() {
        val (month, details) = paneBounds()
        details.top shouldBeGreaterThanOrEqualTo month.bottom
    }

    @Test
    @Config(qualifiers = TABLET)
    fun `a tablet shows the month and the day details side by side`() {
        composeRule.setContent { CalendarTestTheme { CalendarRoute(viewModel = viewModel()) } }

        assertSideBySide()
        composeRule.onNodeWithTag(CALENDAR_MONTH_PANE_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(resources.getString(R.string.calendar_tab_events)).assertIsDisplayed()
    }

    @Test
    fun `a phone stacks the day details under the month`() {
        composeRule.setContent { CalendarTestTheme { CalendarRoute(viewModel = viewModel()) } }

        assertStacked()
    }

    @Test
    @Config(qualifiers = TABLET)
    fun `the selection survives resizing between one and two panes`() {
        val width = mutableStateOf(COMPACT_WIDTH)
        val viewModel = viewModel()
        composeRule.setContent {
            CalendarTestTheme {
                Box(Modifier.width(width.value).fillMaxHeight()) { CalendarRoute(viewModel = viewModel) }
            }
        }
        assertStacked()
        awaitNode(longDate(otherDay))
        composeRule.onAllNodes(longDate(otherDay)).onFirst().performClick()
        awaitNode(longDate(otherDay) and isSelected())

        width.value = EXPANDED_WIDTH
        composeRule.waitForIdle()
        assertSideBySide()
        awaitNode(longDate(otherDay) and isSelected())

        width.value = COMPACT_WIDTH
        composeRule.waitForIdle()
        assertStacked()
        awaitNode(longDate(otherDay) and isSelected())
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
        const val TABLET = "w1280dp-h800dp-land-mdpi"
        val COMPACT_WIDTH = 400.dp
        val EXPANDED_WIDTH = 900.dp
    }
}
