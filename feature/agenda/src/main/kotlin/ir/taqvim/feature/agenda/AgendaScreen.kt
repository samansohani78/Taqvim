/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.agenda

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ir.taqvim.core.ui.component.ScreenSurface
import ir.taqvim.core.ui.component.SegmentedTabs
import ir.taqvim.core.ui.component.TopBar
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

/** Test tag of the lazy list. */
const val AGENDA_LIST_TAG: String = "agenda_list"

/** User actions of the screen; printing and sharing need the texts and the platform, so the route handles them. */
@Immutable
data class AgendaScreenActions(
    val onAction: (AgendaAction) -> Unit = {},
    val onPrint: () -> Unit = {},
    val onShare: () -> Unit = {},
)

/** The month list and agenda (T-901), stateless: renders [state] and reports user actions through [actions]. */
@Composable
fun AgendaScreen(
    state: AgendaUiState,
    actions: AgendaScreenActions,
    modifier: Modifier = Modifier,
) {
    ScreenSurface(
        modifier = modifier,
        topBar = { TopBar(stringResource(R.string.agenda_title)) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val content = state.content
            if (content == null) {
                val description = stringResource(R.string.agenda_loading)
                CircularProgressIndicator(
                    Modifier.align(Alignment.Center).semantics { contentDescription = description },
                )
            } else {
                AgendaBody(content, actions)
            }
        }
    }
}

@Composable
private fun AgendaBody(
    content: AgendaContent,
    actions: AgendaScreenActions,
) {
    Column(Modifier.fillMaxSize()) {
        Toolbar(content, actions)
        if (content.isLoading) {
            val description = stringResource(R.string.agenda_loading_more)
            LinearProgressIndicator(Modifier.fillMaxWidth().semantics { contentDescription = description })
        }
        AgendaList(content, actions.onAction, Modifier.weight(1f))
    }
}

@Composable
private fun Toolbar(
    content: AgendaContent,
    actions: AgendaScreenActions,
) {
    val modes = listOf(stringResource(R.string.agenda_mode_events), stringResource(R.string.agenda_mode_all_days))
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SegmentedTabs(modes, content.mode.ordinal, onSelect = { index ->
            actions.onAction(AgendaAction.SelectMode(AgendaMode.entries[index]))
        })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { actions.onAction(AgendaAction.GoToToday) }) {
                Text(stringResource(R.string.agenda_today))
            }
            Row {
                TextButton(onClick = actions.onPrint) { Text(stringResource(R.string.agenda_print)) }
                TextButton(onClick = actions.onShare) { Text(stringResource(R.string.agenda_share)) }
            }
        }
    }
}

@Composable
private fun AgendaList(
    content: AgendaContent,
    onAction: (AgendaAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val texts = agendaTexts()
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = content.todayIndex.coerceAtLeast(0))
    val latestContent by rememberUpdatedState(content)
    val latestOnAction by rememberUpdatedState(onAction)
    LaunchedEffect(content.scrollToTodayRequest) {
        val index = snapshotFlow { latestContent.todayIndex }.first { it >= 0 }
        listState.scrollToItem(index)
    }
    LaunchedEffect(listState) { reportEdges(listState) { latestOnAction(it) } }
    LazyColumn(modifier.fillMaxSize().testTag(AGENDA_LIST_TAG), state = listState) {
        items(content.items, key = { it.key }, contentType = { it::class }) { item ->
            when (item) {
                is AgendaMonthHeader -> MonthHeaderRow(item, texts)
                is AgendaEmptyMonth -> EmptyMonthRow(texts)
                is AgendaDayRow -> DayRow(item, texts, onAction)
            }
        }
    }
}

/** Asks for earlier or later months whenever the first or last [EDGE_ROWS] rows come into view. */
private suspend fun reportEdges(
    listState: LazyListState,
    onAction: (AgendaAction) -> Unit,
) {
    snapshotFlow {
        val info = listState.layoutInfo
        val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
        Edges(
            atStart = info.totalItemsCount > 0 && listState.firstVisibleItemIndex < EDGE_ROWS,
            atEnd = info.totalItemsCount > 0 && last >= info.totalItemsCount - EDGE_ROWS,
            count = info.totalItemsCount,
        )
    }.distinctUntilChanged().collect { edges ->
        if (edges.atStart) onAction(AgendaAction.LoadEarlier)
        if (edges.atEnd) onAction(AgendaAction.LoadLater)
    }
}

private data class Edges(
    val atStart: Boolean,
    val atEnd: Boolean,
    val count: Int,
)

private const val EDGE_ROWS = 2
