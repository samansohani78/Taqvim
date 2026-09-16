/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.events

import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.core.testing.FakeClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * B10: system Back follows the same rule as Cancel. Back from the screen behind the editor is counted by an outer
 * handler registered first, so it only fires when the editor lets Back through.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditorBackTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val outcomes = mutableListOf<EditorOutcome>()
    private var navigatedBack = 0
    private lateinit var dispatcher: OnBackPressedDispatcher

    private fun route(store: FakeEventStore = FakeEventStore()) {
        val viewModel =
            EventEditorViewModel(null, store, FakeSettingsSource(), FakeClock(EditorFixtures.NOW), SavedStateHandle())
        composeRule.setContent {
            dispatcher = requireNotNull(LocalOnBackPressedDispatcherOwner.current).onBackPressedDispatcher
            BackHandler { navigatedBack++ }
            EditorTestTheme { EventEditorRoute(null, onClose = { outcomes += it }, viewModel = viewModel) }
        }
    }

    private fun screen(session: EditorSession.Editing) {
        val state = EditorPresenter.present(session, EditorFixtures.settings())
        composeRule.setContent {
            dispatcher = requireNotNull(LocalOnBackPressedDispatcherOwner.current).onBackPressedDispatcher
            BackHandler { navigatedBack++ }
            EditorTestTheme {
                EventEditorScreen(state, EventEditorActions(onDiscard = { outcomes += EditorOutcome.DISCARDED }))
            }
        }
    }

    private fun pressBack() {
        composeRule.runOnIdle { dispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    @Test
    fun backLeavesAnUnchangedEditorAtOnce() {
        route()
        pressBack()

        assertEquals(1, navigatedBack)
        composeRule.onNodeWithText("Discard changes?").assertDoesNotExist()
    }

    @Test
    fun backAsksBeforeDiscardingChangesLikeCancel() {
        route()
        composeRule.onNodeWithText("Title").performTextInput("Dentist")
        pressBack()
        composeRule.onNodeWithText("Discard changes?").assertIsDisplayed()
        composeRule.onNodeWithText("Keep editing").performClick()
        composeRule.onNodeWithText("Discard changes?").assertDoesNotExist()
        composeRule.onNodeWithText("Dentist").assertIsDisplayed()
        assertTrue(outcomes.isEmpty())

        pressBack()
        composeRule.onNodeWithText("Discard").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(EditorOutcome.DISCARDED), outcomes)
        assertEquals(0, navigatedBack)
    }

    @Test
    fun aCancelledBackGestureChangesNothing() {
        val form = EditorFixtures.form()
        screen(EditorSession.Editing(form.copy(title = "Changed"), original = form))
        composeRule.runOnIdle {
            dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 0f, 0.3f, BackEventCompat.EDGE_LEFT))
            dispatcher.dispatchOnBackProgressed(BackEventCompat(10f, 0f, 0.6f, BackEventCompat.EDGE_LEFT))
            dispatcher.dispatchOnBackCancelled()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Discard changes?").assertDoesNotExist()
        assertTrue(outcomes.isEmpty())
        assertEquals(0, navigatedBack)
    }

    @Test
    fun backWaitsWhileSavingOrDeleting() {
        val form = EditorFixtures.form()
        screen(EditorSession.Editing(form.copy(title = "Changed"), original = form, busy = true))
        pressBack()

        composeRule.onNodeWithText("Discard changes?").assertDoesNotExist()
        composeRule.onNodeWithText("Cancel").assertIsNotEnabled()
        assertTrue(outcomes.isEmpty())
        assertEquals(0, navigatedBack)
    }

    @Test
    fun theCloseRuleCoversEveryState() {
        val form = EditorFixtures.form()

        fun decision(session: EditorSession.Editing): CloseDecision =
            requireNotNull(
                EditorPresenter.present(session, EditorFixtures.settings()).content as? EditorContent.Editing,
            ).closeDecision

        assertEquals(CloseDecision.CLOSE, decision(EditorSession.Editing(form, form)))
        assertEquals(CloseDecision.CONFIRM, decision(EditorSession.Editing(form.copy(title = "x"), form)))
        assertEquals(CloseDecision.WAIT, decision(EditorSession.Editing(form, form, busy = true)))
        assertEquals(CloseDecision.WAIT, decision(EditorSession.Editing(form.copy(title = "x"), form, busy = true)))
    }
}
