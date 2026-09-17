/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.ics.OccurrenceSeries
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.model.Jdn
import ir.taqvim.data.database.EventExceptionEntity
import ir.taqvim.data.database.EventOverrideEntity
import ir.taqvim.data.database.PersonalEventDao
import ir.taqvim.feature.events.PersonalEvent

/**
 * One occurrence of a repeating personal event as the editor changes it (ADR-0034), over the `event_overrides` rows
 * that the calendar, reminders and export already honour (T-1003).
 */
internal class EventOccurrenceEdits(
    private val events: PersonalEventDao,
) {
    /** The occurrence of [series] that would start on [originalDay]: its stored change, or the series moved there. */
    suspend fun load(
        series: PersonalEvent,
        calendar: CalendarArithmetic,
        originalDay: Jdn,
    ): PersonalEvent? {
        val id = series.id
        val change =
            id?.let { events.overrides(it) }?.firstOrNull { it.originalJdn == originalDay.value && !it.cancelled }
        val base = series.copy(recurrence = null)
        return when {
            id == null || !isOccurrence(series, calendar, originalDay) -> null
            change == null -> base.movedTo(calendar, originalDay)
            else -> base.changedBy(calendar, change)
        }
    }

    /** Stores [occurrence] as the change of the occurrence starting on [originalDay] of event [id]. */
    suspend fun save(
        id: Long,
        calendar: CalendarArithmetic,
        originalDay: Jdn,
        occurrence: PersonalEvent,
    ) {
        events.upsertOverrides(listOf(occurrence.toOverride(id, calendar, originalDay, cancelled = false)))
    }

    /** Cancels the occurrence starting on [originalDay] of [series]; a stored change of it is replaced. */
    suspend fun cancel(
        series: PersonalEvent,
        calendar: CalendarArithmetic,
        originalDay: Jdn,
    ) {
        val id = series.id ?: return
        val occurrence = load(series, calendar, originalDay) ?: return
        events.upsertOverrides(listOf(occurrence.toOverride(id, calendar, originalDay, cancelled = true)))
    }

    /**
     * Keeps only the exceptions and changed occurrences of event [id] whose original day is still an occurrence of
     * [rule] from [start] (a series edit may move every occurrence, ADR-0034).
     */
    suspend fun prune(
        id: Long,
        calendar: CalendarArithmetic,
        start: PersonalEvent,
        rule: RecurrenceRule,
    ) {
        val series = start.copy(recurrence = rule)
        val overrides = events.overrides(id)
        val kept = overrides.filter { isOccurrence(series, calendar, Jdn(it.originalJdn)) }
        if (kept.size != overrides.size) {
            events.deleteOverrides(id)
            events.upsertOverrides(kept)
        }
        val exceptions = events.exceptionDays(id)
        val keptDays = exceptions.filter { isOccurrence(series, calendar, Jdn(it)) }
        if (keptDays.size != exceptions.size) {
            events.deleteExceptions(id)
            events.insertExceptions(keptDays.map { EventExceptionEntity(id, it) })
        }
    }

    private fun isOccurrence(
        series: PersonalEvent,
        calendar: CalendarArithmetic,
        day: Jdn,
    ): Boolean {
        val rule = series.recurrence ?: return false
        return OccurrenceSeries<Nothing>(calendar, series.start, rule).isOccurrence(day)
    }
}

/** This event moved to start on [day], keeping its length. */
private fun PersonalEvent.movedTo(
    calendar: CalendarArithmetic,
    day: Jdn,
): PersonalEvent {
    val length = calendar.toJdn(end).value - calendar.toJdn(start).value
    return copy(start = calendar.fromJdn(day), end = calendar.fromJdn(day + length))
}

/** This event with the stored [change] of one occurrence applied; a `null` colour keeps the event's. */
private fun PersonalEvent.changedBy(
    calendar: CalendarArithmetic,
    change: EventOverrideEntity,
): PersonalEvent =
    copy(
        title = change.title,
        notes = change.notes,
        start = calendar.fromJdn(Jdn(change.startJdn)),
        end = calendar.fromJdn(Jdn(change.endJdn)),
        startMinute = change.startMinute,
        endMinute = change.endMinute,
        colorArgb = change.colorArgb ?: colorArgb,
    )

private fun PersonalEvent.toOverride(
    id: Long,
    calendar: CalendarArithmetic,
    originalDay: Jdn,
    cancelled: Boolean,
): EventOverrideEntity =
    EventOverrideEntity(
        eventId = id,
        originalJdn = originalDay.value,
        title = title,
        notes = notes,
        startJdn = calendar.toJdn(start).value,
        startMinute = startMinute,
        endJdn = calendar.toJdn(end).value,
        endMinute = endMinute,
        colorArgb = colorArgb,
        cancelled = cancelled,
    )
