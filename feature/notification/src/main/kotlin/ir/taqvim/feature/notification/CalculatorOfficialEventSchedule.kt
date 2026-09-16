/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import ir.taqvim.core.events.AstronomicalEventSource
import ir.taqvim.core.events.EventDefinition
import ir.taqvim.core.events.EventId
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.events.Occurrence
import ir.taqvim.core.events.OccurrenceCalculator
import ir.taqvim.core.events.SourceCalendars
import ir.taqvim.core.model.Jdn

/**
 * [OfficialEventSchedule] over the T-300 occurrence calculator (T-1002: reminders attach to an [EventId] and follow the
 * event from year to year). Every year of the event's calendar that overlaps the requested days is computed, so an
 * occurrence right after a year boundary is found; occurrences outside the event's validity are left out.
 *
 * Each event is computed with the calendars of its own source ([sourceCalendars]), exactly as the calendar display does
 * (e.g. Iranian official events always follow the Iranian official lunar calendar, ADR-0010), so a reminder never
 * fires for a different day than the one shown.
 */
class CalculatorOfficialEventSchedule(
    definitions: Collection<EventDefinition>,
    private val languageTag: String,
    private val sourceCalendars: SourceCalendars = SourceCalendars.DEFAULT,
    astronomy: AstronomicalEventSource? = null,
) : OfficialEventSchedule {
    private val byId: Map<EventId, EventDefinition> = definitions.associateBy { it.id }
    private val calculators: Map<EventSource, OccurrenceCalculator> =
        definitions
            .map { it.source }
            .distinct()
            .associateWith { OccurrenceCalculator(definitions, sourceCalendars.providerFor(it), astronomy) }

    override fun title(eventId: EventId): String? = byId[eventId]?.title?.forLanguage(languageTag)

    override fun days(
        eventId: EventId,
        from: Jdn,
        until: Jdn,
    ): List<Jdn> {
        val definition = byId[eventId] ?: return emptyList()
        val calendar = sourceCalendars.providerFor(definition.source).calendarFor(definition.calendar)
        val calculator = calculators[definition.source]
        if (calendar == null || calculator == null || from > until) return emptyList()
        return (calendar.fromJdn(from).year..calendar.fromJdn(until).year)
            .flatMap { year -> runCatching { calculator.occurrences(definition, year) }.getOrDefault(emptyList()) }
            .filter { it.jdn >= from && it.jdn <= until && isValid(it) }
            .map { it.jdn }
            .distinct()
            .sorted()
    }

    private fun isValid(occurrence: Occurrence): Boolean {
        val validity = occurrence.definition.validity ?: return true
        val calendars = sourceCalendars.providerFor(occurrence.definition.source)
        val year =
            if (validity.calendar == occurrence.definition.calendar) {
                occurrence.date.year
            } else {
                calendars.calendarFor(validity.calendar)?.fromJdn(occurrence.jdn)?.year
            }
        return year != null && validity.contains(year)
    }
}
