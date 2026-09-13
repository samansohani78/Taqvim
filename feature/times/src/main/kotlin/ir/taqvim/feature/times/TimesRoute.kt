/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.times

import android.content.Context
import android.content.res.Resources
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Koin bindings of the Times tab; `:app` provides [TimesSettingsSource] and `kotlin.time.Clock`. */
val timesFeatureModule: Module =
    module {
        viewModelOf(::TimesViewModel)
    }

/** The Times tab bound to its [TimesViewModel]; the monthly report is printed through `PrintManager`. */
@Composable
fun TimesRoute(
    modifier: Modifier = Modifier,
    viewModel: TimesViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val actions =
        remember(viewModel, context, resources) {
            TimesActions(
                onPreviousDay = viewModel::onPreviousDay,
                onNextDay = viewModel::onNextDay,
                onToday = viewModel::onToday,
                onToggleExpanded = viewModel::onToggleExpanded,
                onPrintReport = {
                    viewModel.monthlyReportHtml(reportLabels(resources))?.let { html ->
                        ReportPrinter.print(context, html, resources.getString(R.string.times_report_job))
                    }
                },
            )
        }
    TimesScreen(state, actions, modifier)
}

/** Report texts from string resources in the current configuration. */
internal fun reportLabels(resources: Resources): ReportLabels =
    ReportLabels(
        title = { place, from, to -> resources.getString(R.string.times_report_title, place, from, to) },
        date = resources.getString(R.string.times_report_date),
        prayers = PrayerKind.entries.associateWith { resources.getString(it.label) },
        undefined = resources.getString(R.string.times_undefined),
        polarDay = resources.getString(R.string.times_polar_day),
        polarNight = resources.getString(R.string.times_polar_night),
    )

/** Prints an HTML document: renders it in an off-screen [WebView], then hands its print adapter to `PrintManager`. */
internal object ReportPrinter {
    fun print(
        context: Context,
        html: String,
        jobName: String,
    ) {
        val webView = WebView(context)
        webView.webViewClient =
            object : WebViewClient() {
                override fun onPageFinished(
                    view: WebView,
                    url: String?,
                ) {
                    context.getSystemService(PrintManager::class.java)?.print(
                        jobName,
                        view.createPrintDocumentAdapter(jobName),
                        PrintAttributes.Builder().build(),
                    )
                }
            }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }
}
