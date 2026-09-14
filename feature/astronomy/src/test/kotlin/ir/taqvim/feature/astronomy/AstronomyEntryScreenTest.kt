/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.astronomy

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.testing.FakeClock
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** T-1103 entry in the UI: the Astronomy route opened for planetary hours shows that dialog. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AstronomyEntryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theRouteOpensThePlanetaryHoursOfTheEntryDay() {
        val tehran = AstronomyFixtures.tehran()
        val entry = AstronomyEntry(AstronomyDialogKind.PLANETARY_HOURS, LocalDate(2026, 3, 21).toJdn())
        val viewModel =
            AstronomyViewModel(
                { flowOf(tehran) },
                FakeClock(AstronomyFixtures.at("2026-06-21T12:00", tehran)),
                entry = entry,
            )
        composeRule.setContent { AstronomyTestTheme { AstronomyRoute(entry = entry, viewModel = viewModel) } }

        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule
                .onAllNodes(hasText("Planetary hours of", substring = true))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
    }
}
