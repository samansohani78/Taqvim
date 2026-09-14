/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import ir.taqvim.core.ui.component.EmptyState

/** The in-app FAQ (T-1901): a search field, questions grouped by topic, and a way to report a problem. */
@Composable
internal fun FaqPage(
    content: FaqContent,
    actions: AboutActions,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        item(key = "search") {
            OutlinedTextField(
                value = content.query,
                onValueChange = actions.onFaqQuery,
                label = { Text(stringResource(R.string.about_faq_search)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        }
        if (content.noResults) {
            item(key = "empty") { EmptyState(title = stringResource(R.string.about_faq_no_results)) }
        }
        content.groups.forEach { group ->
            item(key = group.topic.name) {
                Text(
                    stringResource(group.topic.title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).semantics { heading() },
                )
            }
            items(group.entries, key = { it.name }) { entry -> FaqItem(entry, entry in content.expanded, actions) }
        }
        item(key = "help") { FaqHelp(actions) }
    }
}

@Composable
private fun FaqItem(
    entry: FaqEntry,
    expanded: Boolean,
    actions: AboutActions,
) {
    val state = stringResource(if (expanded) R.string.about_faq_expanded else R.string.about_faq_collapsed)
    Column {
        ListItem(
            headlineContent = { Text(stringResource(entry.question)) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier =
                Modifier
                    .clickable(role = Role.Button) { actions.onToggleFaq(entry) }
                    .semantics { stateDescription = state },
        )
        if (expanded) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                Text(stringResource(entry.answer), style = MaterialTheme.typography.bodyMedium)
                if (entry.link != null) {
                    OutlinedButton(
                        onClick = { actions.onOpenFaqLink(entry) },
                        modifier = Modifier.padding(top = 8.dp),
                    ) { Text(stringResource(R.string.about_faq_open)) }
                }
            }
        }
    }
}

@Composable
private fun FaqHelp(actions: AboutActions) {
    Column(Modifier.padding(16.dp)) {
        Text(stringResource(R.string.about_faq_more_help), style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = actions.onRequestReport, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.about_report))
        }
    }
}
