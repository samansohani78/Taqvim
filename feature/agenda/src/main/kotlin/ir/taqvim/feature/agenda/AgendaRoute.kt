/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.agenda

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.taqvim.core.model.Jdn
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin bindings of the month list and agenda (ADR-0002). `:app` provides [AgendaSettingsSource], [AgendaDaySource]
 * and `TodayProvider`.
 */
val agendaFeatureModule: Module =
    module {
        factory<AgendaTodaySource> { TickingAgendaTodaySource(get()) }
        viewModel { AgendaViewModel(get(), get(), get(), Dispatchers.Default) }
    }

/** Where the list leads; the app's navigation provides these. */
@Immutable
class AgendaNavigation(
    val onOpenDay: (day: Jdn) -> Unit = {},
    val onOpenEvent: (event: AgendaEvent) -> Unit = {},
)

/** The list bound to its [AgendaViewModel]; prints through `PrintManager` and shares as plain text. */
@Composable
fun AgendaRoute(
    modifier: Modifier = Modifier,
    navigation: AgendaNavigation = AgendaNavigation(),
    viewModel: AgendaViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentNavigation by rememberUpdatedState(navigation)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AgendaEffect.NavigateToDay -> currentNavigation.onOpenDay(effect.jdn)
                is AgendaEffect.NavigateToEvent -> currentNavigation.onOpenEvent(effect.event)
            }
        }
    }
    val context = LocalContext.current
    val texts = agendaTexts()
    val jobName = stringResource(R.string.agenda_print_job)
    val title = stringResource(R.string.agenda_share)
    val actions =
        AgendaScreenActions(
            onAction = viewModel::onAction,
            onPrint = { state.content?.let { AgendaPrinter.print(context, AgendaExport.html(it, texts), jobName) } },
            onShare = { state.content?.let { AgendaSharer.share(context, AgendaExport.text(it, texts), title) } },
        )
    AgendaScreen(state, actions, modifier)
}

/** Prints an HTML document: renders it in an off-screen [WebView], then hands its print adapter to `PrintManager`. */
internal object AgendaPrinter {
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

/** Shares plain text through the system share sheet. */
internal object AgendaSharer {
    /** The share-sheet intent for [text] titled [title]. */
    fun intent(
        text: String,
        title: String,
    ): Intent {
        val send =
            Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, text)
                .putExtra(Intent.EXTRA_SUBJECT, title)
        return Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun share(
        context: Context,
        text: String,
        title: String,
    ) {
        context.startActivity(intent(text, title))
    }
}
