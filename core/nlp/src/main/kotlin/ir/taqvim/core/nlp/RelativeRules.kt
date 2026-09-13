/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.nlp

import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.addMonths
import ir.taqvim.core.model.Jdn

/**
 * Relative phrases (PEG alternatives at every token):
 * `named` ← TODAY | TOMORROW | …; `counted` ← NUMBER UNIT (FUTURE | PAST); `within` ← IN NUMBER UNIT;
 * `weekday` ← (THIS | NEXT | LAST)? WEEKDAY (NEXT | LAST)?; `unitShift` ← (NEXT | LAST)? UNIT (NEXT | LAST)?.
 */
internal class RelativeRules(
    private val tokens: List<Token>,
    private val context: ParseContext,
) {
    private val calendar = context.calendars[context.preferredCalendar] ?: GregorianCalendarSystem

    fun candidates(): List<Candidate> =
        tokens.indices.flatMap { listOfNotNull(named(it), counted(it), within(it), weekday(it), unitShift(it)) }

    private fun candidate(
        range: IntRange,
        jdn: Jdn,
        confidence: Double,
    ) = Candidate(range, jdn, calendar.system, confidence, ParseKind.RELATIVE)

    private fun named(index: Int): Candidate? =
        NAMED_OFFSETS.firstNotNullOfOrNull { (concept, offset) ->
            Lexicon.concept(tokens, index, concept).takeIf { it > 0 }?.let {
                candidate(index until index + it, context.reference + offset, NAMED_CONFIDENCE)
            }
        }

    private fun counted(index: Int): Candidate? {
        val count = Lexicon.number(tokens, index) ?: return null
        val unit = unitAt(index + 1) ?: return null
        return direction(index + 2)?.let { (sign, width) ->
            candidate(index until index + 2 + width, shift(unit, sign * count), COUNTED_CONFIDENCE)
        }
    }

    private fun within(index: Int): Candidate? {
        if (Lexicon.concept(tokens, index, Concept.IN) != 1) return null
        val count = Lexicon.number(tokens, index + 1) ?: return null
        return unitAt(index + 2)?.let { candidate(index..index + 2, shift(it, count), COUNTED_CONFIDENCE) }
    }

    private fun weekday(index: Int): Candidate? {
        val day = Lexicon.weekday(tokens[index]).firstOrNull() ?: return null
        val before = modifier(index - 1)
        val after = modifier(index + 1)
        val today = context.reference.weekday()
        val ahead = day.daysAfter(today)
        val back = -nonZero(today.daysAfter(day))
        val modifier = before ?: after?.takeIf { it != Concept.THIS }
        val range =
            when {
                before != null -> index - 1..index
                modifier != null -> index..index + 1
                else -> index..index
            }
        val offset =
            when (modifier) {
                Concept.NEXT -> nonZero(ahead)
                Concept.LAST -> back
                else -> ahead
            }
        val confidence = if (modifier == null) BARE_WEEKDAY_CONFIDENCE else MODIFIED_WEEKDAY_CONFIDENCE
        return candidate(range, context.reference + offset, confidence)
    }

    private fun unitShift(index: Int): Candidate? {
        val unit = unitAt(index)?.takeIf { it != Concept.DAY } ?: return null
        val before = modifier(index - 1)?.takeIf { it != Concept.THIS }
        val after = modifier(index + 1)?.takeIf { it != Concept.THIS }
        val modifier = before ?: after ?: return null
        val range = if (before != null) index - 1..index else index..index + 1
        return candidate(range, shift(unit, if (modifier == Concept.NEXT) 1 else -1), UNIT_SHIFT_CONFIDENCE)
    }

    private fun unitAt(index: Int): Concept? = UNITS.firstOrNull { Lexicon.concept(tokens, index, it) == 1 }

    private fun modifier(index: Int): Concept? = MODIFIERS.firstOrNull { Lexicon.concept(tokens, index, it) == 1 }

    /** Sign and width of a FUTURE or PAST word at [index]. */
    private fun direction(index: Int): Pair<Int, Int>? {
        val future = Lexicon.concept(tokens, index, Concept.FUTURE)
        val past = Lexicon.concept(tokens, index, Concept.PAST)
        return when {
            future > 0 -> 1 to future
            past > 0 -> -1 to past
            else -> null
        }
    }

    private fun shift(
        unit: Concept,
        amount: Int,
    ): Jdn =
        when (unit) {
            Concept.DAY -> {
                context.reference + amount
            }

            Concept.WEEK -> {
                context.reference + amount * DAYS_PER_WEEK
            }

            else -> {
                val months = if (unit == Concept.YEAR) amount * MONTHS_PER_YEAR else amount
                calendar.toJdn(calendar.addMonths(calendar.fromJdn(context.reference), months))
            }
        }

    private fun nonZero(days: Int): Int = if (days == 0) DAYS_PER_WEEK else days

    private companion object {
        const val NAMED_CONFIDENCE = 0.9
        const val COUNTED_CONFIDENCE = 0.85
        const val MODIFIED_WEEKDAY_CONFIDENCE = 0.85
        const val BARE_WEEKDAY_CONFIDENCE = 0.6
        const val UNIT_SHIFT_CONFIDENCE = 0.7
        const val DAYS_PER_WEEK = 7
        const val MONTHS_PER_YEAR = 12
        val UNITS = listOf(Concept.DAY, Concept.WEEK, Concept.MONTH, Concept.YEAR)
        val MODIFIERS = listOf(Concept.THIS, Concept.NEXT, Concept.LAST)
        val NAMED_OFFSETS =
            listOf(
                Concept.DAY_AFTER_TOMORROW to 2,
                Concept.DAY_BEFORE_YESTERDAY to -2,
                Concept.TODAY to 0,
                Concept.TOMORROW to 1,
                Concept.YESTERDAY to -1,
            )
    }
}
