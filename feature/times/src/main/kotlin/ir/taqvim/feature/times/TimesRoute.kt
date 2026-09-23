/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.times

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
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

/**
 * Prints an HTML document: renders it in an off-screen [WebView], then hands its print adapter to `PrintManager`.
 *
 * `WebView.createPrintDocumentAdapter` only keeps working while the [WebView] it came from is alive; Android's own
 * docs warn the caller must hold a reference until printing is done, because nothing else does — the print spooler
 * calls back into the adapter, which calls back into the [WebView], from another process, on its own schedule. A
 * local variable does not survive [print] returning, so before this fix the [WebView] (and the Activity [context] it
 * was built from) were one GC pause away from a blank or failed print job.
 *
 * The fix gives the [WebView] a real GC root instead of an artificial one: it is added, at zero size, as a child of
 * the printing Activity's own decor view, so it is exactly as reachable as that Activity already is (this doubles as
 * the fix some WebView versions need to render at all when never attached to a window). A local `release()` detaches
 * and destroys it once the print framework is done, one way or another (`onFinish` fires on success, on failure and
 * when the user cancels), or once loading itself fails to produce a job to hand off — a global registry was
 * considered and rejected (docs/PLAN.md §0.2 forbids object-level mutable state; `NoGlobalMutableState` in `:lint`
 * enforces it) because it would keep every in-flight WebView reachable for the life of the process, not just its own
 * Activity.
 */
internal object ReportPrinter {
    fun print(
        context: Context,
        html: String,
        jobName: String,
        newWebView: (Context) -> WebView = ::WebView,
    ) {
        val container = context.findActivity()?.window?.decorView as? ViewGroup ?: return
        val webView = newWebView(context)
        container.addView(webView, 0, 0)
        var released = false

        fun release() {
            if (released) return
            released = true
            container.removeView(webView)
            webView.destroy()
        }
        webView.webViewClient =
            object : WebViewClient() {
                override fun onPageFinished(
                    view: WebView,
                    url: String?,
                ) {
                    val printManager = context.getSystemService(PrintManager::class.java)
                    if (printManager == null) {
                        release()
                        return
                    }
                    // If the WebView cannot build an adapter, that is handled the same way an unavailable print
                    // service already is: release it instead of leaving the report open with nothing printed.
                    val adapter = runCatching { view.createPrintDocumentAdapter(jobName) }.getOrNull()
                    if (adapter == null) {
                        release()
                        return
                    }
                    printManager.print(
                        jobName,
                        ReleasingPrintDocumentAdapter(adapter, ::release),
                        PrintAttributes.Builder().build(),
                    )
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError,
                ) {
                    // The report is a self-contained loadDataWithBaseURL document with no navigation, so this should
                    // never fire; if it somehow does, release the WebView instead of leaking it forever.
                    if (request.isForMainFrame) release()
                }
            }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    /** The Activity that owns [this], if any: a [WebView] needs one to render, not just any [Context]. */
    private tailrec fun Context.findActivity(): Activity? =
        when (this) {
            is Activity -> this
            is ContextWrapper -> baseContext.findActivity()
            else -> null
        }

    /** Delegates every call to [delegate]; [onFinished] then releases the [WebView] the adapter was built from. */
    private class ReleasingPrintDocumentAdapter(
        private val delegate: PrintDocumentAdapter,
        private val onFinished: () -> Unit,
    ) : PrintDocumentAdapter() {
        override fun onStart() = delegate.onStart()

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes?,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback?,
            extras: Bundle?,
        ) = delegate.onLayout(oldAttributes, newAttributes, cancellationSignal, callback, extras)

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor?,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback?,
        ) = delegate.onWrite(pages, destination, cancellationSignal, callback)

        // Called exactly once, whether the job printed, failed, or was cancelled (PrintDocumentAdapter#onFinish).
        override fun onFinish() {
            delegate.onFinish()
            onFinished()
        }
    }
}
