/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events

import ir.taqvim.core.events.CalendarProvider
import ir.taqvim.core.model.Jdn
import kotlinx.datetime.TimeZone

/**
 * The days personal events are shown on, for surfaces other than the calendar (search, T-804). Occurrences come from
 * the same expansion as the calendar (recurrence, exception days, moved and cancelled occurrences) and are dated by
 * the ADR-0031 rule, so a result opens on the day the calendar shows it.
 */
object PersonalEventDays {
    /**
     * The first day on or after [today] that [record] is shown on in [zone], or `null` when it has no such day.
     * Recurring events are looked up [window] days ahead; a one-off event is found however far ahead it is. An
     * occurrence that started earlier and still runs gives [today].
     */
    fun nextShownDay(
        record: PersonalEventRecord,
        calendars: CalendarProvider,
        today: Jdn,
        zone: TimeZone,
        window: Int,
    ): Jdn? {
        val ahead = today + window.toLong()
        val until = if (record.recurrence == null) maxOf(ahead, Jdn(record.event.endJdn) + 1) else ahead
        return PersonalExpansion
            .expand(record, EventDays.widened(today..until), calendars)
            .map { EventDays.of(it, zone) }
            .filter { it.endInclusive >= today && it.start <= until }
            .minOfOrNull { maxOf(it.start, today) }
    }
}
