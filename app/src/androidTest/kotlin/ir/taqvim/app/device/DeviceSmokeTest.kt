/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.device

import android.Manifest
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Opens every screen of Taqvim on a real Android device (Gradle Managed Device, T-1600/T-1700) in Persian and English:
 * each `taqvim://` link, each top-level tab and each More entry, selects every tab of the screen and rotates the linked
 * screens once. A crash, ANR or missing screen fails the test.
 */
@RunWith(Parameterized::class)
class DeviceSmokeTest(
    private val language: String,
) {
    @get:Rule
    val notifications: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @Before
    fun setUp() {
        useLanguage(language)
    }

    @Test
    fun everyLinkedScreenOpensAndSurvivesRotation() {
        LINKS.forEach { (link, destination) ->
            openLink(link, destinationTag(destination))
            visitTabs()
            rotate(destinationTag(destination))
            assertInFront()
        }
    }

    @Test
    fun everyTabAndMoreEntryOpens() {
        openLink("taqvim://calendar", destinationTag("Calendar"))
        TABS.forEach { (tab, destination) ->
            awaitTag("tab:$tab").click()
            awaitTag(destinationTag(destination))
            visitTabs()
        }
        MORE_ENTRIES.forEach { (entry, destination) ->
            awaitTag("tab:MORE").click()
            awaitTag("more:$entry").click()
            awaitTag(destinationTag(destination))
            visitTabs()
            if (entry == "MAP") visitToggles()
            assertInFront()
            back(destinationTag("More"))
        }
    }

    @Test
    fun onboardingPagesOpen() {
        updatePreferences { it.copy(onboardingCompleted = false) }
        openLink("taqvim://calendar", "onboarding:next")
        repeat(ONBOARDING_PAGES) {
            rotate("onboarding:next")
            awaitTag("onboarding:next").click()
        }
        awaitTag(destinationTag("Calendar"))
    }

    /** Switches every checkable control of the screen on and back, e.g. each world map layer and the globe (T-1301). */
    private fun visitToggles() {
        val count = device.findObjects(By.checkable(true)).size
        repeat(count) { index ->
            device.findObjects(By.checkable(true)).getOrNull(index)?.let { toggle ->
                val wasChecked = toggle.isChecked
                toggle.click()
                device.waitForIdle()
                if (!wasChecked) device.findObjects(By.checkable(true)).getOrNull(index)?.click()
                device.waitForIdle()
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun languages(): List<String> = listOf("fa", "en")

        private const val ONBOARDING_PAGES = 3

        /** `destinationTag` of `:app`: every screen is tagged with its destination's class name. */
        private fun destinationTag(name: String): String = "destination:$name"

        /** Documented links (ADR-0016) and the destination each opens. */
        private val LINKS =
            listOf(
                "taqvim://calendar" to "Calendar",
                "taqvim://day/1405-01-01" to "Day",
                "taqvim://day/1447-09-01?calendar=islamic" to "Day",
                "taqvim://day/2083-01-01?calendar=nepali" to "Day",
                "taqvim://day/5787-01-01?calendar=hebrew" to "Day",
                "taqvim://timeline/1405-01-01" to "Timeline",
                "taqvim://event/new/1405-01-01" to "EventEditor",
                "taqvim://convert?date=1405-01-01&from=persian" to "Converter",
                "taqvim://times" to "Times",
                "taqvim://astronomy" to "Astronomy",
                "taqvim://map" to "WorldMap",
                "taqvim://search?q=nowruz" to "SearchFor",
                "taqvim://settings" to "Settings",
                "taqvim://settings/backup" to "Backup",
                "taqvim://settings/privacy" to "Privacy",
                "taqvim://settings/about" to "About",
            )

        /** Top-level tabs (`TopLevelTab`) and their screens. */
        private val TABS = listOf("TIMES" to "Times", "TOOLS" to "Tools", "MORE" to "More", "CALENDAR" to "Calendar")

        /** More entries (`MoreEntry`) and their screens. */
        private val MORE_ENTRIES =
            listOf(
                "YEAR" to "Year",
                "AGENDA" to "Agenda",
                "SEARCH" to "Search",
                "ASTRONOMY" to "Astronomy",
                "MAP" to "WorldMap",
                "COMPASS" to "Compass",
                "LEVEL" to "Level",
                "SETTINGS" to "Settings",
                "BACKUP" to "Backup",
                "PRIVACY" to "Privacy",
                "ABOUT" to "About",
            )
    }
}
