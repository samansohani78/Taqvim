/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.about

import kotlin.time.Instant

/**
 * Removes personal data from diagnostics before they are shown, copied, shared, reported or stored (F-16): the whole
 * value of a sensitive key (`title=`, `عنوان=`, `Authorization:` …) up to the end of its line or field, bearer tokens,
 * quoted text (event titles and names), e-mail addresses, the path and query of links (tokens), digits beyond two
 * decimals (coordinates) and long digit runs (phone numbers, identifiers).
 */
object DiagnosticsRedactor {
    const val REDACTED: String = "[redacted]"
    private const val NUMBER = "[number]"
    private const val FRACTION_DIGITS = 2

    /**
     * Keys whose value is free text — a title, a note, a place, an authorization header (review R03). Their value
     * goes whole, up to the end of its line or field. The same keys in the app's other languages are read from
     * [LOCALIZED_KEYS], a data file, since text in their scripts may not appear in code.
     */
    private val TEXT_KEYS: List<String> by lazy {
        listOf(
            "title",
            "name",
            "summary",
            "description",
            "notes?",
            "address",
            "location",
            "place",
            "city",
            "authorization",
            "proxy-authorization",
            "cookie",
            "set-cookie",
            "credentials?",
        ) +
            localizedKeys()
    }

    /** Absolute classpath path of the localized keys (a relative one breaks under R8 repackaging). */
    private const val LOCALIZED_KEYS = "/ir/taqvim/feature/about/sensitive-keys.txt"

    /** The localized keys, quoted for a pattern; none when the file cannot be read, so redaction never fails. */
    private fun localizedKeys(): List<String> =
        runCatching {
            DiagnosticsRedactor::class.java
                .getResourceAsStream(LOCALIZED_KEYS)
                ?.bufferedReader(Charsets.UTF_8)
                ?.useLines { lines ->
                    lines.map(String::trim).filter { it.isNotEmpty() && !it.startsWith("#") }.toList()
                }.orEmpty()
                .map(Regex::escape)
        }.getOrDefault(emptyList())

    /** Keys whose value is one token — a secret, a link, a coordinate — so the words after it are kept. */
    private val TOKEN_KEYS =
        listOf(
            "passphrase",
            "password",
            "passwd",
            "secret",
            "token",
            "auth",
            "session(?:[-_]?id)?",
            "x-api-key",
            "api[-_]?key",
            "access[-_]?token",
            "refresh[-_]?token",
            "id[-_]?token",
            "otp",
            "pin",
            "e-?mail",
            "latitude",
            "longitude",
            "lat",
            "lon",
            "lng",
            "query",
            "uri",
            "url",
        )

    /** The key must not continue a longer word, so `rename=` and `tokenizer:` are kept. */
    private fun keyed(
        keys: List<String>,
        value: String,
    ) = Regex("""(?i)(?<![\p{L}\p{N}_-])(${keys.joinToString("|")})(\s*[=:]\s*)("[^"]*"|«[^»]*»|$value)""")

    private val TEXT_VALUE by lazy { keyed(TEXT_KEYS, """[^;&\r\n]*[^;&\r\n\s]""") }
    private val TOKEN_VALUE = keyed(TOKEN_KEYS, """[^\s;&]+""")
    private val BEARER = Regex("""(?i)\b(bearer)\s+[^\s;&]+""")
    private val LINK = Regex("""(?i)\b([a-z][a-z0-9+.-]*)://(?:[^\s/?#@]*@)?([^\s/?#:]*)\S*""")
    private val EMAIL = Regex("""[\p{L}\p{N}._%+-]+@[\p{L}\p{N}-]+(?:\.[\p{L}\p{N}-]+)+""")
    private val QUOTED = Regex(""""[^"]*"|«[^»]*»|“[^”]*”""")
    private val FRACTION = Regex("""(\p{Nd}+[.٫])(\p{Nd}{$FRACTION_DIGITS})\p{Nd}+""")
    private val LONG_NUMBER = Regex("""\p{Nd}{7,}""")

    fun redact(text: String): String {
        val pairs =
            listOf(TEXT_VALUE, TOKEN_VALUE)
                .fold(text) { current, pattern ->
                    pattern.replace(current) { it.groupValues[1] + it.groupValues[2] + REDACTED }
                }.let { keyed -> BEARER.replace(keyed) { it.groupValues[1] + " " + REDACTED } }
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
