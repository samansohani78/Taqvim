/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.about

import kotlin.time.Instant

/**
 * Removes personal data from diagnostics before they are shown, copied, shared or reported (F-16): values of
 * sensitive keys (`title=`, `passphrase=`, `lat=` …), quoted text (event titles and names), e-mail addresses,
 * the path and query of links (tokens), digits beyond two decimals (coordinates) and long digit runs (phone
 * numbers, identifiers).
 */
object DiagnosticsRedactor {
    const val REDACTED: String = "[redacted]"
    private const val NUMBER = "[number]"
    private const val FRACTION_DIGITS = 2

    private val SENSITIVE_VALUE =
        Regex(
            """(?i)\b(title|name|summary|description|notes?|passphrase|password|token|secret|e-?mail|address|""" +
                """location|place|city|latitude|longitude|lat|lon|lng|query|uri|url)""" +
                """(\s*[=:]\s*)("[^"]*"|«[^»]*»|\S+)""",
        )
    private val LINK = Regex("""(?i)\b([a-z][a-z0-9+.-]*)://(?:[^\s/?#@]*@)?([^\s/?#:]*)\S*""")
    private val EMAIL = Regex("""[\p{L}\p{N}._%+-]+@[\p{L}\p{N}-]+(?:\.[\p{L}\p{N}-]+)+""")
    private val QUOTED = Regex(""""[^"]*"|«[^»]*»|“[^”]*”""")
    private val FRACTION = Regex("""(\p{Nd}+[.٫])(\p{Nd}{$FRACTION_DIGITS})\p{Nd}+""")
    private val LONG_NUMBER = Regex("""\p{Nd}{7,}""")

    fun redact(text: String): String {
        val pairs = SENSITIVE_VALUE.replace(text) { it.groupValues[1] + it.groupValues[2] + REDACTED }
        val links = LINK.replace(pairs) { "${it.groupValues[1]}://${it.groupValues[2]}/$REDACTED" }
        val quoted = QUOTED.replace(EMAIL.replace(links, REDACTED), REDACTED)
        return LONG_NUMBER.replace(shortenFractions(quoted), NUMBER)
    }

    fun redact(entry: DiagnosticEntry): DiagnosticEntry =
        entry.copy(tag = redact(entry.tag), message = redact(entry.message))

    /** Cuts every fraction to two digits; repeated because a cut can expose another long fraction. */
    private fun shortenFractions(text: String): String =
        generateSequence(text) { current ->
            FRACTION.replace(current) { it.groupValues[1] + it.groupValues[2] }.takeIf { it != current }
        }.last()
}

/** Plain-text form of diagnostics entries, one line each. */
internal object DiagnosticsFormat {
    fun time(atEpochMillis: Long): String = Instant.fromEpochMilliseconds(atEpochMillis).toString()

    fun time(entry: DiagnosticEntry): String = time(entry.atEpochMillis)

    fun line(entry: DiagnosticEntry): String = "${time(entry)} ${entry.level} ${entry.tag}: ${entry.message}"
}
