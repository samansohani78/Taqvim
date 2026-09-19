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

    /**
     * Longest logical line accepted, in characters: far beyond any real property (a long DESCRIPTION is a few
     * kilobytes), so only a hostile or broken file reaches it (review R04).
     */
    const val MAX_LOGICAL_CHARS: Int = 256 * 1_024

    /** Most logical lines accepted in one document; a 5 MiB subscription of short lines stays well below it. */
    const val MAX_LOGICAL_LINES: Int = 250_000

    /** Physical lines read between two calls of the reader's cancellation check. */
    const val CHECK_EVERY_LINES: Int = 4_096

    /** Logical lines of [text] with the 1-based number of their first physical line; accepts CRLF and bare LF. */
    fun unfold(text: String): List<Pair<Int, String>> = unfold(text, mutableListOf()) {}

    /**
     * Logical lines of [text], appending continuations to one [StringBuilder] so the cost is linear in the input
     * (each continuation used to copy the whole line built so far). A line over [MAX_LOGICAL_CHARS] or a document
     * over [MAX_LOGICAL_LINES] adds a problem to [errors] and ends the read; [checkCancelled] runs every
     * [CHECK_EVERY_LINES] physical lines and may throw to stop it.
     */
    fun unfold(
        text: String,
        errors: MutableList<IcsProblem>,
        checkCancelled: () -> Unit,
    ): List<Pair<Int, String>> {
        val unfolder = Unfolder(errors)
        var number = 0
        var start = 0
        while (start <= text.length && errors.isEmpty()) {
            val end = text.indexOf('\n', start).let { if (it < 0) text.length else it }
            val stop = if (end > start && text[end - 1] == '\r') end - 1 else end
            number++
            if (number % CHECK_EVERY_LINES == 0) checkCancelled()
            if (stop > start) unfolder.physical(text, start, stop, number)
            start = end + 1
        }
        return unfolder.finish()
    }

    /** The logical lines built so far and the one being built. */
    private class Unfolder(
        private val errors: MutableList<IcsProblem>,
    ) {
        private val logical = mutableListOf<Pair<Int, String>>()
        private val current = StringBuilder()
        private var first = 0

        /** Adds the non-empty physical line `text[start until stop]`, numbered [number]. */
        fun physical(
            text: String,
            start: Int,
            stop: Int,
            number: Int,
        ) {
            if (current.isNotEmpty() && isContinuation(text[start])) {
                current.append(text, start + 1, stop)
                if (current.length > MAX_LOGICAL_CHARS) {
                    errors += IcsProblem(first, "content line longer than $MAX_LOGICAL_CHARS characters")
                }
                return
            }
            flush()
            if (logical.size >= MAX_LOGICAL_LINES) {
                errors += IcsProblem(number, "more than $MAX_LOGICAL_LINES content lines")
            }
            current.append(text, start, stop)
            first = number
        }

        fun finish(): List<Pair<Int, String>> {
            if (errors.isEmpty()) flush()
            return logical
        }

        private fun flush() {
            if (current.isNotEmpty()) logical += first to current.toString()
            current.setLength(0)
        }
    }

    fun isContinuation(char: Char): Boolean = char == ' ' || char == '\t'

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
