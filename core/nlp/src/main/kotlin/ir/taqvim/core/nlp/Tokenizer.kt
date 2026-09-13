/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.nlp

import ir.taqvim.core.i18n.Numerals

/** Lexical classes of the date grammar. */
internal enum class TokenType {
    /** A run of decimal digits of any supported script. */
    NUMBER,

    /** A run of letters, combining marks and zero-width (non-)joiners. */
    WORD,

    /** A date field separator: `/`, `-`, `.`, `٫`, an en dash or a bidi mark. */
    SEPARATOR,

    /** Any other visible character, e.g. a comma. */
    PUNCTUATION,
}

/** A token of the input with its character range [start] (inclusive) to [end] (exclusive). */
internal data class Token(
    val type: TokenType,
    val raw: String,
    val start: Int,
    val end: Int,
) {
    /** Normalized matching key of a word (see [Lexicon.key]); the raw text for other tokens. */
    val key: String = if (type == TokenType.WORD) Lexicon.key(raw) else raw

    /** Value of a number token of at most nine digits, otherwise `null`. */
    val number: Int? =
        if (type == TokenType.NUMBER && raw.length <= MAX_DIGITS) {
            raw.fold(0) { value, char -> value * DECIMAL + (Numerals.digitValue(char) ?: 0) }
        } else {
            null
        }

    private companion object {
        const val MAX_DIGITS = 9
        const val DECIMAL = 10
    }
}

/** Splits text into [Token]s; whitespace separates tokens and is dropped. */
internal object Tokenizer {
    private const val ZERO_WIDTH_NON_JOINER = '‌'
    private const val ZERO_WIDTH_JOINER = '‍'
    private val SEPARATORS = setOf('/', '-', '.', '٫', '–', '‎', '‏', '؜')
    private val MARKS =
        setOf(
            Character.NON_SPACING_MARK.toInt(),
            Character.COMBINING_SPACING_MARK.toInt(),
            Character.ENCLOSING_MARK.toInt(),
        )

    fun tokenize(text: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var index = 0
        while (index < text.length) {
            val type = typeOf(text[index])
            val end = if (type == TokenType.NUMBER || type == TokenType.WORD) runEnd(text, index, type) else index + 1
            if (type != null) tokens += Token(type, text.substring(index, end), index, end)
            index = end
        }
        return tokens
    }

    /** Whether [char] belongs to a word. */
    fun isWordChar(char: Char): Boolean =
        char.isLetter() || char == ZERO_WIDTH_NON_JOINER || char == ZERO_WIDTH_JOINER ||
            Character.getType(char) in MARKS

    private fun typeOf(char: Char): TokenType? =
        when {
            Numerals.digitValue(char) != null -> TokenType.NUMBER
            char in SEPARATORS -> TokenType.SEPARATOR
            char.isWhitespace() -> null
            isWordChar(char) -> TokenType.WORD
            else -> TokenType.PUNCTUATION
        }

    private fun runEnd(
        text: String,
        start: Int,
        type: TokenType,
    ): Int {
        var end = start + 1
        while (end < text.length && typeOf(text[end]) == type) end++
        return end
    }
}
