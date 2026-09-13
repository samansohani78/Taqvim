/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import ir.taqvim.core.i18n.Numerals
import java.math.BigDecimal
import java.util.Locale

/** A token of the duration grammar at character [position]. */
internal sealed interface DurationToken {
    val position: Int

    data class Number(
        val value: BigDecimal,
        override val position: Int,
    ) : DurationToken

    data class UnitWord(
        val unit: DurationUnitWord,
        override val position: Int,
    ) : DurationToken

    /** One of `+ - * / ( )`, after mapping `−`, `×` and `÷`. */
    data class Symbol(
        val char: Char,
        override val position: Int,
    ) : DurationToken
}

/** Tokens of a whole text, or the first character that is not part of the grammar. */
internal sealed interface Tokenized {
    data class Tokens(
        val tokens: List<DurationToken>,
    ) : Tokenized

    data class Failure(
        val error: DurationError,
    ) : Tokenized

    fun <T> fold(
        onTokens: (List<DurationToken>) -> T,
        onError: (DurationError) -> T,
    ): T =
        when (this) {
            is Tokens -> onTokens(tokens)
            is Failure -> onError(error)
        }
}

internal object DurationTokenizer {
    private const val SYMBOLS = "+-*/()"
    private const val ZERO_WIDTH_NON_JOINER = '‌'
    private val ALIASES = mapOf('−' to '-', '×' to '*', '÷' to '/')
    private val DECIMAL_SEPARATORS = setOf('.', '٫')

    // Grammar keywords matched against the user's input; they are never displayed.
    @Suppress("NoHardcodedNonLatinText")
    private val CONNECTORS = setOf("and", "و")

    @Suppress("NoHardcodedNonLatinText")
    private val UNITS: Map<String, DurationUnitWord> =
        mapOf(
            DurationUnitWord.WEEK to listOf("w", "wk", "week", "weeks", "هفته"),
            DurationUnitWord.DAY to listOf("d", "day", "days", "روز"),
            DurationUnitWord.HOUR to listOf("h", "hr", "hrs", "hour", "hours", "ساعت"),
            // Persian spellings, also with the Arabic yeh (U+064A) of Arabic keyboards.
            DurationUnitWord.MINUTE to listOf("m", "min", "mins", "minute", "minutes", "دقیقه", "دقيقه"),
            DurationUnitWord.SECOND to listOf("s", "sec", "secs", "second", "seconds", "ثانیه", "ثانيه"),
        ).flatMap { (unit, words) -> words.map { it to unit } }.toMap()

    fun tokenize(text: String): Tokenized {
        val tokens = ArrayList<DurationToken>()
        var index = 0
        while (index < text.length) {
            val char = text[index]
            val symbol = ALIASES[char] ?: char.takeIf { it in SYMBOLS }
            val next =
                when {
                    char.isWhitespace() || char == ZERO_WIDTH_NON_JOINER -> {
                        index + 1
                    }

                    Numerals.digitValue(char) != null -> {
                        number(text, index, tokens)
                    }

                    char.isLetter() -> {
                        word(text, index, tokens)
                    }

                    symbol != null -> {
                        tokens += DurationToken.Symbol(symbol, index)
                        index + 1
                    }

                    else -> {
                        null
                    }
                }
            index = next ?: return Tokenized.Failure(DurationError.UnexpectedCharacter(index))
        }
        return Tokenized.Tokens(tokens)
    }

    /** Reads the number starting at [start]; returns the index after it. */
    private fun number(
        text: String,
        start: Int,
        tokens: MutableList<DurationToken>,
    ): Int {
        val digits = StringBuilder()
        var index = start
        var seenSeparator = false
        while (index < text.length) {
            val digit = Numerals.digitValue(text[index])
            val separator =
                !seenSeparator && text[index] in DECIMAL_SEPARATORS &&
                    text.getOrNull(index + 1)?.let(Numerals::digitValue) != null
            if (digit == null && !separator) break
            digits.append(digit?.let { '0' + it } ?: '.')
            seenSeparator = seenSeparator || separator
            index++
        }
        tokens += DurationToken.Number(BigDecimal(digits.toString()), start)
        return index
    }

    /** Reads the word starting at [start]; returns the index after it, or `null` when it is not a unit or connector. */
    private fun word(
        text: String,
        start: Int,
        tokens: MutableList<DurationToken>,
    ): Int? {
        var index = start
        while (index < text.length && text[index].isLetter()) index++
        val word = text.substring(start, index).lowercase(Locale.ROOT)
        val unit = UNITS[word]
        if (unit != null) tokens += DurationToken.UnitWord(unit, start)
        return index.takeIf { unit != null || word in CONNECTORS }
    }
}
