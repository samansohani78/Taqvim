/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * T-1003 calendar files UI: the page the user reaches from Settings, bound to its view model, so the test fails if
 * the screen stops calling the picker or the view model stops reaching the store (ADR-0054).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IcsFilesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val store = FakeIcsFilesStore()
    private val picks = mutableListOf<String>()

    /** The screen over a real [IcsFilesViewModel], with the picker stubbed where the system dialog would be. */
    private fun showPage(picked: String? = null) {
        val viewModel = IcsFilesViewModel(store)
        composeRule.setContent {
            LocationTestTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                IcsFilesScreen(
                    state = state,
                    actions =
                        IcsFilesActions(
                            onDuplicatesChanged = viewModel::onDuplicatesChanged,
                            onImport = viewModel::onImport,
                            onExport = viewModel::onExport,
                        ),
                    onPickImport = {
                        picks += "import"
                        picked?.let(viewModel::onImport)
                    },
                    onPickExport = {
                        picks += "export"
                        picked?.let(viewModel::onExport)
                    },
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun importingAFileReportsWhatWasAddedUpdatedAndLeftAlone() {
        showPage(picked = "content://calendar.ics")

        composeRule.onNodeWithText("Choose a file to import").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("import"), picks)
        assertEquals(listOf("content://calendar.ics" to IcsDuplicates.REPLACE), store.imports)
        composeRule
            .onNodeWithText("Imported. Added: 1 · Updated: 0 · Unchanged: 0")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun turningOffUpdatingSendsTheSkipPolicyToTheStore() {
        showPage(picked = "content://calendar.ics")

        composeRule.onNodeWithText("Update events already here").performScrollTo().performClick()
        composeRule.onNodeWithText("Choose a file to import").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("content://calendar.ics" to IcsDuplicates.SKIP), store.imports)
    }

    @Test
    fun exportIsOfferedOnlyWhenThereIsSomethingToWrite() {
        store.events.value = 0
        showPage()

        composeRule.onNodeWithText("Choose where to save").performScrollTo().assertIsNotEnabled()
        store.events.value = 4
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Events to write: 4", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Choose where to save").performScrollTo().assertIsEnabled()
    }

    @Test
    fun exportingReportsHowManyEventsWereWritten() {
        store.events.value = 3
        showPage(picked = "content://out.ics")

        composeRule.onNodeWithText("Choose where to save").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("export"), picks)
        assertEquals(listOf("content://out.ics"), store.exports)
        composeRule.onNodeWithText("Exported. Events written: 3").performScrollTo().assertIsDisplayed()
    }
}
