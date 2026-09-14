/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.search

import android.content.Context
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.core.model.Jdn
import java.time.Duration
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

/** T-804 UI tests: typing "نور" shows Nowruz and opens it; each state shows and reports what it should. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SearchScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<SearchAction>()

    @Test
    fun typingNurShowsNowruzAndOpensIt() {
        val opened = mutableListOf<Triple<SearchEventKind, String, Jdn?>>()
        val sources = FakeSearchSources(PERSIAN_FA)
        val viewModel =
            SearchViewModel(sources, sources, sources, sources, SessionRecentQueriesStore(), Dispatchers.Main)
        val navigation = SearchNavigation(onOpenEvent = { kind, id, day -> opened += Triple(kind, id, day) })
        composeRule.setContent {
            SearchTestTheme(rtl = true) { SearchRoute(navigation = navigation, viewModel = viewModel) }
        }
        composeRule.onNodeWithTag(SEARCH_FIELD_TAG).performTextInput("نور")
        composeRule.waitUntil(timeoutMillis = 5_000) {
            // The query is debounced: let the paused main looper's clock pass.
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            composeRule.onAllNodesWithText("عید نوروز").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("عید نوروز").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            shadowOf(Looper.getMainLooper()).idle()
            opened.isNotEmpty()
        }
        assertEquals(listOf(Triple(SearchEventKind.OFFICIAL, "ir.nowruz", NOWRUZ_1405)), opened)
    }

    @Test
    fun resultsShowGroupsDetailsAndReportActions() {
        val results = sampleResults(GREGORIAN_EN, "r")
        composeRule.setContent {
            SearchTestTheme { SearchScreen(SearchUiState("r", content = results), { actions += it }) }
        }
        val heading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
        composeRule.onNode(hasText("Events") and heading).assertExists()
        composeRule.onNodeWithText("Official occasion · Saturday, March 21, 2026 · Holiday").assertIsDisplayed()
        composeRule.onNodeWithText("Go to Saturday, March 21, 2026").performClick()
        composeRule.onNodeWithText("Nature Day").performClick()
        composeRule.onNode(hasText("Tools") and hasClickAction()).performClick()
        composeRule.onNodeWithText("Clear").performClick()
        composeRule.onNodeWithTag(SEARCH_FIELD_TAG).performTextInput("b")

        val nature =
            results.sections
                .first()
                .results
                .first { it.title == "Nature Day" }
        assertEquals(
            listOf(
                SearchAction.OpenDate(results.date ?: error("no date")),
                SearchAction.OpenResult(nature),
                SearchAction.SelectFilter(SearchFilter.TOOLS),
                SearchAction.ClearQuery,
            ),
            actions.take(4),
        )
        val typed = actions.drop(4).single()
        assertTrue(typed is SearchAction.ChangeQuery && "b" in typed.text)
    }

    @Test
    fun idleSearchingAndEmptyStates() {
        var state by mutableStateOf(SearchUiState(recent = SAMPLE_RECENT))
        composeRule.setContent { SearchTestTheme { SearchScreen(state, { actions += it }) } }
        composeRule.onNodeWithText("Recent searches").assertIsDisplayed()
        composeRule.onNodeWithText("قبله").performClick()
        composeRule.onNodeWithText("Clear history").performClick()
        assertEquals(listOf(SearchAction.UseRecent("قبله"), SearchAction.ClearRecent), actions)

        state = SearchUiState()
        composeRule.onNodeWithText("Search events, tools and settings").assertIsDisplayed()
        state = SearchUiState(query = "x", content = SearchContent.Searching)
        composeRule.onNodeWithContentDescription("Searching").assertExists()
        state = SearchUiState(query = "x", content = SearchContent.NoResults)
        composeRule.onNodeWithText("No results").assertIsDisplayed()
        state =
            SearchUiState(
                query = "x",
                content = SearchContent.Results(persistentListOf(), null),
            )
        composeRule.onNodeWithTag(SEARCH_RESULTS_TAG).assertExists()
    }

    @Test
    fun catalogIsTitledFromResources() {
        val resources = ApplicationProvider.getApplicationContext<Context>().resources
        val entries = ResourceSearchCatalog(resources).entries()
        assertEquals(SettingsEntry.entries.size + ToolEntry.entries.size, entries.size)
        val language = SearchEntry(SearchTarget.Settings(SettingsEntry.LANGUAGE), "Language", LANGUAGE_KEYWORDS)
        assertEquals(language, entries[0])
        assertTrue(entries.all { it.title.isNotBlank() && it.keywords.isNotEmpty() })
        assertEquals(entries.size, entries.map { it.title }.distinct().size)
        val prayer =
            SearchRanker
                .rank("sunrise", emptyList(), entries, { "" })
                .single()
                .results
                .single()
        assertEquals(SearchTarget.Tool(ToolEntry.PRAYER_TIMES), prayer.target)
    }

    private companion object {
        val LANGUAGE_KEYWORDS = listOf("locale", "translation", "Persian", "English")
    }
}
