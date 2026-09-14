/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.navigation

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.app.MainActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf

/** ADR-0015 in the running app: tabs open their screens, screens open from More, back returns, links leave the app. */
@RunWith(AndroidJUnit4::class)
class AppShellTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    // Robolectric starts TaqvimApplication, and so Koin, for every test.
    @After
    fun stopDependencyGraph() {
        stopKoin()
    }

    @Test
    fun theCalendarOpensFirstAndEachTabOpensItsScreen() {
        screen(AppDestination.Calendar).assertExists()
        composeRule.onNodeWithTag(tabTag(TopLevelTab.CALENDAR)).assertIsSelected()

        listOf(TopLevelTab.TIMES, TopLevelTab.TOOLS, TopLevelTab.MORE, TopLevelTab.CALENDAR).forEach { tab ->
            composeRule.onNodeWithTag(tabTag(tab)).performClick()
            screen(tab.destination).assertExists()
            composeRule.onNodeWithTag(tabTag(tab)).assertIsSelected()
        }
    }

    @Test
    fun screensOpenFromMoreAndBackReturnsToTheCalendar() {
        composeRule.onNodeWithTag(tabTag(TopLevelTab.MORE)).performClick()
        composeRule.onNodeWithTag("more:" + MoreEntry.YEAR.name).performClick()
        screen(AppDestination.Year).assertExists()
        composeRule.onNodeWithTag(tabTag(TopLevelTab.MORE)).assertIsSelected()

        Espresso.pressBack()
        screen(AppDestination.More).assertExists()
        Espresso.pressBack()
        screen(AppDestination.Calendar).assertExists()
    }

    @Test
    fun webLinksAndDeviceEventsOpenOutsideTheApp() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val actions = ContextExternalActions(application)
        val shadow = shadowOf(application)
        shadow.clearNextStartedActivities()

        actions.openUrl("https://calendar.ut.ac.ir/Fa/")
        val link = shadow.nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, link.action)
        assertEquals(Uri.parse("https://calendar.ut.ac.ir/Fa/"), link.data)

        actions.openUrl("javascript:alert(1)")
        assertNull(shadow.nextStartedActivity)

        actions.openDeviceEvent(5)
        val event = shadow.nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, event.action)
        assertEquals(Uri.withAppendedPath(CalendarContract.Events.CONTENT_URI, "5"), event.data)
    }

    private fun screen(destination: AppDestination) = composeRule.onNodeWithTag(destinationTag(destination))
}
