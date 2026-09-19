/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import ir.taqvim.core.ui.component.ScreenSurface
import ir.taqvim.core.ui.component.TopBar
import ir.taqvim.core.ui.component.TopBarAction

/** The stateless About screen; [reportPreview] is the report text shown while the user confirms it. */
@Composable
fun AboutScreen(
    state: AboutUiState,
    actions: AboutActions,
    modifier: Modifier = Modifier,
    reportPreview: String? = null,
) {
    ScreenSurface(modifier, topBar = { AboutTopBar(state, actions) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state.page) {
                AboutPage.HOME -> AboutHome(state.info, state.crash, actions)
                AboutPage.LICENSES -> LicensesPage(state.licenses, actions)
                AboutPage.LICENSE_TEXT -> LicenseTextPage(state.licenseText, actions)
                AboutPage.DATA_SOURCES -> DataSourcesPage(actions)
                AboutPage.DIAGNOSTICS -> DiagnosticsPage(state.diagnostics, actions)
                AboutPage.CRASH -> CrashPage(state.crash, actions)
                AboutPage.FAQ -> FaqPage(state.faq, actions)
            }
        }
    }
    if (state.confirmReport) ReportDialog(reportPreview, actions)
}

@Composable
private fun AboutTopBar(
    state: AboutUiState,
    actions: AboutActions,
) {
    val title =
        when (state.page) {
            AboutPage.HOME -> {
                stringResource(R.string.about_title)
            }

            AboutPage.LICENSES -> {
                stringResource(R.string.about_licenses)
            }

            AboutPage.LICENSE_TEXT -> {
                state.licenseText
                    ?.license
                    ?.name
                    .orEmpty()
            }

            AboutPage.DATA_SOURCES -> {
                stringResource(R.string.about_data_sources)
            }

            AboutPage.DIAGNOSTICS -> {
                stringResource(R.string.about_diagnostics)
            }

            AboutPage.CRASH -> {
                stringResource(R.string.about_crash)
            }

            AboutPage.FAQ -> {
                stringResource(R.string.about_faq)
            }
        }
    val back =
        TopBarAction(
            icon = ImageVector.vectorResource(R.drawable.about_ic_back),
            label = stringResource(R.string.about_back),
            onClick = actions.onBack,
        )
    TopBar(title = title, navigation = back.takeIf { state.canGoBack })
}

@Composable
private fun AboutHome(
    info: AboutInfo?,
    crash: CrashContent,
    actions: AboutActions,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.about_app_name),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            if (info != null) {
                Text(stringResource(R.string.about_version, info.versionName, info.versionCode, info.buildType))
            }
        }
        HomeLinks(info?.links, actions)
        AboutRow(stringResource(R.string.about_faq), stringResource(R.string.about_faq_summary)) {
            actions.onOpenFaq()
        }
        AboutRow(stringResource(R.string.about_licenses), stringResource(R.string.about_licenses_summary)) {
            actions.onOpenLicenses()
        }
        AboutRow(stringResource(R.string.about_data_sources), stringResource(R.string.about_data_sources_summary)) {
            actions.onOpenDataSources()
        }
        AboutRow(stringResource(R.string.about_diagnostics), stringResource(R.string.about_diagnostics_summary)) {
            actions.onOpenDiagnostics()
        }
        // Only a run that crashed has anything to show, so the entry stays hidden until then.
        if (crash.rows.isNotEmpty()) {
            AboutRow(stringResource(R.string.about_crash), stringResource(R.string.about_crash_summary)) {
                actions.onOpenCrash()
            }
        }
        AboutRow(stringResource(R.string.about_report), stringResource(R.string.about_report_summary)) {
            actions.onRequestReport()
        }
    }
}

@Composable
private fun HomeLinks(
    links: AboutLinks?,
    actions: AboutActions,
) {
    val entries =
        listOfNotNull(
            links?.website?.let { R.string.about_website to it },
            links?.sourceCode?.let { R.string.about_source_code to it },
            links?.privacyPolicy?.let { R.string.about_privacy_policy to it },
        )
    entries.forEach { (label, url) ->
        AboutRow(stringResource(label), url) { actions.onOpenLink(url) }
    }
}

/** A clickable list row with a title and an optional summary. */
@Composable
internal fun AboutRow(
    title: String,
    summary: String?,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = summary?.let { { Text(it) } },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
    )
}

@Composable
private fun ReportDialog(
    preview: String?,
    actions: AboutActions,
) {
    AlertDialog(
        onDismissRequest = actions.onDismissReport,
        title = { Text(stringResource(R.string.about_report_title)) },
        text = {
            Column {
                Text(stringResource(R.string.about_report_explanation))
                if (preview != null) {
                    Text(
                        preview,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier =
                            Modifier
                                .padding(top = 12.dp)
                                .heightIn(max = 240.dp)
                                .verticalScroll(rememberScrollState()),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = actions.onSendReport, enabled = preview != null) {
                Text(stringResource(R.string.about_report_send))
            }
        },
        dismissButton = {
            TextButton(onClick = actions.onDismissReport) { Text(stringResource(R.string.about_report_cancel)) }
        },
    )
}
