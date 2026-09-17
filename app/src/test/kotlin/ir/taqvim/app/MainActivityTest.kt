/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.app.navigation.AppDestination
import ir.taqvim.app.navigation.TopLevelTab
import ir.taqvim.app.navigation.destinationTag
import ir.taqvim.app.navigation.tabTag
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun launchesTheCalendarWithKoinStarted() {
        check(GlobalContext.getOrNull() != null) { "Koin must be started by TaqvimApplication" }
        composeRule.finishOnboarding()
        composeRule.onNodeWithTag(tabTag(TopLevelTab.CALENDAR)).assertIsDisplayed()
        composeRule.onNodeWithTag(destinationTag(AppDestination.Calendar), useUnmergedTree = true).assertExists()
    }
}
