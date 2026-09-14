/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.core.i18n.LanguageTable
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** T-1103 entry in the UI: a calendar route opened on a day shows that day's month. */
@RunWith(AndroidJUnit4::class)
class CalendarInitialDayScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theRouteOpensOnTheMonthOfTheInitialDay() {
        val english = requireNotNull(LanguageTable.forCode("en"))
        val days = FakeDaySource()
        val viewModel =
            CalendarViewModel(
                FakeSettingsSource(PERSIAN_FIRST.copy(languageCode = "en")),
                FakeTodaySource(gregorian(2026, 4, 10)),
                days,
                days,
                SearchEventsUseCase(FakeSearchSource(emptyMap())),
                FakePlaceSource(null),
                FakeNowSource(TEST_NOW),
                FakeDisplayStore(),
                initialDay = gregorian(2026, 12, 25),
            )
        composeRule.setContent { CalendarTestTheme { CalendarRoute(viewModel = viewModel) } }

        val dey1405 = "${requireNotNull(english.monthNames.persian)[9]} 1405"
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodes(hasText(dey1405)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
    }
}
