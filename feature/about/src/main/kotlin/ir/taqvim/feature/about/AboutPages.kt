/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import ir.taqvim.core.ui.component.EmptyState

@Composable
internal fun LicensesPage(
    content: LicensesContent,
    actions: AboutActions,
) {
    when (content) {
        LicensesContent.Loading -> {
            Loading()
        }

        LicensesContent.Unavailable -> {
            EmptyState(title = stringResource(R.string.about_licenses_unavailable))
        }

        is LicensesContent.Ready -> {
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    Text(
                        pluralStringResource(
                            R.plurals.about_licenses_count,
                            content.componentCount,
                            content.componentCount,
                        ),
                        modifier = Modifier.padding(16.dp),
                    )
                }
                content.groups.forEach { group ->
                    item(key = group.license.id) {
                        AboutRow(
                            group.license.name,
                            pluralStringResource(
                                R.plurals.about_license_modules,
                                group.modules.size,
                                group.modules.size,
                            ),
                        ) { actions.onOpenLicense(group.license.id) }
                    }
                    items(group.modules, key = { "${group.license.id}/${it.module}" }) { row ->
                        Column(Modifier.padding(horizontal = 32.dp, vertical = 4.dp)) {
                            Text(row.module, style = MaterialTheme.typography.bodyMedium.ltr())
                            Text(row.versions, style = MaterialTheme.typography.bodySmall.ltr())
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun LicenseTextPage(
    content: LicenseTextContent?,
    actions: AboutActions,
) {
    if (content == null) return
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        content.license.url?.let { url ->
            AboutRow(stringResource(R.string.about_license_open_link), url) { actions.onOpenLink(url) }
        }
        val text = content.text
        when {
            content.loading -> {
                Loading()
            }

            text != null -> {
                SelectionContainer {
                    Text(
                        text,
                        style =
                            MaterialTheme.typography.bodySmall
                                .copy(fontFamily = FontFamily.Monospace)
                                .ltr(),
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            else -> {
                Text(stringResource(R.string.about_license_text_missing), modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
internal fun DataSourcesPage(actions: AboutActions) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(DataSource.entries, key = { it.name }) { source ->
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                val title = stringResource(source.title)
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(source.description), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(source.license.label), style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Every source has this button; TalkBack names the source so they can be told apart (T-1700).
                    val openDescription = stringResource(R.string.about_data_open_source_of, title)
                    OutlinedButton(
                        onClick = { actions.onOpenLink(source.url) },
                        modifier = Modifier.semantics { contentDescription = openDescription },
                    ) {
                        Text(stringResource(R.string.about_data_open_source))
                    }
                    if (source.license == DataLicense.UNICODE_3_0) {
                        OutlinedButton(onClick = actions.onOpenDataLicense) {
                            Text(stringResource(R.string.about_data_license_text))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DiagnosticsPage(
    content: DiagnosticsContent,
    actions: AboutActions,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        item { DiagnosticsControls(content, actions) }
        if (content.rows.isEmpty()) {
            item { EmptyState(title = stringResource(R.string.about_diagnostics_empty)) }
        }
        items(content.rows) { row ->
            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Text("${row.time} · ${row.level} · ${row.tag}", style = MaterialTheme.typography.labelSmall.ltr())
                Text(row.message, style = MaterialTheme.typography.bodySmall.ltr())
            }
        }
    }
}

@Composable
private fun DiagnosticsControls(
    content: DiagnosticsContent,
    actions: AboutActions,
) {
    Column(Modifier.padding(16.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DiagnosticLevel.entries.forEach { level ->
                FilterChip(
                    selected = content.minimum == level,
                    onClick = { actions.onMinimumLevel(level) },
                    label = { Text(stringResource(level.filterLabel())) },
                )
            }
        }
        if (content.hidden > 0) {
            Text(pluralStringResource(R.plurals.about_diagnostics_hidden, content.hidden, content.hidden))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = actions.onCopyDiagnostics) { Text(stringResource(R.string.about_copy)) }
            OutlinedButton(onClick = actions.onShareDiagnostics) { Text(stringResource(R.string.about_share)) }
            OutlinedButton(onClick = actions.onRequestReport) { Text(stringResource(R.string.about_report)) }
        }
    }
}

@Composable
private fun Loading() {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/** Label resource of a level filter chip. */
internal fun DiagnosticLevel.filterLabel(): Int =
    when (this) {
        DiagnosticLevel.DEBUG -> R.string.about_level_all
        DiagnosticLevel.INFO -> R.string.about_level_info
        DiagnosticLevel.WARN -> R.string.about_level_warn
        DiagnosticLevel.ERROR -> R.string.about_level_error
    }

/** Code, versions, logs and license texts are Latin text read left to right in every layout direction. */
private fun androidx.compose.ui.text.TextStyle.ltr() = copy(textDirection = TextDirection.Ltr)
