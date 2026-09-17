/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ics

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.Jdn

/**
 * One personal series in its own calendar, the start of the occurrence pipeline that the calendar, reminders, search,
 * export and the editor share (ADR-0035): [start] repeats by [rule] (once when `null`), each occurrence lasts
 * [lengthDays] more days, [excluded] days do not occur and [overrides] replace occurrences by their original day.
 * Surfaces map the resulting [SeriesInstance]s to their own records and date them; they never expand rules themselves.
 */
public data class OccurrenceSeries<out T>(
    public val calendar: CalendarArithmetic,
    public val start: CalendarDate,
    public val rule: RecurrenceRule?,
    public val lengthDays: Long = 0,
    public val excluded: Set<Jdn> = emptySet(),
    public val overrides: Map<Jdn, T> = emptyMap(),
) {
    init {
        require(start.system == calendar.system) { "start must be a ${calendar.system} date (was ${start.system})" }
        require(lengthDays >= 0) { "lengthDays must not be negative (was $lengthDays)" }
    }

    /** The day of the first occurrence. */
    public val first: Jdn
        get() = calendar.toJdn(start)

    /**
     * Occurrence days on or after [from] (all when `null`), ascending and lazy, before [excluded] and [overrides] are
     * applied; the rule is sought to [from] rather than walked from [start].
     */
    public fun starts(from: Jdn? = null): Sequence<Jdn> {
        val rule = rule ?: return sequenceOf(first).filter { from == null || it >= from }
        return RecurrenceEngine(calendar).occurrences(start, rule, from)
    }

    /** Whether [day] is an occurrence day of the rule (exclusions aside); a one-off series has only [first]. */
    public fun isOccurrence(day: Jdn): Boolean = starts(day).firstOrNull() == day

    /**
     * The instances that can take place from [from] to [until]: [excluded] days removed, instances overlapping the
     * days kept (an occurrence that started up to [lengthDays] earlier still overlaps), and overridden instances kept
     * wherever their original day lies because the override may move them — callers check where those land.
     */
    public fun instances(
        from: Jdn,
        until: Jdn,
    ): List<SeriesInstance<T>> {
        val earliest = from - lengthDays
        val seek = overrides.keys.minOrNull()?.let { minOf(it, earliest) } ?: earliest
        val days = if (rule == null) sequenceOf(first) else starts(seek)
        return seriesInstances(days, excluded, overrides, earliest, until)
    }
}
