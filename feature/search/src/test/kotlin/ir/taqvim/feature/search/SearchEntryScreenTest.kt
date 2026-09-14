/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.search

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** T-1103 entry in the UI: the search route opened with a query shows it in the search field. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SearchEntryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theFieldShowsTheLinkedQuery() {
        val sources = FakeSearchSources(PERSIAN_FA)
        val viewModel =
            SearchViewModel(sources, sources, sources, sources, SessionRecentQueriesStore(), Dispatchers.Main, "نور")
        composeRule.setContent {
            SearchTestTheme(rtl = true) { SearchRoute(initialQuery = "نور", viewModel = viewModel) }
        }

        composeRule.onNodeWithTag(SEARCH_FIELD_TAG).assert(hasText("نور"))
    }
}
