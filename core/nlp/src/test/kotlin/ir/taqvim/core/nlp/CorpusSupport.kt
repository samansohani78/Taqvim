/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.nlp

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.addMonths
import ir.taqvim.core.i18n.FormatTable
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.i18n.PersianText
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import kotlin.random.Random

/** One corpus phrase with the context it is read in and the day (or range) it must resolve to. */
internal data class CorpusRow(
    val id: String,
    val language: String,
    val calendar: CalendarSystem,
    val reference: Jdn,
    val phrase: String,
    val start: Jdn,
    val end: Jdn? = null,
) {
    fun toLine(): String {
        val referenceDate = GregorianCalendarSystem.fromJdn(reference).toIsoLikeString()
        val endText = end?.value?.toString() ?: NO_END
        return listOf(id, "$language;${calendar.name};$referenceDate", phrase, start.value.toString(), endText)
            .joinToString("\t")
    }

    fun context(): ParseContext =
        ParseContext.forLanguage(requireNotNull(LanguageTable.forCode(language)), reference, calendar, SyntheticEvents)

    companion object {
        private const val NO_END = "-"

        fun parse(line: String): CorpusRow {
            val columns = line.split('\t')
            val (language, calendar, reference) = columns[1].split(';')
            val (year, month, day) = reference.split('-').map(String::toInt)
            return CorpusRow(
                id = columns[0],
                language = language,
                calendar = CalendarSystem.valueOf(calendar),
                reference = GregorianCalendarSystem.toJdn(CalendarDate(CalendarSystem.GREGORIAN, year, month, day)),
                phrase = columns[2],
                start = Jdn(columns[3].toLong()),
                end = columns.last().takeIf { it != NO_END }?.let { Jdn(it.toLong()) },
            )
        }
    }
}

/** Invented event names (no real holidays) resolved to a fixed month and day in the reference's year. */
internal object SyntheticEvents : AnchorLookup {
    data class Event(
        val name: String,
        val system: CalendarSystem,
        val month: Int,
        val day: Int,
    )

    val persian =
        listOf(
            Event("جشن نمونه", CalendarSystem.PERSIAN, 3, 14),
            Event("جشنواره آزمایشی", CalendarSystem.ISLAMIC, 9, 10),
        )
    val english =
        listOf(
            Event("Sample Festival", CalendarSystem.GREGORIAN, 5, 20),
            Event("Test Founders Holiday", CalendarSystem.PERSIAN, 8, 2),
        )

    override fun find(
        query: String,
        reference: Jdn,
    ): Jdn? =
        (persian + english)
            .firstOrNull {
                PersianText.searchKey(it.name) == PersianText.searchKey(query)
            }?.let { occurrence(it, reference) }

    fun occurrence(
        event: Event,
        reference: Jdn,
    ): Jdn {
        val calendar = ParseContext.DEFAULT_CALENDARS.getValue(event.system)
        return calendar.toJdn(CalendarDate(event.system, calendar.fromJdn(reference).year, event.month, event.day))
    }
}

/** Deterministic random choices and calendar helpers for the corpus generator. */
internal class CorpusKit(
    seed: Int,
) {
    val random = Random(seed)
    val fa: LanguageSpec = requireNotNull(LanguageTable.forCode("fa"))
    val en: LanguageSpec = requireNotNull(LanguageTable.forCode("en"))

    fun calendar(system: CalendarSystem): CalendarArithmetic = ParseContext.DEFAULT_CALENDARS.getValue(system)

    /** A reference day between 2020-01-01 and the end of 2034. */
    fun reference(): Jdn = Jdn(REFERENCE_START + random.nextInt(REFERENCE_DAYS))

    fun number(range: IntRange): Int = random.nextInt(range.first, range.last + 1)

    fun date(
        system: CalendarSystem,
        years: IntRange,
    ): CalendarDate {
        val calendar = calendar(system)
        val year = number(years)
        val month = number(1..calendar.monthsInYear(year))
        return CalendarDate(system, year, month, number(1..calendar.monthLength(year, month)))
    }

    fun jdn(date: CalendarDate): Jdn = calendar(date.system).toJdn(date)

    fun months(
        system: CalendarSystem,
        reference: Jdn,
        count: Int,
    ): Jdn = calendar(system).let { it.toJdn(it.addMonths(it.fromJdn(reference), count)) }

    fun monthName(
        language: LanguageSpec,
        system: CalendarSystem,
        month: Int,
    ): String {
        val names = FormatTable.of(language).monthNames[system] ?: requireNotNull(language.monthNames.forSystem(system))
        return names[month - 1]
    }

    fun weekdayName(
        language: LanguageSpec,
        day: Weekday,
    ): String = FormatTable.of(language).weekdays.getValue(CalendarSystem.GREGORIAN)[day.ordinal]

    fun weekday(): Weekday = Weekday.entries[random.nextInt(Weekday.entries.size)]

    fun persianDigits(text: String): String = Numerals.localizeDigits(text, NumeralSystem.PERSIAN)

    /** The first day strictly after [reference] that falls on [day]. */
    fun next(
        reference: Jdn,
        day: Weekday,
    ): Jdn = (1..DAYS_PER_WEEK).map { reference + it }.first { it.weekday() == day }

    /** The last day strictly before [reference] that falls on [day]. */
    fun previous(
        reference: Jdn,
        day: Weekday,
    ): Jdn = (1..DAYS_PER_WEEK).map { reference - it }.first { it.weekday() == day }

    /** [reference] itself or the next day that falls on [day]. */
    fun upcoming(
        reference: Jdn,
        day: Weekday,
    ): Jdn = (0 until DAYS_PER_WEEK).map { reference + it }.first { it.weekday() == day }

    companion object {
        /** JDN of 2020-01-01 (2000-01-01 is JDN 2451545; 7305 days later). */
        const val REFERENCE_START = 2_458_850L
        const val REFERENCE_DAYS = 5_479
        const val DAYS_PER_WEEK = 7

        fun id(
            category: String,
            index: Int,
        ): String = "$category-${(index + 1).toString().padStart(3, '0')}"
    }
}
