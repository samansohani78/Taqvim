/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.nlp

import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.Jdn

/**
 * Parses date phrases in Persian and English (T-500, F-03) with a small hand-written PEG-style grammar over tokens:
 * written dates in three calendars (digits of any script), relative phrases, phrases anchored to a named event, and
 * ranges. Every reading is returned, ranked by confidence, then by length, then by position.
 */
public object DateParser {
    private const val MAX_CONFIDENCE = 0.99
    private val RANKING =
        compareByDescending<ParseResult> { it.confidence }
            .thenByDescending { it.span.last - it.span.first }
            .thenBy { it.span.first }

    /** All readings of [text], best first. */
    public fun parse(
        text: String,
        context: ParseContext,
    ): List<ParseResult> {
        val tokens = Tokenizer.tokenize(text)
        val single =
            AbsoluteRules(tokens, context).candidates() +
                RelativeRules(tokens, context).candidates() +
                AnchoredRules(tokens, text, context).candidates()
        return (single + RangeRules(tokens, context).combine(single))
            .map { it.toResult(tokens, context) }
            .sortedWith(RANKING)
            .distinctBy { Triple(it.span, it.jdn, it.end?.jdn) }
    }

    /** The best reading of [text], or `null` when it contains no date. */
    public fun parseBest(
        text: String,
        context: ParseContext,
    ): ParseResult? = parse(text, context).firstOrNull()

    private fun Candidate.toResult(
        tokens: List<Token>,
        context: ParseContext,
    ): ParseResult {
        val calendar = context.calendars[system] ?: GregorianCalendarSystem
        return ParseResult(
            date = calendar.fromJdn(jdn),
            jdn = jdn,
            confidence = confidence.coerceIn(0.0, MAX_CONFIDENCE),
            span = tokens[this.tokens.first].start until tokens[this.tokens.last].end,
            kind = kind,
            end = endJdn?.let { ParsedEnd(calendar.fromJdn(it), it) },
        )
    }
}

/**
 * Phrases around a named event: `offset` ← NUMBER UNIT (ANCHOR_BEFORE | ANCHOR_AFTER) EVENT;
 * `event` ← up to six words that [AnchorLookup] recognizes (longest first).
 */
internal class AnchoredRules(
    private val tokens: List<Token>,
    private val text: String,
    private val context: ParseContext,
) {
    private val calendar = context.calendars[context.preferredCalendar] ?: GregorianCalendarSystem

    fun candidates(): List<Candidate> {
        val anchors = context.anchors ?: return emptyList()
        return tokens.indices.flatMap { listOfNotNull(offsetFromEvent(it, anchors), event(it, anchors)) }
    }

    private fun offsetFromEvent(
        index: Int,
        anchors: AnchorLookup,
    ): Candidate? {
        val count = Lexicon.number(tokens, index) ?: return null
        val step = stepDays(index + 1)
        val direction = anchorDirection(index + 2)
        if (step == null || direction == null) return null
        return event(index + 2 + direction.second, anchors)?.let { found ->
            Candidate(
                index..found.tokens.last,
                found.jdn + direction.first * count * step,
                calendar.system,
                OFFSET_CONFIDENCE,
                ParseKind.ANCHORED,
            )
        }
    }

    private fun event(
        index: Int,
        anchors: AnchorLookup,
    ): Candidate? {
        val longest = minOf(MAX_EVENT_WORDS, tokens.size - index)
        return (longest downTo 1).firstNotNullOfOrNull { width ->
            val window = tokens.subList(index, index + width)
            window
                .takeIf { words -> words.all { it.type == TokenType.WORD } }
                ?.let { anchors.find(text.substring(it.first().start, it.last().end), context.reference) }
                ?.let {
                    Candidate(
                        index until index + width,
                        it,
                        calendar.system,
                        EVENT_CONFIDENCE,
                        ParseKind.ANCHORED,
                    )
                }
        }
    }

    private fun stepDays(index: Int): Int? =
        when {
            Lexicon.concept(tokens, index, Concept.DAY) == 1 -> 1
            Lexicon.concept(tokens, index, Concept.WEEK) == 1 -> DAYS_PER_WEEK
            else -> null
        }

    private fun anchorDirection(index: Int): Pair<Int, Int>? {
        val before = Lexicon.concept(tokens, index, Concept.ANCHOR_BEFORE)
        val after = Lexicon.concept(tokens, index, Concept.ANCHOR_AFTER)
        return when {
            before > 0 -> -1 to before
            after > 0 -> 1 to after
            else -> null
        }
    }

    private companion object {
        const val OFFSET_CONFIDENCE = 0.88
        const val EVENT_CONFIDENCE = 0.5
        const val MAX_EVENT_WORDS = 6
        const val DAYS_PER_WEEK = 7
    }
}

/**
 * Ranges: `range` ← RANGE_FROM? DATE (RANGE_TO | `-`) DATE with the second date not before the first;
 * `dayRange` ← RANGE_FROM? DAY RANGE_TO DATE, the first day sharing the second date's month and year.
 */
internal class RangeRules(
    private val tokens: List<Token>,
    private val context: ParseContext,
) {
    fun combine(singles: List<Candidate>): List<Candidate> {
        val byStart = singles.groupBy { it.tokens.first }
        val joined =
            singles.flatMap { first ->
                connectorWidth(first.tokens.last + 1)
                    ?.let { width ->
                        byStart[first.tokens.last + 1 + width].orEmpty().mapNotNull { range(first, it) }
                    }.orEmpty()
            }
        return joined + tokens.indices.flatMap { dayRanges(it, byStart) }
    }

    private fun connectorWidth(index: Int): Int? {
        val word = Lexicon.concept(tokens, index, Concept.RANGE_TO)
        val dash = tokens.getOrNull(index)?.raw in DASHES
        return if (word > 0) word else 1.takeIf { dash }
    }

    private fun range(
        first: Candidate,
        second: Candidate,
    ): Candidate? =
        second.takeIf { it.jdn >= first.jdn }?.let {
            // The connector corroborates both ends, so a range outranks either end read alone.
            val confidence = (first.confidence + it.confidence) / 2 + RANGE_BONUS
            Candidate(
                start(first.tokens.first)..it.tokens.last,
                first.jdn,
                first.system,
                confidence,
                ParseKind.RANGE,
                it.jdn,
            )
        }

    private fun dayRanges(
        index: Int,
        byStart: Map<Int, List<Candidate>>,
    ): List<Candidate> {
        val day = tokens[index].number?.takeIf { tokens[index].raw.length <= 2 }
        val width = connectorWidth(index + 1)
        if (day == null || width == null) return emptyList()
        return byStart[index + 1 + width].orEmpty().filter { it.kind == ParseKind.ABSOLUTE }.mapNotNull { second ->
            val calendar = context.calendars[second.system] ?: return@mapNotNull null
            val last = calendar.fromJdn(second.jdn)
            calendar.takeIf { it.isValid(last.year, last.month, day) && day < last.day }?.let {
                val jdn: Jdn = it.toJdn(CalendarDate(last.system, last.year, last.month, day))
                Candidate(
                    start(index)..second.tokens.last,
                    jdn,
                    second.system,
                    second.confidence + RANGE_BONUS,
                    ParseKind.RANGE,
                    second.jdn,
                )
            }
        }
    }

    /** [index], or one token earlier when that token is a RANGE_FROM word. */
    private fun start(index: Int): Int =
        if (Lexicon.concept(tokens, index - 1, Concept.RANGE_FROM) ==
            1
        ) {
            index - 1
        } else {
            index
        }

    private companion object {
        const val RANGE_BONUS = 0.04
        val DASHES = setOf("-", "–")
    }
}
