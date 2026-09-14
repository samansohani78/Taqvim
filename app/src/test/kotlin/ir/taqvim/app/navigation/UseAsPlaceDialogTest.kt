/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.core.model.Coordinates
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

/** T-1301: a point picked on the map is saved as the chosen place only after the user confirms it. */
@RunWith(AndroidJUnit4::class)
class UseAsPlaceDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    // Robolectric starts TaqvimApplication, and so Koin, for every test.
    @After
    fun stopDependencyGraph() {
        stopKoin()
    }

    @Test
    fun theDialogNamesThePointAndReportsConfirmAndCancel() {
        var confirmed = 0
        var dismissed = 0
        composeRule.setContent {
            UseAsPlaceDialog(Coordinates(35.6892, 51.389), onConfirm = { confirmed++ }, onDismiss = { dismissed++ })
        }

        composeRule.onNodeWithText("35.69, 51.39", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Use location").performClick()
        composeRule.onNodeWithText("Cancel").performClick()

        assertEquals(1, confirmed)
        assertEquals(1, dismissed)
    }
}
