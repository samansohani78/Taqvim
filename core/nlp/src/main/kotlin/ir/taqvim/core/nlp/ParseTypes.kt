/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.nlp

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.IranIslamicCalendar
import ir.taqvim.core.calendar.NepaliCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.i18n.DateFieldOrder
import ir.taqvim.core.i18n.FormatTable
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn

/** What kind of phrase produced a [ParseResult]. */
public enum class ParseKind {
    /** A written date: numeric, with a month name, or numbers in a long format. */
    ABSOLUTE,

    /** Relative to the reference day: "tomorrow", "3 days ago", "next Friday". */
    RELATIVE,

    /** Relative to a named event: "2 days before Eid al-Fitr". */
    ANCHORED,

    /** Two dates joined by a range connector: "from … to …". */
    RANGE,
}

/** The last day of a range: [date] in the range's calendar and its [jdn]. */
public data class ParsedEnd(
    public val date: CalendarDate,
    public val jdn: Jdn,
)

/**
 * One reading of a date phrase (T-500): [date] in the calendar the phrase was written in (relative phrases use the
 * preferred calendar), its [jdn], a [confidence] in `0..1`, the character [span] it covers in the input, and the range
 * [end] for [ParseKind.RANGE].
 */
public data class ParseResult(
    public val date: CalendarDate,
    public val jdn: Jdn,
    public val confidence: Double,
    public val span: IntRange,
    public val kind: ParseKind,
    public val end: ParsedEnd? = null,
)

/** Finds the day of a named event (e.g. through the event search index) for a phrase near [reference]. */
public fun interface AnchorLookup {
    /** The day of the event named by [query] closest to [reference], or `null` if no event matches. */
    public fun find(
        query: String,
        reference: Jdn,
    ): Jdn?
}

/**
 * How to read phrases: relative ones are resolved against [reference]; numbers without a month name are tried in
 * every calendar of [calendars], favouring [preferredCalendar]; [numericOrder] decides `1/2/2026`-style ambiguity and
 * [textOrder] the order of numbers-only long dates whose year comes last.
 */
public data class ParseContext(
    public val reference: Jdn,
    public val preferredCalendar: CalendarSystem = CalendarSystem.PERSIAN,
    public val numericOrder: DateFieldOrder = DateFieldOrder.DMY,
    public val textOrder: DateFieldOrder = DateFieldOrder.DMY,
    public val anchors: AnchorLookup? = null,
    public val calendars: Map<CalendarSystem, CalendarArithmetic> = DEFAULT_CALENDARS,
) {
    public companion object {
        /** Persian (A-02), Iranian official Islamic (A-05) and Gregorian; see [calendarsFor] for Bikram Sambat. */
        public val DEFAULT_CALENDARS: Map<CalendarSystem, CalendarArithmetic> =
            mapOf(
                CalendarSystem.PERSIAN to PersianCalendarSystem,
                CalendarSystem.ISLAMIC to IranIslamicCalendar(),
                CalendarSystem.GREGORIAN to GregorianCalendarSystem,
            )

        /**
         * The calendars of [arithmetic] to read numbers in for someone who uses [used]. Bikram Sambat years run about
         * 57 years ahead of Gregorian ones, so "05/06/2026" would also be a plausible BS date; the Nepali calendar is
         * tried only by people who use it (T-105).
         */
        public fun calendarsFor(
            used: Collection<CalendarSystem>,
            arithmetic: Map<CalendarSystem, CalendarArithmetic> = DEFAULT_CALENDARS + NEPALI,
        ): Map<CalendarSystem, CalendarArithmetic> = arithmetic.filterKeys { it != CalendarSystem.NEPALI || it in used }

        private val NEPALI = CalendarSystem.NEPALI to NepaliCalendarSystem

        /** A context with [language]'s numeric and long field orders; [calendar] defaults to its first calendar. */
        public fun forLanguage(
            language: LanguageSpec,
            reference: Jdn,
            calendar: CalendarSystem = language.calendars.first(),
            anchors: AnchorLookup? = null,
        ): ParseContext =
            ParseContext(
                reference = reference,
                preferredCalendar = calendar,
                numericOrder = language.datePattern.order,
                textOrder = textOrderOf(FormatTable.of(language).datePatterns[CalendarSystem.GREGORIAN].orEmpty()),
                anchors = anchors,
                calendars = calendarsFor(language.calendars + calendar),
            )

        /** Whether the day or the month comes first in a CLDR date [pattern] (year-first patterns read as DMY). */
        internal fun textOrderOf(pattern: String): DateFieldOrder {
            val day = pattern.indexOf('d')
            val month = pattern.indexOf('M')
            return if (day >= 0 && month >= 0 && month < day) DateFieldOrder.MDY else DateFieldOrder.DMY
        }
    }
}

/** A reading over the token range [tokens], before it is turned into a [ParseResult]. */
internal data class Candidate(
    val tokens: IntRange,
    val jdn: Jdn,
    val system: CalendarSystem,
    val confidence: Double,
    val kind: ParseKind,
    val endJdn: Jdn? = null,
)
