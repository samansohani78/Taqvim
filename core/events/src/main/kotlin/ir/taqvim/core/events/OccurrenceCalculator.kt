/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.events

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.IranIslamicCalendar
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.lastWeekdayOfMonth
import ir.taqvim.core.calendar.nthWeekdayOfMonth
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.calendar.yearLength
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

/** Calendar arithmetic per [CalendarSystem]; `null` when a calendar is not available. */
public fun interface CalendarProvider {
    public fun calendarFor(system: CalendarSystem): CalendarArithmetic?

    public companion object {
        /** Persian (A-02), Iranian official Islamic (A-05) and Gregorian; Nepali arrives with T-105. */
        public val DEFAULT: CalendarProvider =
            CalendarProvider { system ->
                when (system) {
                    CalendarSystem.PERSIAN -> PersianCalendarSystem
                    CalendarSystem.ISLAMIC -> IRAN_ISLAMIC
                    CalendarSystem.GREGORIAN -> GregorianCalendarSystem
                    CalendarSystem.NEPALI -> null
                }
            }

        private val IRAN_ISLAMIC = IranIslamicCalendar()
    }
}

/** Instants of astronomical events, provided by `:core:astronomy` (T-403). */
public fun interface AstronomicalEventSource {
    /** The [kind] instants in [from] until [until]. */
    public fun instants(
        kind: AstroKind,
        from: Instant,
        until: Instant,
    ): List<Instant>
}

/**
 * Computes the occurrences of event definitions (T-300). All [definitions] are known up front so that
 * `RelativeToEvent` rules can be resolved; duplicate ids, unknown targets and reference cycles are rejected here.
 */
public class OccurrenceCalculator(
    definitions: Collection<EventDefinition>,
    private val calendars: CalendarProvider = CalendarProvider.DEFAULT,
    private val astronomy: AstronomicalEventSource? = null,
) {
    private val byId: Map<EventId, EventDefinition> = definitions.associateBy { it.id }

    init {
        require(byId.size == definitions.size) { "event ids must be unique" }
        byId.values.forEach(::requireResolvable)
    }

    /**
     * Occurrences of [definition] in [year] of its own calendar, sorted and without duplicates. Empty when the calendar
     * is not available. Validity and visibility are applied later by policies (T-302).
     */
    public fun occurrences(
        definition: EventDefinition,
        year: Int,
    ): List<Occurrence> {
        val calendar = calendars.calendarFor(definition.calendar) ?: return emptyList()
        val days =
            when (val rule = definition.rule) {
                is EventRule.RelativeToEvent -> relativeDays(rule, calendar, year)
                is EventRule.Astronomical -> astronomicalDays(rule, calendar, year)
                else -> directDays(rule, calendar, year)
            }
        return days.distinct().sorted().map {
            Occurrence(
                definition,
                it,
                calendar.fromJdn(it),
                definition.isHoliday,
                year,
            )
        }
    }

    private fun relativeDays(
        rule: EventRule.RelativeToEvent,
        calendar: CalendarArithmetic,
        year: Int,
    ): List<Jdn> {
        val target = requireNotNull(byId[rule.eventId]) { "event '${rule.eventId.value}' is not known" }
        val targetCalendar = calendars.calendarFor(target.calendar) ?: return emptyList()
        val (start, end) = yearSpan(calendar, year)
        val firstYear = targetCalendar.fromJdn(start - rule.offsetDays).year - 1
        val lastYear = targetCalendar.fromJdn(end - 1 - rule.offsetDays).year + 1
        return (firstYear..lastYear)
            .flatMap { occurrences(target, it) }
            .map { it.jdn + rule.offsetDays }
            .filter { it >= start && it < end }
    }

    private fun astronomicalDays(
        rule: EventRule.Astronomical,
        calendar: CalendarArithmetic,
        year: Int,
    ): List<Jdn> {
        val source = checkNotNull(astronomy) { "astronomical rules need an AstronomicalEventSource" }
        val zone = TimeZone.of(rule.timeZone)
        val (start, end) = yearSpan(calendar, year)
        val from = start.toLocalDate().atStartOfDayIn(zone)
        val until = end.toLocalDate().atStartOfDayIn(zone)
        return source
            .instants(rule.kind, from, until)
            .filter { it >= from && it < until }
            .map { it.toJdn(zone) + rule.offsetDays }
    }

    private fun requireResolvable(start: EventDefinition) {
        val path = mutableListOf(start.id)
        var current = start
        while (true) {
            val rule = current.rule as? EventRule.RelativeToEvent ?: return
            val next =
                requireNotNull(byId[rule.eventId]) {
                    "event '${current.id.value}' refers to unknown event '${rule.eventId.value}'"
                }
            require(next.id !in path) {
                "RelativeToEvent cycle: ${(path + next.id).joinToString(" → ") { it.value }}"
            }
            path += next.id
            current = next
        }
    }
}

/** JDN of 1/1 of [year] and of the following year's first day. */
private fun yearSpan(
    calendar: CalendarArithmetic,
    year: Int,
): Pair<Jdn, Jdn> = calendar.toJdn(calendar.date(year, 1, 1)) to calendar.toJdn(calendar.date(year + 1, 1, 1))

/** Days of the rules that need neither other events nor astronomy. */
private fun directDays(
    rule: EventRule,
    calendar: CalendarArithmetic,
    year: Int,
): List<Jdn> =
    when (rule) {
        is EventRule.Fixed -> {
            listOfNotNull(validDay(calendar, year, rule.month, rule.day))
        }

        is EventRule.NthWeekdayOfMonth -> {
            listOfNotNull(calendar.nthWeekdayOfMonth(year, rule.month, rule.weekday, rule.n)?.let(calendar::toJdn))
        }

        is EventRule.LastWeekdayOfMonth -> {
            listOf(calendar.toJdn(calendar.lastWeekdayOfMonth(year, rule.month, rule.weekday, rule.offsetDays)))
        }

        is EventRule.LastDayOfMonth -> {
            listOf(calendar.toJdn(calendar.date(year, rule.month, calendar.monthLength(year, rule.month))))
        }

        is EventRule.Single -> {
            if (rule.year == year) listOfNotNull(validDay(calendar, year, rule.month, rule.day)) else emptyList()
        }

        is EventRule.NthDayOfYear -> {
            if (rule.n <=
                calendar.yearLength(year)
            ) {
                listOf(yearSpan(calendar, year).first + (rule.n - 1))
            } else {
                emptyList()
            }
        }

        is EventRule.RelativeToEvent, is EventRule.Astronomical -> {
            emptyList()
        }
    }

private fun validDay(
    calendar: CalendarArithmetic,
    year: Int,
    month: Int,
    day: Int,
): Jdn? = if (calendar.isValid(year, month, day)) calendar.toJdn(calendar.date(year, month, day)) else null
