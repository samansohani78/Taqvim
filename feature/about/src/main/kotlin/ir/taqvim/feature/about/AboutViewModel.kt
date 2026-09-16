/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.taqvim.core.model.attempt
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The About screen (T-1504): app facts and links, the bundled open-source licenses, data sources, redacted local
 * diagnostics with a level filter, and the problem report composed only after the user confirms (F-16).
 */
class AboutViewModel(
    infoSource: AboutInfoSource,
    diagnosticsSource: DiagnosticsSource,
    private val licenseSource: LicenseCatalogSource,
    private val deviceSource: DeviceInfoSource,
) : ViewModel() {
    private val pages = MutableStateFlow(listOf(AboutPage.HOME))
    private val minimum = MutableStateFlow(DiagnosticLevel.DEBUG)
    private val licenses = MutableStateFlow<LicensesContent>(LicensesContent.Loading)
    private val licensesRequested = MutableStateFlow(false)
    private val licenseText = MutableStateFlow<LicenseTextContent?>(null)
    private val confirmReport = MutableStateFlow(false)
    private val latestInfo = MutableStateFlow<AboutInfo?>(null)
    private val latestEntries = MutableStateFlow<List<DiagnosticEntry>>(emptyList())
    private val faqTexts = MutableStateFlow<Map<FaqEntry, FaqText>>(emptyMap())
    private val faqQuery = MutableStateFlow("")
    private val faqExpanded = MutableStateFlow<Set<FaqEntry>>(emptySet())
    private val effectChannel = Channel<AboutEffect>(Channel.BUFFERED)

    /** One-shot effects, such as opening an answer's screen of the app. */
    val effects: Flow<AboutEffect> = effectChannel.receiveAsFlow()

    private val faq = combine(faqTexts, faqQuery, faqExpanded, ::faqContent)

    private val navigation =
        combine(pages, licenses, licenseText, confirmReport, faq) { stack, list, text, confirm, faqContent ->
            NavigationPart(stack, list, text, confirm, faqContent)
        }
    private val data =
        combine(
            infoSource.about().onEach { latestInfo.value = it },
            diagnosticsSource.recent(DIAGNOSTICS_LIMIT).onEach { latestEntries.value = it },
            minimum,
        ) { info, entries, level -> DataPart(info, diagnosticsContent(entries, level)) }

    val uiState: StateFlow<AboutUiState> =
        combine(data, navigation) { part, nav ->
            AboutUiState(
                page = nav.pages.last(),
                canGoBack = nav.pages.size > 1,
                info = part.info,
                licenses = nav.licenses,
                licenseText = nav.licenseText,
                diagnostics = part.diagnostics,
                confirmReport = nav.confirmReport,
                faq = nav.faq,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AboutUiState())

    fun onOpenLicenses() {
        push(AboutPage.LICENSES)
        if (licensesRequested.compareAndSet(expect = false, update = true)) {
            viewModelScope.launch {
                licenses.value =
                    attempt { licenseSource.catalog() }
                        .fold(
                            onSuccess = { LicensesContent.Ready(it.groups(), it.components.size) },
                            onFailure = { LicensesContent.Unavailable },
                        )
            }
        }
    }

    /** Opens the text of the listed license [id]; ignored until the list is loaded. */
    fun onOpenLicense(id: String) {
        val list = licenses.value
        val license =
            if (list is LicensesContent.Ready) list.groups.firstOrNull { it.license.id == id }?.license else null
        if (license != null) openText(license)
    }

    /** Opens the Unicode License v3 of the CLDR data. */
    fun onOpenDataLicense() {
        openText(DataSource.UNICODE_LICENSE)
    }

    fun onOpenDataSources() {
        push(AboutPage.DATA_SOURCES)
    }

    fun onOpenDiagnostics() {
        push(AboutPage.DIAGNOSTICS)
    }

    /** Opens the FAQ; [texts] are its questions and answers in the current language, searched by [onFaqQuery]. */
    fun onOpenFaq(texts: Map<FaqEntry, FaqText>) {
        faqTexts.value = texts
        push(AboutPage.FAQ)
    }

    fun onFaqQuery(query: String) {
        faqQuery.value = query
    }

    /** Expands [entry], or collapses it when it is expanded. */
    fun onToggleFaq(entry: FaqEntry) {
        faqExpanded.update { if (entry in it) it - entry else it + entry }
    }

    /** Opens the screen of the app that [entry]'s answer points to; entries without a link do nothing. */
    fun onOpenFaqLink(entry: FaqEntry) {
        entry.link?.let { effectChannel.trySend(AboutEffect.OpenInApp(it)) }
    }

    /** Returns to the previous page; `false` on the first page, where leaving the screen is the caller's. */
    fun onBack(): Boolean {
        confirmReport.value = false
        val handled = pages.value.size > 1
        if (handled) pages.update { it.dropLast(1) }
        return handled
    }

    fun onMinimumLevel(level: DiagnosticLevel) {
        minimum.value = level
    }

    fun onRequestReport() {
        confirmReport.value = true
    }

    fun onDismissReport() {
        confirmReport.value = false
    }

    /** The shown diagnostics as redacted plain text, for copy and share. */
    fun diagnosticsText(): String =
        latestEntries.value
            .filter { it.level >= minimum.value }
            .joinToString("\n") { DiagnosticsFormat.line(DiagnosticsRedactor.redact(it)) }

    /** The problem report of the current facts and diagnostics; `null` until the app facts are known. */
    fun report(texts: ReportTexts): ProblemReport? =
        latestInfo.value?.let { ProblemReportComposer.compose(it, deviceSource.device(), latestEntries.value, texts) }

    /** The confirmed report was handed to the user's app. */
    fun onReportSent() {
        confirmReport.value = false
    }

    private fun push(page: AboutPage) {
        pages.update { if (it.last() == page) it else it + page }
    }

    private fun openText(license: LicenseInfo) {
        push(AboutPage.LICENSE_TEXT)
        val asset = license.textAsset
        licenseText.value = LicenseTextContent(license, text = null, loading = asset != null)
        if (asset == null) return
        viewModelScope.launch {
            val text = attempt { licenseSource.text(asset) }.getOrNull()
            licenseText.update { current ->
                if (current?.license?.id == license.id) current.copy(text = text, loading = false) else current
            }
        }
    }

    private data class NavigationPart(
        val pages: List<AboutPage>,
        val licenses: LicensesContent,
        val licenseText: LicenseTextContent?,
        val confirmReport: Boolean,
        val faq: FaqContent,
    )

    private data class DataPart(
        val info: AboutInfo,
        val diagnostics: DiagnosticsContent,
    )

    companion object {
        /** Diagnostics entries loaded for the page and the report. */
        const val DIAGNOSTICS_LIMIT: Int = 500
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/** Redacted diagnostics rows at or above [minimum]. */
internal fun diagnosticsContent(
    entries: List<DiagnosticEntry>,
    minimum: DiagnosticLevel,
): DiagnosticsContent {
    val shown = entries.filter { it.level >= minimum }
    val rows =
        shown.map { entry ->
            val redacted = DiagnosticsRedactor.redact(entry)
            DiagnosticRow(DiagnosticsFormat.time(entry), entry.level, redacted.tag, redacted.message)
        }
    return DiagnosticsContent(minimum, rows.toImmutableList(), hidden = entries.size - shown.size)
}
