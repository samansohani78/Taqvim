/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.search

import ir.taqvim.core.i18n.FuzzyMatcher
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.i18n.PersianText

/** How a text matched the query, from strongest to weakest. */
enum class MatchKind {
    /** The whole text equals the query. */
    EXACT,

    /** The text starts with the query. */
    PREFIX,

    /** A later word of the text starts with the query. */
    WORD_PREFIX,

    /** The query occurs inside the text. */
    SUBSTRING,

    /** The text or one of its words is within a few edits of the query. */
    FUZZY,
}

/** The best match of a query in one of an item's texts; a higher [score] is better. */
data class TextMatch(
    val text: String,
    val kind: MatchKind,
    val score: Int,
)

/**
 * Query matching for the unified search (T-804, F-12). Texts are compared by [key]: the T-203 search key (Arabic and
 * Persian letter variants, diacritics, ZWNJ, spaces and case do not matter) with every decimal digit folded to ASCII,
 * so "۱۴۰۵" finds "1405". Scores follow the kinds; within a kind, shorter completions and earlier positions win.
 */
object SearchMatcher {
    /** Shortest query (in key characters) matched with typos. */
    const val MIN_FUZZY_LENGTH: Int = 4

    /** Shortest query that tolerates two edits instead of one. */
    const val LONG_QUERY_LENGTH: Int = 5

    private const val EXACT_SCORE = 5_000
    private const val PREFIX_SCORE = 4_000
    private const val WORD_PREFIX_SCORE = 3_000
    private const val SUBSTRING_SCORE = 2_000
    private const val FUZZY_SCORE = 1_000
    private const val EDIT_PENALTY = 100
    private const val MAX_PENALTY = 999

    /** The comparison key of [text]. Idempotent. */
    fun key(text: String): String =
        buildString {
            PersianText.searchKey(text).forEach { char -> append(Numerals.digitValue(char)?.let { '0' + it } ?: char) }
        }

    /** The best match of [query] among [texts], or `null` when the query is blank or nothing matches. */
    fun match(
        query: String,
        texts: List<String>,
    ): TextMatch? {
        val queryKey = key(query)
        if (queryKey.isEmpty()) return null
        return texts.mapNotNull { matchText(queryKey, it) }.maxWithOrNull(compareBy { it.score })
    }

    /**
     * The characters of [text] that the literal occurrence of [query] covers, or `null` when [query] is blank or does
     * not occur literally (typo matches are not highlighted).
     */
    fun highlight(
        text: String,
        query: String,
    ): IntRange? {
        val queryKey = key(query)
        val keyChars = StringBuilder()
        val origins = mutableListOf<Int>()
        text.forEachIndexed { index, char ->
            key(char.toString()).forEach {
                keyChars.append(it)
                origins += index
            }
        }
        val start = if (queryKey.isEmpty()) -1 else keyChars.indexOf(queryKey)
        return if (start < 0) null else origins[start]..origins[start + queryKey.length - 1]
    }

    private fun matchText(
        queryKey: String,
        text: String,
    ): TextMatch? {
        val textKey = key(text)
        val literal = if (textKey.isEmpty()) null else literal(queryKey, textKey, text)
        return literal ?: fuzzy(queryKey, text)
    }

    private fun literal(
        queryKey: String,
        textKey: String,
        text: String,
    ): TextMatch? {
        val position = textKey.indexOf(queryKey)
        val wordStart = wordKeys(text).any { it.startsWith(queryKey) }
        return when {
            position < 0 -> null
            textKey == queryKey -> TextMatch(text, MatchKind.EXACT, EXACT_SCORE)
            position == 0 -> TextMatch(text, MatchKind.PREFIX, PREFIX_SCORE - band(textKey.length - queryKey.length))
            wordStart -> TextMatch(text, MatchKind.WORD_PREFIX, WORD_PREFIX_SCORE - band(position))
            else -> TextMatch(text, MatchKind.SUBSTRING, SUBSTRING_SCORE - band(position))
        }
    }

    private fun fuzzy(
        queryKey: String,
        text: String,
    ): TextMatch? {
        if (queryKey.length < MIN_FUZZY_LENGTH) return null
        val maxDistance = if (queryKey.length >= LONG_QUERY_LENGTH) FuzzyMatcher.DEFAULT_MAX_DISTANCE else 1
        val distance =
            (wordKeys(text) + key(text))
                .distinct()
                .minOfOrNull { FuzzyMatcher.distance(queryKey, it, maxDistance) }
                ?: return null
        val score = FUZZY_SCORE - distance * EDIT_PENALTY
        return if (distance > maxDistance) null else TextMatch(text, MatchKind.FUZZY, score)
    }

    private fun wordKeys(text: String): List<String> =
        PersianText
            .normalize(text)
            .split(' ')
            .map(::key)
            .filter { it.isNotEmpty() }

    private fun band(penalty: Int): Int = minOf(penalty, MAX_PENALTY)
}
