/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.nlp

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.i18n.DateFieldOrder
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import kotlin.math.abs

/** Year, month and day fields read in one order, with a [weight] for how likely that order is. */
internal data class Fields(
    val year: Int,
    val month: Int,
    val day: Int,
    val weight: Double,
)

/** Token positions of the day and (optional) year number around a month name. */
private data class Shape(
    val day: Int,
    val year: Int?,
)

/**
 * Written dates (PEG alternatives, tried at every token):
 * `separated` ← NUMBER SEP NUMBER SEP NUMBER (same separator) CALENDAR?;
 * `named` ← day/year numbers around a MONTH name (D M Y, Y M D, M D Y, D M, M D), allowing two filler tokens;
 * `spaced` ← NUMBER filler? NUMBER filler? NUMBER (long formats without month names).
 */
internal class AbsoluteRules(
    private val tokens: List<Token>,
    private val context: ParseContext,
) {
    fun candidates(): List<Candidate> = tokens.indices.flatMap { separated(it) + named(it) + spaced(it) }

    private fun separated(index: Int): List<Candidate> {
        val window = tokens.subList(index, minOf(index + SEPARATED_TOKENS, tokens.size))
        val complete =
            window.size == SEPARATED_TOKENS && window.filterIndexed { i, _ -> i % 2 == 0 }.all { it.number != null }
        val separators = window.filterIndexed { i, _ -> i % 2 == 1 }
        if (!complete || separators.any { it.type != TokenType.SEPARATOR } || separators[0].raw != separators[1].raw) {
            return emptyList()
        }
        val values = Triple(window.first().number ?: 0, window[2].number ?: 0, window.last().number ?: 0)
        return orderings(window.first().raw.length, window.last().raw.length, values, context.numericOrder)
            .flatMap { inCalendars(it, index, index + SEPARATED_TOKENS, SEPARATED_CONFIDENCE) }
    }

    private fun spaced(index: Int): List<Candidate> {
        val chain =
            generateSequence(index.takeIf { tokens[it].number != null }) { numberAfterFiller(it + 1) }
                .take(FIELDS)
                .toList()
        if (chain.size < FIELDS) return emptyList()
        val (first, middle, last) = chain
        val values = Triple(tokens[first].number ?: 0, tokens[middle].number ?: 0, tokens[last].number ?: 0)
        return orderings(tokens[first].raw.length, tokens[last].raw.length, values, context.textOrder)
            .flatMap { inCalendars(it, first, last + 1, SPACED_CONFIDENCE) }
    }

    private fun named(index: Int): List<Candidate> {
        val (months, width) = Lexicon.month(tokens, index) ?: return emptyList()
        val shapes = shapesAround(numberBefore(index), numberAfter(index + width))
        val shared = months.map { it.first }.distinct().size > 1
        return months.flatMap { (system, month) ->
            val penalty = if (shared && system != context.preferredCalendar) SHARED_NAME_PENALTY else 0.0
            shapes.mapNotNull { shape ->
                resolve(shape, system, month, index until index + width)?.let {
                    it.copy(
                        confidence =
                            it.confidence - penalty,
                    )
                }
            }
        }
    }

    /** Day/year arrangements of the numbers [left] and [right] of a month name (D M Y, Y M D, M D Y, D M, M D). */
    private fun shapesAround(
        left: Int?,
        right: Int?,
    ): List<Shape> {
        val second = right?.let { numberAfter(it + 1) }
        val dayLeft = isDay(left)
        val dayRight = isDay(right)
        return listOfNotNull(
            shape(left, right, dayLeft && isYear(right)),
            shape(right, left, dayRight && isYear(left)),
            shape(right, second, dayRight && isYear(second)),
            shape(left, null, dayLeft && !isYear(right)),
            shape(right, null, dayRight && !isYear(left) && !isYear(second)),
        )
    }

    private fun shape(
        day: Int?,
        year: Int?,
        applies: Boolean,
    ): Shape? = if (applies && day != null) Shape(day, year) else null

    private fun resolve(
        shape: Shape,
        system: CalendarSystem,
        month: Int,
        monthTokens: IntRange,
    ): Candidate? {
        val calendar = context.calendars[system]
        val day = tokens[shape.day].number
        if (calendar == null || day == null) return null
        val year = shape.year?.let { tokens[it].number }
        // Like numeric dates, a year far from the reference is implausible ("#4521 is due March 26, 2028").
        val distance = year?.let { abs(it - calendar.fromJdn(context.reference).year) } ?: 0
        val jdn =
            (if (year != null) exact(calendar, year, month, day) else nearest(calendar, month, day))
                ?.takeIf { distance <= MAX_YEAR_DISTANCE } ?: return null
        val used = listOfNotNull(shape.day, shape.year, monthTokens.first, monthTokens.last)
        val base = if (year != null) NAMED_CONFIDENCE - distance / DISTANCE_SCALE else YEARLESS_CONFIDENCE
        val range = used.min()..used.max()
        return Candidate(range, jdn, system, base + weekdayAdjustment(range, jdn), ParseKind.ABSOLUTE)
    }

    private fun inCalendars(
        fields: Fields,
        start: Int,
        end: Int,
        base: Double,
    ): List<Candidate> {
        val marker = Lexicon.calendarMarker(tokens, end)
        val last = end - 1 + (marker?.second ?: 0)
        return context.calendars.mapNotNull { (system, calendar) ->
            val distance = abs(fields.year - calendar.fromJdn(context.reference).year)
            val plausible = distance <= MAX_YEAR_DISTANCE && (marker == null || marker.first == system)
            exact(calendar, fields.year, fields.month, fields.day)?.takeIf { plausible }?.let { jdn ->
                val penalty = if (marker == null && system != context.preferredCalendar) OTHER_CALENDAR_PENALTY else 0.0
                val confidence =
                    base * fields.weight - penalty - distance / DISTANCE_SCALE + weekdayAdjustment(start..last, jdn)
                Candidate(start..last, jdn, system, confidence, ParseKind.ABSOLUTE)
            }
        }
    }

    private fun exact(
        calendar: CalendarArithmetic,
        year: Int,
        month: Int,
        day: Int,
    ): Jdn? =
        if (calendar.isValid(
                year,
                month,
                day,
            )
        ) {
            calendar.toJdn(CalendarDate(calendar.system, year, month, day))
        } else {
            null
        }

    /** The year-less [month]/[day] occurrence closest to the reference day (later wins a tie). */
    private fun nearest(
        calendar: CalendarArithmetic,
        month: Int,
        day: Int,
    ): Jdn? {
        val referenceYear = calendar.fromJdn(context.reference).year
        return (referenceYear - 1..referenceYear + 1)
            .mapNotNull { exact(calendar, it, month, day) }
            .minWithOrNull(compareBy<Jdn>({ abs(it - context.reference) }, { if (it >= context.reference) 0 else 1 }))
    }

    /** Small bonus when a nearby weekday name matches the date, a penalty when it contradicts it. */
    private fun weekdayAdjustment(
        range: IntRange,
        jdn: Jdn,
    ): Double {
        val named =
            listOf(range.first - 1, range.first - 2, range.last + 1, range.last + 2)
                .flatMap { Lexicon.weekday(tokens.getOrNull(it)) }
                .toSet()
        return when {
            named.isEmpty() -> 0.0
            jdn.weekday() in named -> WEEKDAY_MATCH_BONUS
            else -> -WEEKDAY_MISMATCH_PENALTY
        }
    }

    private fun isDay(index: Int?): Boolean =
        index != null && tokens[index].raw.length <= 2 && tokens[index].number in 1..MAX_DAY

    private fun isYear(index: Int?): Boolean = index != null && tokens[index].raw.length >= YEAR_DIGITS

    /** The nearest number before [index] reached over at most [MAX_GAP] filler tokens. */
    private fun numberBefore(index: Int): Int? =
        (index - 1 downTo maxOf(0, index - 1 - MAX_GAP))
            .firstOrNull { tokens[it].number != null || !isFiller(it) }
            ?.takeIf { tokens[it].number != null }

    /** The nearest number at or after [index] reached over at most [MAX_GAP] filler tokens. */
    private fun numberAfter(index: Int): Int? =
        (index until minOf(tokens.size, index + 1 + MAX_GAP))
            .firstOrNull { tokens[it].number != null || !isFiller(it) }
            ?.takeIf { tokens[it].number != null }

    /** The next number at [index], or after one non-separator filler token. */
    private fun numberAfterFiller(index: Int): Int? {
        val next = tokens.getOrNull(index)
        return when {
            next?.number != null -> {
                index
            }

            next?.type != TokenType.SEPARATOR && isFiller(index) && tokens.getOrNull(index + 1)?.number != null -> {
                index +
                    1
            }

            else -> {
                null
            }
        }
    }

    /** Punctuation, a separator, or a short word (`de`, `د`, `ê`) that is neither a month nor a connector. */
    private fun isFiller(index: Int): Boolean {
        val token = tokens.getOrNull(index)
        val shortWord =
            token?.type == TokenType.WORD &&
                token.raw.length <= MAX_FILLER_LENGTH &&
                Lexicon.month(tokens, index) == null &&
                CONNECTORS.none { Lexicon.concept(tokens, index, it) > 0 }
        return token != null && (token.type == TokenType.PUNCTUATION || token.type == TokenType.SEPARATOR || shortWord)
    }

    internal companion object {
        const val SEPARATED_CONFIDENCE = 0.95
        const val NAMED_CONFIDENCE = 0.95
        const val YEARLESS_CONFIDENCE = 0.8
        const val SPACED_CONFIDENCE = 0.6
        const val OTHER_CALENDAR_PENALTY = 0.15
        const val ALTERNATIVE_ORDER_WEIGHT = 0.8
        const val WEEKDAY_MATCH_BONUS = 0.01
        const val WEEKDAY_MISMATCH_PENALTY = 0.3
        const val MAX_YEAR_DISTANCE = 150
        const val DISTANCE_SCALE = 2000.0
        const val YEAR_DIGITS = 3
        const val MAX_DAY = 31
        const val MAX_GAP = 2
        const val FIELDS = 3
        const val SEPARATED_TOKENS = 5
        const val MAX_FILLER_LENGTH = 3
        const val SHARED_NAME_PENALTY = 0.05
        val CONNECTORS = listOf(Concept.RANGE_FROM, Concept.RANGE_TO, Concept.ANCHOR_BEFORE, Concept.ANCHOR_AFTER)

        /** Field orders for three numbers whose first or last one has [YEAR_DIGITS] digits or more. */
        fun orderings(
            firstDigits: Int,
            lastDigits: Int,
            values: Triple<Int, Int, Int>,
            preferred: DateFieldOrder,
        ): List<Fields> {
            val (a, b, c) = values
            val dayFirst = if (preferred == DateFieldOrder.MDY) ALTERNATIVE_ORDER_WEIGHT else 1.0
            val monthFirst = if (preferred == DateFieldOrder.MDY) 1.0 else ALTERNATIVE_ORDER_WEIGHT
            return when {
                firstDigits >= YEAR_DIGITS -> listOf(Fields(a, b, c, 1.0))
                lastDigits >= YEAR_DIGITS -> listOf(Fields(c, b, a, dayFirst), Fields(c, a, b, monthFirst))
                else -> emptyList()
            }
        }
    }
}
