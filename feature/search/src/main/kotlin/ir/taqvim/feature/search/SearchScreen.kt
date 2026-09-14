/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ir.taqvim.core.ui.component.EmptyState
import ir.taqvim.core.ui.component.ScreenSurface
import ir.taqvim.core.ui.component.SegmentedTabs
import ir.taqvim.core.ui.component.TopBar

/** Test tag of the query field. */
const val SEARCH_FIELD_TAG: String = "search_field"

/** Test tag of the result list. */
const val SEARCH_RESULTS_TAG: String = "search_results"

/** The unified search (T-804), stateless: renders [state] and reports user actions through [onAction]. */
@Composable
fun SearchScreen(
    state: SearchUiState,
    onAction: (SearchAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenSurface(
        modifier = modifier,
        topBar = { TopBar(stringResource(R.string.search_title)) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QueryField(state.query, onAction)
            val filters = SearchFilter.entries.map { stringResource(it.label()) }
            SegmentedTabs(filters, state.filter.ordinal, onSelect = { index ->
                onAction(SearchAction.SelectFilter(SearchFilter.entries[index]))
            })
            when (val content = state.content) {
                SearchContent.Idle -> RecentQueries(state.recent, onAction)
                SearchContent.Searching -> Searching()
                SearchContent.NoResults -> NoResults()
                is SearchContent.Results -> ResultList(content, onAction, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QueryField(
    query: String,
    onAction: (SearchAction) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = { onAction(SearchAction.ChangeQuery(it)) },
        modifier = Modifier.fillMaxWidth().testTag(SEARCH_FIELD_TAG),
        label = { Text(stringResource(R.string.search_hint)) },
        singleLine = true,
        trailingIcon =
            if (query.isEmpty()) {
                null
            } else {
                {
                    TextButton(onClick = { onAction(SearchAction.ClearQuery) }) {
                        Text(stringResource(R.string.search_clear))
                    }
                }
            },
    )
}

@Composable
private fun Searching() {
    val description = stringResource(R.string.search_searching)
    LinearProgressIndicator(Modifier.fillMaxWidth().semantics { contentDescription = description })
}

@Composable
private fun NoResults() {
    EmptyState(
        title = stringResource(R.string.search_no_results),
        message = stringResource(R.string.search_no_results_message),
    )
}

@Composable
private fun ResultList(
    content: SearchContent.Results,
    onAction: (SearchAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxWidth().testTag(SEARCH_RESULTS_TAG)) {
        content.date?.let { date ->
            item(key = "date") { DateRow(date, onAction) }
        }
        content.sections.forEach { section ->
            item(key = "header-${section.group}") { SectionHeader(section.group) }
            items(section.results, key = { it.key }) { result -> ResultRow(result, onAction) }
        }
    }
}

/** The label of a filter; group sections reuse it as their header. */
internal fun SearchFilter.label(): Int =
    when (this) {
        SearchFilter.ALL -> R.string.search_filter_all
        SearchFilter.EVENTS -> R.string.search_filter_events
        SearchFilter.TOOLS -> R.string.search_filter_tools
        SearchFilter.SETTINGS -> R.string.search_filter_settings
    }
