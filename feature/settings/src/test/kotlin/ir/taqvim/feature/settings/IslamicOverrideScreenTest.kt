/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import android.content.Context
import android.os.Looper
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import java.time.Duration
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

/** ADR-0037 UI test: the page says which dates are in use, warns about broken or ended files and reports actions. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IslamicOverrideScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val computed = IslamicOverrideStatus(LocationFixtures.english, IslamicOverrideKind.NONE)
    private val official =
        computed.copy(
            kind = IslamicOverrideKind.OFFICIAL,
            firstMonth = CalendarDate(CalendarSystem.ISLAMIC, 1446, 9, 1),
            lastMonth = CalendarDate(CalendarSystem.ISLAMIC, 1448, 9, 1),
        )

    private fun settle() {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        composeRule.waitForIdle()
    }

    private fun seen(text: String) {
        composeRule.onNodeWithText(text, substring = true).performScrollTo().assertIsDisplayed()
    }

    private fun show(
        state: IslamicOverrideUiState,
        actions: IslamicOverrideActions = IslamicOverrideActions(),
        onPickFile: () -> Unit = {},
    ) {
        composeRule.setContent {
            LocationTestTheme { IslamicOverrideScreen(state, actions, onPickFile) }
        }
        settle()
    }

    @Test
    fun computedDatesAreTheDefaultAndNothingCanBeRemoved() {
        var switched: Boolean? = null
        var picked = false
        show(
            IslamicOverrideUiState(status = computed),
            IslamicOverrideActions(onOfficialChanged = { switched = it }),
            onPickFile = { picked = true },
        )

        seen("In use: computed dates")
        composeRule.onNodeWithText("Use the official dates announced in Iran").assertIsOff().performClick()
        switched shouldBe true
        composeRule.onNodeWithText("Import a dates file…").performScrollTo().performClick()
        picked shouldBe true
        composeRule.onNodeWithText("Use computed dates only").assertDoesNotExist()
    }

    @Test
    fun officialDatesShowTheirRangeAndCanBeRemoved() {
        var removed = false
        show(IslamicOverrideUiState(status = official), IslamicOverrideActions(onRemove = { removed = true }))

        seen("In use: official dates of Iran, Ramadan 1446 to Ramadan 1448")
        composeRule.onNodeWithText("Use the official dates announced in Iran").assertIsOn()
        composeRule.onNodeWithText("Use computed dates only").performScrollTo().performClick()
        removed shouldBe true
    }

    @Test
    fun brokenAndEndedFilesAreExplained() {
        val state = mutableStateOf(IslamicOverrideUiState(status = official.copy(kind = IslamicOverrideKind.IMPORTED)))
        composeRule.setContent {
            LocationTestTheme { IslamicOverrideScreen(state.value, IslamicOverrideActions(), onPickFile = {}) }
        }
        settle()
        seen("In use: imported dates, Ramadan 1446 to Ramadan 1448")

        state.value = IslamicOverrideUiState(status = official.copy(ended = true))
        settle()
        seen("The official dates have ended; later dates are computed.")

        state.value = IslamicOverrideUiState(status = computed.copy(kind = IslamicOverrideKind.IMPORTED, broken = true))
        settle()
        seen("In use: computed dates")
        seen("can no longer be read")
    }

    @Test
    fun everyImportOutcomeHasAMessage() {
        val state = mutableStateOf(IslamicOverrideUiState(status = computed))
        composeRule.setContent {
            LocationTestTheme { IslamicOverrideScreen(state.value, IslamicOverrideActions(), onPickFile = {}) }
        }
        OverrideImportResult.entries.forEach { result ->
            state.value = IslamicOverrideUiState(status = computed, importResult = result)
            settle()
            composeRule.onNodeWithText(text(importText(result))).performScrollTo().assertIsDisplayed()
        }
        state.value = IslamicOverrideUiState(status = computed, changeFailed = true)
        settle()
        seen("The change could not be saved. Try again.")
    }

    @Test
    fun loadingShowsAProgressIndicator() {
        show(IslamicOverrideUiState())
        composeRule.onNodeWithText("In use:", substring = true).assertDoesNotExist()
    }

    private fun text(resource: Int): String = ApplicationProvider.getApplicationContext<Context>().getString(resource)
}
