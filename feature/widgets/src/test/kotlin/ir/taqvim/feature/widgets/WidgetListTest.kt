/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.core.i18n.TextDirection
import ir.taqvim.core.ui.theme.TaqvimTheme
import ir.taqvim.core.ui.theme.ThemeMode
import ir.taqvim.core.ui.theme.ThemeSettings
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** T-1200: the widget settings screen — the placed widgets, and the empty state when there are none. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetListTest {
    @get:Rule
    val composeRule = createComposeRule()

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun eachPlacedWidgetIsNamedAndOpensItsConfiguration() {
        val opened = mutableListOf<Int>()
        val placed =
            persistentListOf(
                PlacedWidget(11, WidgetKind.DAY_SUMMARY_2X2),
                PlacedWidget(12, WidgetKind.MOON),
            )
        show(WidgetListUiState(loading = false, placed = placed)) { opened += it.appWidgetId }

        composeRule.onNodeWithText("Today").assertIsDisplayed()
        composeRule.onNodeWithText("Moon phase").performClick()

        assertEquals(listOf(12), opened)
    }

    @Test
    fun withNothingPlacedTheScreenSaysHowToAddOne() {
        // Only the launcher can add a widget, so the empty state explains that instead of offering a list to add from.
        show(WidgetListUiState(loading = false))

        composeRule.onNodeWithText("No widgets placed").assertIsDisplayed()
        composeRule
            .onNodeWithText("Add a Taqvim widget from your home screen first; its settings appear here afterwards.")
            .assertIsDisplayed()
    }

    @Test
    fun theViewModelListsEveryPlacedWidgetInKindOrder(): Unit =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val installed =
                FakeInstalledWidgets(
                    mapOf(
                        WidgetKind.MOON to setOf(7, 3),
                        WidgetKind.DATE_1X1 to setOf(5),
                    ),
                )

            val viewModel = WidgetListViewModel(installed)
            runCurrent()

            // Grouped by kind in the catalogue's order, then by id, so the list does not reshuffle between visits.
            assertEquals(
                listOf(
                    PlacedWidget(5, WidgetKind.DATE_1X1),
                    PlacedWidget(3, WidgetKind.MOON),
                    PlacedWidget(7, WidgetKind.MOON),
                ),
                viewModel.uiState.value.placed,
            )
            assertEquals(false, viewModel.uiState.value.loading)
        }

    private fun show(
        state: WidgetListUiState,
        onConfigure: (PlacedWidget) -> Unit = {},
    ) {
        composeRule.setContent { TestTheme { WidgetListScreen(state, WidgetListActions(onConfigure)) } }
    }

    @Composable
    private fun TestTheme(content: @Composable () -> Unit) {
        TaqvimTheme(ThemeSettings(mode = ThemeMode.LIGHT, dynamicColor = false), TextDirection.LTR, content = content)
    }
}
