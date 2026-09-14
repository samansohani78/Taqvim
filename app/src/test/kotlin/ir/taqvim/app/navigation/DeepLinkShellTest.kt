/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.navigation

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.app.TaqvimAppShell
import ir.taqvim.core.calendar.toJdn
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

/** T-1103 in the running shell: a day link opens the calendar on that day; an unreadable link shows the calendar. */
@RunWith(AndroidJUnit4::class)
class DeepLinkShellTest {
    @get:Rule
    val composeRule = createComposeRule()

    // Robolectric starts TaqvimApplication, and so Koin, for every test.
    @After
    fun stopDependencyGraph() {
        stopKoin()
    }

    @Test
    fun aDayLinkOpensTheCalendarOnThatDay() {
        val nowruz = DeepLinks.parse("taqvim://day/1405-01-01")
        assertTrue(nowruz == AppDestination.Day(LocalDate(2026, 3, 21).toJdn().value))
        var opened = 0
        composeRule.setContent { TaqvimAppShell(link = nowruz, onLinkOpened = { opened++ }) }

        composeRule.onNodeWithTag(destinationTag(nowruz)).assertExists()
        assertTrue(opened == 1)
    }

    @Test
    fun anUnreadableLinkShowsTheCalendar() {
        composeRule.setContent { TaqvimAppShell(link = DeepLinks.parse("taqvim://nothing-here")) }

        composeRule.onNodeWithTag(destinationTag(AppDestination.Calendar)).assertExists()
    }
}
