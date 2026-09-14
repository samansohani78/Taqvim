/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ir.taqvim.core.ui.component.ScreenSurface
import ir.taqvim.core.ui.component.SegmentedTabs
import ir.taqvim.core.ui.component.TopBar

/** Settings home (T-1500), stateless: renders [state] and reports user actions through [actions]. */
@Composable
fun SettingsHomeScreen(
    state: SettingsHomeUiState,
    actions: SettingsHomeActions,
    modifier: Modifier = Modifier,
) {
    ScreenSurface(
        modifier = modifier,
        topBar = { TopBar(stringResource(R.string.settings_home_title)) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.loading) {
                val description = stringResource(R.string.settings_home_loading)
                CircularProgressIndicator(
                    Modifier.align(Alignment.Center).semantics { contentDescription = description },
                )
            } else {
                SettingsHomeContent(state, actions)
            }
        }
    }
    state.dialog?.let { ChoiceDialogView(it, actions) }
    state.confirmation?.let { ToggleConfirmationView(it, actions) }
}

@Composable
private fun SettingsHomeContent(
    state: SettingsHomeUiState,
    actions: SettingsHomeActions,
) {
    val searching = state.query.isNotBlank()
    val visible = rememberVisibleRows(state)
    val listState = rememberLazyListState()
    LaunchedEffect(state.highlighted, visible) {
        val index = visible.indexOfFirst { it.id == state.highlighted }
        if (index >= 0) listState.scrollToItem(index)
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = actions.onQueryChanged,
            label = { Text(stringResource(R.string.settings_search_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        if (!searching) {
            SegmentedTabs(
                tabs = SettingsTab.entries.map { stringResource(it.title) },
                selectedIndex = state.tab.ordinal,
                onSelect = { actions.onTabSelected(SettingsTab.entries[it]) },
            )
        }
        if (searching && visible.isEmpty()) {
            Text(stringResource(R.string.settings_search_no_results, state.query))
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(visible, key = { it.id }) { row ->
                SettingsRowItem(
                    row = row,
                    highlighted = row.id == state.highlighted,
                    showTab = searching,
                    onClick = actions.onRowClicked,
                )
            }
        }
    }
}

/** The rows of the selected tab, or the search results across all tabs while a query is typed. */
@Composable
private fun rememberVisibleRows(state: SettingsHomeUiState): List<SettingsRow> {
    val resources = LocalResources.current
    return remember(state.rows, state.query, state.tab, resources) {
        if (state.query.isNotBlank()) {
            state.rows.filter { row ->
                SettingsSearch.matches(
                    state.query,
                    resources.getString(row.id.title),
                    resources.getString(row.id.keywords),
                )
            }
        } else {
            state.rows.filter { it.id.tab == state.tab }
        }
    }
}
