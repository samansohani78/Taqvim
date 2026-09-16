/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.events

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.ics.RecurrenceEngine
import ir.taqvim.core.model.Jdn
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * One upcoming occurrence of the edited series as the user will see it: its [day] and, for a timed event, the
 * [minute] of the day, both in the viewer's zone.
 */
internal data class PreviewSlot(
    val day: Jdn,
    val minute: Int?,
)

/**
 * The next [SIZE] occurrences of the form's repetition from [today] on (review F02), expanded by the same
 * [RecurrenceEngine] as the calendar and then moved from the event's zone into [viewZoneId], so a zone change or DST
 * transition shows as a different local time. Empty while the event does not repeat or the form has errors.
 */
internal object RecurrencePreview {
    const val SIZE: Int = 10

    private const val MINUTES_PER_HOUR = 60

    fun slots(
        form: EditorForm,
        calendar: CalendarArithmetic,
        today: Jdn,
        viewZoneId: String,
    ): List<PreviewSlot> {
        val repeat = form.repeat ?: return emptyList()
        if (EventValidator.validate(form, calendar).isNotEmpty()) return emptyList()
        val days = RecurrenceEngine(calendar).occurrences(form.start, repeat.toRule(calendar), today).take(SIZE)
        return days.map { day -> slot(form, day, viewZoneId) }.toList()
    }

    private fun slot(
        form: EditorForm,
        day: Jdn,
        viewZoneId: String,
    ): PreviewSlot {
        if (form.allDay) return PreviewSlot(day, null)
        val time = LocalTime(form.startMinute / MINUTES_PER_HOUR, form.startMinute % MINUTES_PER_HOUR)
        val local =
            LocalDateTime(day.toLocalDate(), time)
                .toInstant(TimeZone.of(form.timeZoneId))
                .toLocalDateTime(TimeZone.of(viewZoneId))
        return PreviewSlot(local.date.toJdn(), local.hour * MINUTES_PER_HOUR + local.minute)
    }
}
