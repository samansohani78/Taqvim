/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.agenda

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.core.model.Jdn
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** T-901 UI tests: scrolling a hundred rows and back, what each state shows, and the actions. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AgendaScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<AgendaAction>()

    private fun show(
        state: AgendaUiState,
        screenActions: AgendaScreenActions = AgendaScreenActions(onAction = { actions += it }),
    ) {
        composeRule.setContent { AgendaTestTheme { AgendaScreen(state, screenActions) } }
    }

    private fun assertShown(item: AgendaItem) {
        val text =
            when (item) {
                is AgendaMonthHeader -> "${item.title.name} ${item.title.year}"
                is AgendaDayRow -> item.longDate
                is AgendaEmptyMonth -> error("the month list has no empty months")
            }
        composeRule.onNodeWithText(text, substring = true).assertIsDisplayed()
    }

    @Test
    fun scrollsAHundredRowsAndBack() {
        val content = sampleContent(mode = AgendaMode.MONTH_LIST)
        assertTrue(content.items.size > 100)
        show(AgendaUiState(content))
        val list = composeRule.onNodeWithTag(AGENDA_LIST_TAG)
        assertShown(content.items[content.todayIndex])
        (0..100 step 5).forEach { index ->
            list.performScrollToIndex(index)
            assertShown(content.items[index])
        }
        (100 downTo 0 step 5).forEach { index ->
            list.performScrollToIndex(index)
            assertShown(content.items[index])
        }
        list.performScrollToIndex(content.items.lastIndex)
        composeRule.waitForIdle()
        assertTrue(AgendaAction.LoadEarlier in actions)
        assertTrue(AgendaAction.LoadLater in actions)
    }

    @Test
    fun actionsAreReported() {
        val calls = mutableListOf<String>()
        val content = sampleContent()
        val screenActions =
            AgendaScreenActions(
                onAction = { actions += it },
                onPrint = { calls += "print" },
                onShare = { calls += "share" },
            )
        show(AgendaUiState(content), screenActions)
        composeRule.onNodeWithText("All days").performClick()
        composeRule.onNodeWithText("Today").performClick()
        composeRule.onNodeWithText("Print").performClick()
        composeRule.onNodeWithText("Share").performClick()
        val list = composeRule.onNodeWithTag(AGENDA_LIST_TAG)
        list.performScrollToKey("day-${(TODAY + 2).value}")
        composeRule.onNodeWithContentDescription("Birthday, Personal").performClick()
        list.performScrollToKey("day-${(TODAY + 40).value}")
        composeRule.onNodeWithText("Friday, October 23, 2026", substring = true).performClick()

        assertEquals(listOf("print", "share"), calls)
        val reported = actions.filterNot { it == AgendaAction.LoadEarlier || it == AgendaAction.LoadLater }
        assertEquals(
            listOf(
                AgendaAction.SelectMode(AgendaMode.MONTH_LIST),
                AgendaAction.GoToToday,
                AgendaAction.OpenEvent(SAMPLE_EVENTS.getValue(TODAY + 2).last()),
                AgendaAction.OpenDay(TODAY + 40),
            ),
            reported,
        )
    }

    @Test
    fun todayHolidaysAndEmptyMonthsAreShown() {
        show(AgendaUiState(sampleContent()))
        composeRule.onNode(isSelected() and hasText("Today", substring = true)).assertExists()
        composeRule.onNodeWithText("Sunday, September 13, 2026 · Today").assertIsDisplayed()
        composeRule.onNodeWithText("No events").assertIsDisplayed()
        composeRule.onNodeWithText("Tuesday, September 15, 2026 · Holiday").assertIsDisplayed()
        composeRule.onNodeWithTag(AGENDA_LIST_TAG).performScrollToIndex(0)
        composeRule.onNodeWithText("August 2026").assertIsDisplayed()
        composeRule.onNodeWithText("No events this month").assertIsDisplayed()
    }

    @Test
    fun loadingStatesAreAnnounced() {
        var state by mutableStateOf(AgendaUiState())
        composeRule.setContent { AgendaTestTheme { AgendaScreen(state, AgendaScreenActions()) } }
        composeRule.onNodeWithContentDescription("Loading the agenda").assertExists()
        state = AgendaUiState(sampleContent(isLoading = true))
        composeRule.onNodeWithContentDescription("Loading more months").assertExists()
    }

    @Test
    fun routeShowsTheListAndNavigates() {
        val opened = mutableListOf<Jdn>()
        val sources = FakeAgendaSources(GREGORIAN_EN)
        val viewModel = AgendaViewModel(sources, sources, sources, Dispatchers.Main)
        composeRule.setContent {
            AgendaTestTheme {
                AgendaRoute(navigation = AgendaNavigation(onOpenDay = { opened += it }), viewModel = viewModel)
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Sunday, September 13, 2026 · Today").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Sunday, September 13, 2026 · Today").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { opened.isNotEmpty() }
        assertEquals(listOf(TODAY), opened)
    }

    @Test
    fun shareIntentCarriesTheText() {
        val intent = AgendaSharer.intent("Agenda text", "Share")
        assertEquals(Intent.ACTION_CHOOSER, intent.action)
        val send = intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        assertEquals("Agenda text", send?.getStringExtra(Intent.EXTRA_TEXT))
        assertEquals("text/plain", send?.type)
    }
}
