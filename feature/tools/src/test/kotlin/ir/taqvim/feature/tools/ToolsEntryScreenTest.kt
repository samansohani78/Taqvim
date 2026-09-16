/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.core.testing.FakeClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** T-1103 entry in the UI: the Tools route opened with converter text shows it in the converter. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ToolsEntryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theConverterShowsTheLinkedText() {
        val viewModel =
            ToolsViewModel(
                { flowOf(ToolsFixtures.settings()) },
                FakeClock(ToolsFixtures.NOW),
                initialConverterText = "1 Farvardin 1405",
                computeDispatcher = Dispatchers.Main,
            )
        composeRule.setContent {
            ToolsTestTheme { ToolsRoute(converterText = "1 Farvardin 1405", viewModel = viewModel) }
        }

        composeRule.onNodeWithText("1 Farvardin 1405").assertExists()
    }
}
