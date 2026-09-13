/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

/**
 * Persian text normalization for display-independent comparison and search (T-203, F-12).
 *
 * Characters are referenced by code point so that no user-facing script appears in Kotlin sources.
 */
public object PersianText {
    private const val ARABIC_YEH = 'ي'
    private const val ALEF_MAKSURA = 'ى'
    private const val FARSI_YEH = 'ی'
    private const val ARABIC_KAF = 'ك'
    private const val KEHEH = 'ک'
    private const val TATWEEL = 'ـ'
    private const val ZERO_WIDTH_NON_JOINER = '‌'
    private const val ZERO_WIDTH_JOINER = '‍'
    private const val ALEF = 'ا'
    private const val WAW = 'و'

    /** Alef with madda/hamza above/hamza below, and alef wasla — folded to plain alef for search keys. */
    private val ALEF_VARIANTS = setOf('آ', 'أ', 'إ', 'ٱ')
    private const val WAW_WITH_HAMZA = 'ؤ'
    private const val YEH_WITH_HAMZA = 'ئ'

    /** Arabic combining marks: tanween/harakat/shadda/sukun (U+064B–U+065F), superscript alef, Quranic marks. */
    private fun isDiacritic(char: Char): Boolean =
        char in 'ً'..'ٟ' || char == 'ٰ' || char in 'ۖ'..'ۜ' || char in '۟'..'ۤ' ||
            char == 'ۧ' || char == 'ۨ' || char in '۪'..'ۭ'

    /**
     * Canonical Persian form: Arabic yeh (ي) and alef maksura (ى) → Farsi yeh (ی), Arabic kaf (ك) → keheh (ک),
     * tatweel and diacritics removed, zero-width joiners removed, zero-width non-joiners and all whitespace runs
     * collapsed to a single space, and the result trimmed. Idempotent.
     */
    public fun normalize(text: String): String {
        val builder = StringBuilder(text.length)
        var pendingSpace = false
        for (char in text) {
            if (char == ZERO_WIDTH_NON_JOINER || char.isWhitespace()) {
                pendingSpace = builder.isNotEmpty()
            } else if (!isIgnorable(char)) {
                if (pendingSpace) builder.append(' ')
                pendingSpace = false
                builder.append(canonicalLetter(char))
            }
        }
        return builder.toString()
    }

    private fun isIgnorable(char: Char): Boolean = char == TATWEEL || char == ZERO_WIDTH_JOINER || isDiacritic(char)

    /**
     * Search key: [normalize]d, lower-cased, with hamza carriers folded (آ أ إ ٱ → ا, ؤ → و, ئ → ی)
     * and spaces removed, so "نوروز", "نو روز" and "نوروز" with a ZWNJ all compare equal. Idempotent.
     */
    public fun searchKey(text: String): String =
        buildString {
            for (char in normalize(text).lowercase()) {
                when (char) {
                    ' ' -> Unit
                    in ALEF_VARIANTS -> append(ALEF)
                    WAW_WITH_HAMZA -> append(WAW)
                    YEH_WITH_HAMZA -> append(FARSI_YEH)
                    else -> append(char)
                }
            }
        }

    private fun canonicalLetter(char: Char): Char =
        when (char) {
            ARABIC_YEH, ALEF_MAKSURA -> FARSI_YEH
            ARABIC_KAF -> KEHEH
            else -> char
        }
}

/** Typo-tolerant matching for search (T-203): restricted Damerau–Levenshtein distance on [PersianText.searchKey]s. */
public object FuzzyMatcher {
    /** Default tolerance: at most two single-character edits or adjacent transpositions. */
    public const val DEFAULT_MAX_DISTANCE: Int = 2

    /**
     * Optimal string alignment distance between [a] and [b] (insertions, deletions, substitutions and adjacent
     * transpositions). Stops early and returns `maxDistance + 1` as soon as the distance must exceed [maxDistance].
     */
    public fun distance(
        a: String,
        b: String,
        maxDistance: Int = DEFAULT_MAX_DISTANCE,
    ): Int {
        require(maxDistance >= 0) { "maxDistance must be ≥ 0 (was $maxDistance)" }
        if (Math.abs(a.length - b.length) > maxDistance) return maxDistance + 1
        var beforePrevious = IntArray(b.length + 1)
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)
        for (i in 1..a.length) {
            current[0] = i
            var rowMinimum = i
            for (j in 1..b.length) {
                current[j] = editCost(a, b, i, j, previous, current, beforePrevious)
                rowMinimum = minOf(rowMinimum, current[j])
            }
            if (rowMinimum > maxDistance) return maxDistance + 1
            val recycled = beforePrevious
            beforePrevious = previous
            previous = current
            current = recycled
        }
        return minOf(previous[b.length], maxDistance + 1)
    }

    /** Whether [candidate] matches [query] within [maxDistance] edits after normalization. */
    public fun matches(
        query: String,
        candidate: String,
        maxDistance: Int = DEFAULT_MAX_DISTANCE,
    ): Boolean = distance(PersianText.searchKey(query), PersianText.searchKey(candidate), maxDistance) <= maxDistance

    /** Whether the last two characters of `a[..i]` and `b[..j]` are swapped. */
    private fun isTransposition(
        a: String,
        b: String,
        i: Int,
        j: Int,
    ): Boolean = i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]

    @Suppress("LongParameterList")
    private fun editCost(
        a: String,
        b: String,
        i: Int,
        j: Int,
        previous: IntArray,
        current: IntArray,
        beforePrevious: IntArray,
    ): Int {
        val substitution = if (a[i - 1] == b[j - 1]) 0 else 1
        var cost = minOf(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + substitution)
        if (isTransposition(a, b, i, j)) {
            cost = minOf(cost, beforePrevious[j - 2] + 1)
        }
        return cost
    }
}
