/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.about

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Koin bindings of the About screen; `:app` provides [AboutInfoSource] and [DiagnosticsSource]. */
val aboutFeatureModule: Module =
    module {
        viewModelOf(::AboutViewModel)
        factory<DeviceInfoSource> { BuildDeviceInfoSource() }
        factory<LicenseCatalogSource> { AssetLicenseCatalogSource(androidContext().assets) }
    }

/** The About screen bound to its [AboutViewModel]; [onExit] leaves the screen from its first page. */
@Composable
fun AboutRoute(
    modifier: Modifier = Modifier,
    onExit: () -> Unit = {},
    viewModel: AboutViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val actions =
        remember(viewModel, context, resources, onExit) { aboutActions(viewModel, context, resources, onExit) }
    val preview = if (state.confirmReport) viewModel.report(reportTexts(resources))?.body else null
    LaunchedEffect(viewModel, context) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AboutEffect.OpenInApp -> AboutIntents.start(context, AboutIntents.inApp(context, effect.link))
            }
        }
    }
    AboutScreen(state, actions, modifier, preview)
}

/** The FAQ questions and answers in the current configuration, for searching them. */
internal fun faqTexts(resources: Resources): Map<FaqEntry, FaqText> =
    FaqEntry.entries.associateWith { entry ->
        FaqText(
            topic = resources.getString(entry.topic.title),
            question = resources.getString(entry.question),
            answer = resources.getString(entry.answer),
        )
    }

private fun aboutActions(
    viewModel: AboutViewModel,
    context: Context,
    resources: Resources,
    onExit: () -> Unit,
): AboutActions =
    AboutActions(
        onBack = { if (!viewModel.onBack()) onExit() },
        onOpenLicenses = viewModel::onOpenLicenses,
        onOpenLicense = viewModel::onOpenLicense,
        onOpenDataLicense = viewModel::onOpenDataLicense,
        onOpenDataSources = viewModel::onOpenDataSources,
        onOpenDiagnostics = viewModel::onOpenDiagnostics,
        onMinimumLevel = viewModel::onMinimumLevel,
        onOpenLink = { url -> AboutIntents.start(context, AboutIntents.view(url)) },
        onCopyDiagnostics = {
            val label = resources.getString(R.string.about_diagnostics_clip)
            AboutIntents.copy(context, label, viewModel.diagnosticsText())
        },
        onShareDiagnostics = {
            val share = AboutIntents.share(subject = null, text = viewModel.diagnosticsText())
            AboutIntents.start(context, Intent.createChooser(share, resources.getString(R.string.about_share_title)))
        },
        onOpenFaq = { viewModel.onOpenFaq(faqTexts(resources)) },
        onFaqQuery = viewModel::onFaqQuery,
        onToggleFaq = viewModel::onToggleFaq,
        onOpenFaqLink = viewModel::onOpenFaqLink,
        onRequestReport = viewModel::onRequestReport,
        onDismissReport = viewModel::onDismissReport,
        onSendReport = {
            viewModel.report(reportTexts(resources))?.let { report ->
                val chooserTitle = resources.getString(R.string.about_report_chooser)
                AboutIntents.start(context, AboutIntents.report(report, chooserTitle))
            }
            viewModel.onReportSent()
        },
    )

/** Report texts from string resources in the current configuration. */
internal fun reportTexts(resources: Resources): ReportTexts =
    ReportTexts(
        subject = resources.getString(R.string.about_report_subject),
        app = { name, code, build -> resources.getString(R.string.about_report_app, name, code, build) },
        device = { resources.getString(R.string.about_report_device, it) },
        android = { release, sdk -> resources.getString(R.string.about_report_android, release, sdk) },
        language = { resources.getString(R.string.about_report_language, it) },
        diagnostics = { resources.getString(R.string.about_report_diagnostics, it) },
        noDiagnostics = resources.getString(R.string.about_report_no_diagnostics),
    )

/** Intents of the About screen; nothing is uploaded, every report goes through an app the user picks. */
internal object AboutIntents {
    private const val TEXT_TYPE = "text/plain"

    fun view(url: String): Intent = Intent(Intent.ACTION_VIEW, url.toUri())

    /** A `taqvim://` [link] opened by this app only (docs/AUTOMATION.md). */
    fun inApp(
        context: Context,
        link: String,
    ): Intent = view(link).setPackage(context.packageName)

    fun share(
        subject: String?,
        text: String,
    ): Intent =
        Intent(Intent.ACTION_SEND).setType(TEXT_TYPE).putExtra(Intent.EXTRA_TEXT, text).apply {
            if (subject != null) putExtra(Intent.EXTRA_SUBJECT, subject)
        }

    /** An e-mail to the report's recipient, or the share sheet when there is none, behind a chooser. */
    fun report(
        report: ProblemReport,
        chooserTitle: String,
    ): Intent {
        val recipient = report.recipient
        val send =
            if (recipient == null) {
                share(report.subject, report.body)
            } else {
                Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
                    .putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
                    .putExtra(Intent.EXTRA_SUBJECT, report.subject)
                    .putExtra(Intent.EXTRA_TEXT, report.body)
            }
        return Intent.createChooser(send, chooserTitle)
    }

    fun start(
        context: Context,
        intent: Intent,
    ): Boolean = runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess

    fun copy(
        context: Context,
        label: String,
        text: String,
    ): Boolean =
        runCatching {
            val clipboard = requireNotNull(context.getSystemService(ClipboardManager::class.java))
            clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        }.isSuccess
}
