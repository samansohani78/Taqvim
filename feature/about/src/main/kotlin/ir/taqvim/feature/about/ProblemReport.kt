/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.about

/** A problem report ready for the user's e-mail or sharing app; [recipient] is `null` when no address is known. */
data class ProblemReport(
    val recipient: String?,
    val subject: String,
    val body: String,
)

/** Localized texts of a problem report, from string resources. */
data class ReportTexts(
    val subject: String,
    val app: (versionName: String, versionCode: Long, buildType: String) -> String,
    val device: (String) -> String,
    val android: (release: String, sdkInt: Int) -> String,
    val language: (String) -> String,
    val diagnostics: (count: Int) -> String,
    val noDiagnostics: String,
    /** Heading of the stored crash, given the time it happened. */
    val crash: (time: String) -> String,
)

/**
 * Builds problem reports: app, device and language facts, the newest stored crash and the newest diagnostics, all
 * redacted (F-16).
 */
object ProblemReportComposer {
    /** Most diagnostics entries a report carries. */
    const val MAX_ENTRIES: Int = 200

    /** Longest crash text a report carries; a longer one is cut at its start, which holds the failure itself. */
    const val MAX_CRASH_CHARS: Int = 4_000

    fun compose(
        info: AboutInfo,
        device: DeviceInfo,
        entries: List<DiagnosticEntry>,
        texts: ReportTexts,
        crash: CrashReport? = null,
    ): ProblemReport {
        val facts =
            listOf(
                texts.app(info.versionName, info.versionCode, info.buildType),
                texts.device("${device.manufacturer} ${device.model}"),
                texts.android(device.androidRelease, device.sdkInt),
                texts.language(info.languageCode),
            )
        val lines = entries.take(MAX_ENTRIES).map { DiagnosticsFormat.line(DiagnosticsRedactor.redact(it)) }
        val diagnostics =
            if (lines.isEmpty()) listOf(texts.noDiagnostics) else listOf(texts.diagnostics(lines.size)) + lines
        val body = facts + crashSection(crash, texts) + "" + diagnostics
        return ProblemReport(info.links.supportEmail, texts.subject, body.joinToString("\n"))
    }

    private fun crashSection(
        crash: CrashReport?,
        texts: ReportTexts,
    ): List<String> {
        if (crash == null) return emptyList()
        val text = DiagnosticsRedactor.redact(crash.text).take(MAX_CRASH_CHARS)
        return listOf("", texts.crash(DiagnosticsFormat.time(crash.atEpochMillis)), text)
    }
}
