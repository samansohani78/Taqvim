/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.nlp

import java.util.BitSet

/**
 * A date mention found in free text (T-501): its character [span], the [best] reading and the other readings of the
 * same span (e.g. a year-first number read in another calendar), best first.
 */
public data class DetectedDate(
    public val span: IntRange,
    public val best: ParseResult,
    public val alternatives: List<ParseResult> = emptyList(),
)

/** Which readings [TextDateDetector] reports: phrase [kinds] and a [minConfidence] in `0..1`. */
public data class DetectionOptions(
    public val kinds: Set<ParseKind> = DEFAULT_KINDS,
    public val minConfidence: Double = DEFAULT_MIN_CONFIDENCE,
) {
    public companion object {
        /** Written dates and ranges; relative words like "today" are too common in free text to report by default. */
        public val DEFAULT_KINDS: Set<ParseKind> = setOf(ParseKind.ABSOLUTE, ParseKind.RANGE)

        /** Drops numbers-only long dates (0.6) and event names read alone (0.5) unless something corroborates them. */
        public const val DEFAULT_MIN_CONFIDENCE: Double = 0.7
    }
}

/**
 * Finds date mentions in arbitrary text (T-501, F-04). A scanner marks seed tokens — digits, and for relative or
 * anchored kinds the words that can begin such phrases — and only the token windows around seeds are read with the
 * [DateParser] grammar. Overlapping readings are resolved by score: the most confident span wins (then the longer,
 * then the earlier) and every reading that overlaps an accepted span is dropped. Results are in text order.
 */
public object TextDateDetector {
    /** Tokens kept on each side of a seed; more than any phrase of the grammar reaches from its nearest seed. */
    internal const val WINDOW_TOKENS: Int = 24

    /** Date mentions in [text], read with [context] and filtered by [options]. */
    public fun detect(
        text: String,
        context: ParseContext,
        options: DetectionOptions = DetectionOptions(),
    ): List<DetectedDate> {
        val readings =
            segments(Tokenizer.tokenize(text), options.kinds).flatMap { segment ->
                DateParser
                    .parse(text.substring(segment.first, segment.last + 1), context, options.kinds)
                    .filter { it.confidence >= options.minConfidence }
                    .map { it.copy(span = it.span.first + segment.first..it.span.last + segment.first) }
            }
        return resolve(readings, text.length)
    }

    /** Character ranges of the merged windows of [WINDOW_TOKENS] tokens around every seed token. */
    internal fun segments(
        tokens: List<Token>,
        kinds: Set<ParseKind>,
    ): List<IntRange> {
        val words = ParseKind.RELATIVE in kinds || ParseKind.ANCHORED in kinds
        val windows = mutableListOf<IntRange>()
        tokens.forEachIndexed { index, token ->
            if (token.type == TokenType.NUMBER || (words && Lexicon.isSeedWord(token))) {
                val window = maxOf(0, index - WINDOW_TOKENS)..minOf(tokens.lastIndex, index + WINDOW_TOKENS)
                val previous = windows.lastOrNull()
                if (previous != null && window.first <= previous.last + 1) {
                    windows[windows.lastIndex] = previous.first..window.last
                } else {
                    windows += window
                }
            }
        }
        return windows.map { tokens[it.first].start until tokens[it.last].end }
    }

    private fun resolve(
        readings: List<ParseResult>,
        length: Int,
    ): List<DetectedDate> {
        val taken = BitSet(length)
        val bySpan =
            readings
                .groupBy { it.span }
                .values
                .map { it.sortedWith(DateParser.RANKING) }
                .sortedWith(compareBy(DateParser.RANKING) { it.first() })
        val accepted = mutableListOf<DetectedDate>()
        for (group in bySpan) {
            val span = group.first().span
            if (taken.get(span.first, span.last + 1).isEmpty) {
                taken.set(span.first, span.last + 1)
                accepted += DetectedDate(span, group.first(), group.drop(1))
            }
        }
        return accepted.sortedBy { it.span.first }
    }
}
