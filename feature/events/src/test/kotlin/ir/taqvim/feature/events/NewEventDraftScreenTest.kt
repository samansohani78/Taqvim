/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.events

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.testing.FakeClock
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** T-1103 entry in the UI: the editor route opened with a drawn range shows its start and end times. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NewEventDraftScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theRouteShowsTheDraftsTimes() {
        val draft = NewEventDraft(LocalDate(2026, 12, 25).toJdn(), startMinute = 600, endMinute = 690)
        val viewModel =
            EventEditorViewModel(
                null,
                FakeEventStore(),
                FakeSettingsSource(),
                FakeClock(EditorFixtures.NOW),
                SavedStateHandle(),
                draft,
            )
        composeRule.setContent {
            EditorTestTheme { EventEditorRoute(null, onClose = {}, draft = draft, viewModel = viewModel) }
        }

        composeRule.onNodeWithText("10:00").performScrollTo().assertExists()
        composeRule.onNodeWithText("11:30").performScrollTo().assertExists()
    }
}
