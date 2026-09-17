/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import android.os.Looper
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Duration
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

/** F03 UI test: each subscription health state is shown, and its details open and close. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SubscriptionHealthScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val now = Instant.parse("2026-09-16T12:00:00Z")
    private val clock =
        object : Clock {
            override fun now(): Instant = this@SubscriptionHealthScreenTest.now
        }

    private fun at(instant: Instant) = instant.toEpochMilliseconds()

    private val hourly = SubscriptionHealthData(refreshIntervalMinutes = 60)
    private val items =
        listOf(
            SubscriptionItem(1, "Never", "https://example.org/1.ics", true, null, hourly),
            SubscriptionItem(
                2,
                "Fresh",
                "https://example.org/2.ics",
                true,
                at(now - 1.hours),
                hourly.copy(
                    lastCheckedAtEpochMillis = at(now - 10.minutes),
                    nextCheckAtEpochMillis = at(now + 50.minutes),
                    cachedEvents = 4,
                    cachedFromEpochMillis = at(now - 30.minutes),
                    cachedUntilEpochMillis = at(now + 2.hours),
                ),
            ),
            SubscriptionItem(
                3,
                "Old",
                "https://example.org/3.ics",
                true,
                at(now - 9.hours),
                hourly.copy(lastCheckedAtEpochMillis = at(now - 9.hours)),
            ),
            SubscriptionItem(
                4,
                "Broken",
                "https://example.org/4.ics",
                true,
                at(now - 2.hours),
                hourly.copy(
                    lastCheckedAtEpochMillis = at(now - 2.hours),
                    problemCount = 2,
                    error = SubscriptionError.NOT_AVAILABLE,
                    httpStatus = 404,
                    errorAtEpochMillis = at(now - 5.minutes),
                ),
            ),
            SubscriptionItem(5, "Off", "https://example.org/5.ics", false, null, hourly),
        )

    private fun settle() {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        composeRule.waitForIdle()
    }

    private fun show(): SubscriptionsViewModel {
        val viewModel =
            SubscriptionsViewModel(
                FakeSubscriptionsStore(items = items),
                FakeGeneralSettingsStore(LocationFixtures.english),
                clock,
                { TimeZone.UTC },
            )
        composeRule.setContent { LocationTestTheme { SubscriptionsRoute(viewModel = viewModel) } }
        settle()
        return viewModel
    }

    private fun seen(
        text: String,
        substring: Boolean = true,
    ) {
        composeRule.onNodeWithText(text, substring = substring).performScrollTo().assertIsDisplayed()
    }

    private fun details(name: String) {
        composeRule.onNodeWithContentDescription("Details of $name").performScrollTo().performClick()
        settle()
    }

    @Test
    fun everyHealthStateIsShown() {
        show()

        listOf("Not downloaded yet", "Downloaded", "Out of date", "Last download failed", "Paused").forEach {
            seen(it, substring = false)
        }
    }

    @Test
    fun detailsOpenWithTimesCacheAndFailureAndClose() {
        show()

        details("Broken")
        seen("Failed on ")
        seen("no calendar is available at this address (HTTP 404)")
        seen("Last successful download: ")
        seen("Items in the feed that could not be read fully: 2")
        seen("No events are cached")
        seen("Next check: as soon as the network allows")
        composeRule
            .onNode(
                SemanticsMatcher
                    .expectValue(SemanticsProperties.StateDescription, "Hide details")
                    .and(
                        SemanticsMatcher.expectValue(
                            SemanticsProperties.ContentDescription,
                            listOf("Details of Broken"),
                        ),
                    ),
            ).assertIsDisplayed()

        details("Fresh")
        seen("4 cached events from ")
        seen("Next check: Wednesday, September 16, 2026, 12:50", substring = false)

        details("Broken")
        composeRule.onNodeWithText("Items in the feed", substring = true).assertDoesNotExist()
    }

    @Test
    fun detailsOfNeverAndPausedFeedsSayWhatDidNotHappen() {
        show()

        details("Never")
        seen("Last successful download: never")
        seen("Last check: never")
        details("Off")
        seen("Next check: none while paused")
    }
}
