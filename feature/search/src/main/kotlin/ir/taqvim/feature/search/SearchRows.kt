/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.taqvim.core.ui.component.EmptyState
import kotlinx.collections.immutable.ImmutableList

/** Recent searches as chips, or a hint when there are none. */
@Composable
internal fun RecentQueries(
    recent: ImmutableList<String>,
    onAction: (SearchAction) -> Unit,
) {
    if (recent.isEmpty()) {
        EmptyState(title = stringResource(R.string.search_idle))
        return
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.search_recent),
                modifier = Modifier.weight(1f).semantics { heading() },
                style = MaterialTheme.typography.titleSmall,
            )
            TextButton(onClick = { onAction(SearchAction.ClearRecent) }) {
                Text(stringResource(R.string.search_clear_recent))
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            recent.forEach { query ->
                AssistChip(onClick = { onAction(SearchAction.UseRecent(query)) }, label = { Text(query) })
            }
        }
    }
}

/** "Go to <date>" for a query that reads as a date. */
@Composable
internal fun DateRow(
    date: DateSuggestion,
    onAction: (SearchAction) -> Unit,
) {
    TextButton(onClick = { onAction(SearchAction.OpenDate(date)) }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.search_go_to_date, date.label), modifier = Modifier.fillMaxWidth())
    }
}

/** The header of a result group, announced as a heading. */
@Composable
internal fun SectionHeader(group: SearchGroup) {
    Text(
        stringResource(SearchFilter.entries.first { it.group == group }.label()),
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp).semantics { heading() },
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

/** One result: its title with the query highlighted, and what it is (event kind, day, holiday, matched synonym). */
@Composable
internal fun ResultRow(
    result: SearchResult,
    onAction: (SearchAction) -> Unit,
) {
    val details = details(result)
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = stringResource(R.string.search_open),
                role = Role.Button,
            ) { onAction(SearchAction.OpenResult(result)) }
            .padding(vertical = 10.dp),
    ) {
        Text(highlighted(result.title, result.highlight, MaterialTheme.colorScheme.primary))
        if (details.isNotEmpty()) {
            Text(
                details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun details(result: SearchResult): String {
    val parts =
        listOfNotNull(
            result.eventKind?.let { stringResource(it.label()) },
            result.dayLabel,
            if (result.isHoliday) stringResource(R.string.search_holiday) else null,
            result.matchedAlias,
        )
    return parts.joinToString(stringResource(R.string.search_separator))
}

/** [title] with the characters in [range] emphasized. */
internal fun highlighted(
    title: String,
    range: IntRange?,
    color: Color,
): AnnotatedString =
    buildAnnotatedString {
        append(title)
        if (range != null && range.first >= 0 && range.last < title.length) {
            addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = color), range.first, range.last + 1)
        }
    }

private fun SearchEventKind.label(): Int =
    when (this) {
        SearchEventKind.OFFICIAL -> R.string.search_kind_official
        SearchEventKind.PERSONAL -> R.string.search_kind_personal
        SearchEventKind.DEVICE -> R.string.search_kind_device
        SearchEventKind.SUBSCRIPTION -> R.string.search_kind_subscription
    }
