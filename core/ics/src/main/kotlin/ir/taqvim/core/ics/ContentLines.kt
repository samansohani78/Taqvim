/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ics

/** One unfolded content line (RFC 5545 §3.1): upper-cased [name], upper-cased parameter names and the raw [value]. */
internal data class ContentLine(
    val line: Int,
    val name: String,
    val parameters: Map<String, List<String>>,
    val value: String,
) {
    fun parameter(name: String): String? = parameters[name]?.firstOrNull()
}

/** Content-line syntax: unfolding, folding, parameters and TEXT escaping (§3.1, §3.2, §3.3.11). */
internal object ContentLines {
    /** Maximum octets of one physical line, excluding the line break (§3.1). */
    const val MAX_OCTETS: Int = 75

    private val NAME = Regex("[A-Za-z0-9-]+")
    private const val ONE_BYTE_LIMIT = 0x80
    private const val TWO_BYTE_LIMIT = 0x800
    private const val THREE_BYTE_LIMIT = 0x10000
    private const val TWO_OCTETS = 2
    private const val THREE_OCTETS = 3
    private const val FOUR_OCTETS = 4

    /** Logical lines of [text] with the 1-based number of their first physical line; accepts CRLF and bare LF. */
    fun unfold(text: String): List<Pair<Int, String>> {
        val logical = mutableListOf<Pair<Int, String>>()
        text.split('\n').forEachIndexed { index, rawLine ->
            val physical = rawLine.removeSuffix("\r")
            val previous = logical.lastOrNull()
            if (previous != null && isContinuation(physical)) {
                logical[logical.size - 1] = previous.first to previous.second + physical.substring(1)
            } else if (physical.isNotEmpty()) {
                logical += (index + 1) to physical
            }
        }
        return logical
    }

    private fun isContinuation(physical: String): Boolean =
        physical.isNotEmpty() && (physical[0] == ' ' || physical[0] == '\t')

    /** Physical lines of at most [MAX_OCTETS] UTF-8 octets for [line], never splitting a character (§3.1). */
    fun fold(line: String): List<String> {
        val physical = mutableListOf<String>()
        val current = StringBuilder()
        var octets = 0
        var offset = 0
        while (offset < line.length) {
            val codePoint = line.codePointAt(offset)
            val width = utf8Width(codePoint)
            if (octets + width > MAX_OCTETS) {
                physical += current.toString()
                current.setLength(0)
                current.append(' ')
                octets = 1
            }
            current.appendCodePoint(codePoint)
            octets += width
            offset += Character.charCount(codePoint)
        }
        physical += current.toString()
        return physical
    }

    /** Number of UTF-8 octets of [codePoint]. */
    fun utf8Width(codePoint: Int): Int =
        when {
            codePoint < ONE_BYTE_LIMIT -> 1
            codePoint < TWO_BYTE_LIMIT -> TWO_OCTETS
            codePoint < THREE_BYTE_LIMIT -> THREE_OCTETS
            else -> FOUR_OCTETS
        }

    /** Parses `name *(";" param) ":" value`; `null` when the line is malformed. */
    fun parse(
        line: Int,
        text: String,
    ): ContentLine? {
        val nameEnd = text.indexOfFirst { it == ';' || it == ':' }
        val name = if (nameEnd > 0) text.substring(0, nameEnd) else ""
        if (!NAME.matches(name)) return null
        val (parameters, colon) = scanParameters(text, nameEnd) ?: return null
        return ContentLine(line, name.uppercase(), parameters, text.substring(colon + 1))
    }

    private data class Parameter(
        val name: String,
        val values: List<String>,
        val end: Int,
    )

    /** Parameters from [start] (at a `;` or `:`) and the index of the `:` that precedes the value. */
    private fun scanParameters(
        text: String,
        start: Int,
    ): Pair<Map<String, List<String>>, Int>? {
        val parameters = LinkedHashMap<String, List<String>>()
        var index = start
        while (index in text.indices && text[index] == ';') {
            val parameter = parameter(text, index + 1) ?: return null
            parameters[parameter.name] = parameter.values
            index = parameter.end
        }
        return if (index in text.indices && text[index] == ':') parameters to index else null
    }

    private fun parameter(
        text: String,
        start: Int,
    ): Parameter? {
        val equals = text.indexOf('=', start)
        if (equals <= start || !NAME.matches(text.substring(start, equals))) return null
        val values = mutableListOf<String>()
        var index = equals
        do {
            val (value, end) = parameterValue(text, index + 1) ?: return null
            values += value
            index = end
        } while (index in text.indices && text[index] == ',')
        return Parameter(text.substring(start, equals).uppercase(), values, index)
    }

    /** A quoted or unquoted parameter value starting at [start] and the index just after it. */
    private fun parameterValue(
        text: String,
        start: Int,
    ): Pair<String, Int>? =
        if (start < text.length && text[start] == '"') {
            val close = text.indexOf('"', start + 1)
            if (close < 0) null else text.substring(start + 1, close) to close + 1
        } else {
            val end = (start until text.length).firstOrNull { text[it] in ";:," } ?: text.length
            text.substring(start, end) to end
        }

    /** TEXT value with `\\`, `\;`, `\,` and `\n`/`\N` escapes resolved (§3.3.11). */
    fun unescapeText(value: String): String =
        buildString {
            var index = 0
            while (index < value.length) {
                val char = value[index]
                if (char == '\\' && index + 1 < value.length) {
                    val next = value[index + 1]
                    append(if (next == 'n' || next == 'N') '\n' else next)
                    index += 2
                } else {
                    append(char)
                    index++
                }
            }
        }

    /** [value] escaped as a TEXT value (§3.3.11); carriage returns are dropped. */
    fun escapeText(value: String): String =
        buildString {
            value.replace("\r", "").forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    ';' -> append("\\;")
                    ',' -> append("\\,")
                    '\n' -> append("\\n")
                    else -> append(char)
                }
            }
        }
}
