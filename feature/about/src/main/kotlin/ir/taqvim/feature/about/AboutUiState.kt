/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.about

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** Pages of the About screen. */
enum class AboutPage {
    HOME,
    LICENSES,
    LICENSE_TEXT,
    DATA_SOURCES,
    DIAGNOSTICS,
    FAQ,
}

/** One-shot effects of the About screen. */
sealed interface AboutEffect {
    /** Open a `taqvim://` [link] of docs/AUTOMATION.md inside the app. */
    data class OpenInApp(
        val link: String,
    ) : AboutEffect
}

/** State of the About screen (T-1504). */
data class AboutUiState(
    val page: AboutPage = AboutPage.HOME,
    /** Whether the page has a page to return to inside the screen. */
    val canGoBack: Boolean = false,
    val info: AboutInfo? = null,
    val licenses: LicensesContent = LicensesContent.Loading,
    val licenseText: LicenseTextContent? = null,
    val diagnostics: DiagnosticsContent = DiagnosticsContent(),
    /** The problem report confirmation is shown. */
    val confirmReport: Boolean = false,
    /** The in-app FAQ (T-1901). */
    val faq: FaqContent = FaqContent(),
)

/** The open-source license list. */
sealed interface LicensesContent {
    data object Loading : LicensesContent

    data object Unavailable : LicensesContent

    data class Ready(
        val groups: ImmutableList<LicenseGroup>,
        val componentCount: Int,
    ) : LicensesContent
}

/** One license text; [text] is `null` while [loading] or when the text is not bundled. */
data class LicenseTextContent(
    val license: LicenseInfo,
    val text: String?,
    val loading: Boolean,
)

/** Diagnostics at or above [minimum], already redacted; [hidden] entries are filtered out. */
data class DiagnosticsContent(
    val minimum: DiagnosticLevel = DiagnosticLevel.DEBUG,
    val rows: ImmutableList<DiagnosticRow> = persistentListOf(),
    val hidden: Int = 0,
)

/** One redacted diagnostics entry. */
data class DiagnosticRow(
    val time: String,
    val level: DiagnosticLevel,
    val tag: String,
    val message: String,
)

/** User actions of the About screen. */
@Immutable
data class AboutActions(
    val onBack: () -> Unit = {},
    val onOpenLicenses: () -> Unit = {},
    val onOpenLicense: (String) -> Unit = {},
    val onOpenDataLicense: () -> Unit = {},
    val onOpenDataSources: () -> Unit = {},
    val onOpenDiagnostics: () -> Unit = {},
    val onMinimumLevel: (DiagnosticLevel) -> Unit = {},
    val onOpenLink: (String) -> Unit = {},
    val onCopyDiagnostics: () -> Unit = {},
    val onShareDiagnostics: () -> Unit = {},
    val onRequestReport: () -> Unit = {},
    val onDismissReport: () -> Unit = {},
    val onSendReport: () -> Unit = {},
    val onOpenFaq: () -> Unit = {},
    val onFaqQuery: (String) -> Unit = {},
    val onToggleFaq: (FaqEntry) -> Unit = {},
    val onOpenFaqLink: (FaqEntry) -> Unit = {},
)
