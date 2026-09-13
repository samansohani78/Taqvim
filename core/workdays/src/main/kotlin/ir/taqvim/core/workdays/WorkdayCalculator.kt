/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.workdays

import ir.taqvim.core.events.EventFlag
import ir.taqvim.core.events.EventLookup
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.events.HolidayCalendar
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import kotlin.math.abs

/** Personal leave from [first] to [last], both inclusive. */
public data class LeaveRange(
    public val first: Jdn,
    public val last: Jdn,
) {
    init {
        require(first <= last) { "leave must not end before it starts" }
    }

    /** Whether [jdn] is a day of this leave. */
    public operator fun contains(jdn: Jdn): Boolean = jdn >= first && jdn <= last
}

/** How a day whose only holidays are flagged [EventFlag.HALF_DAY] counts. */
public enum class HalfDayPolicy {
    /** As half a workday (0.5). */
    HALF,

    /** As a full workday (1.0). */
    FULL_WORKDAY,
}

/**
 * A user's working pattern (F-07): [weekend] days, the [holidaySources] whose holidays are days off, the [halfDays]
 * policy and [personalLeave]. The weekend is always configured by the user or taken from a language; Taqvim ships no
 * official weekend data.
 */
public data class WorkdayProfile(
    public val weekend: Set<Weekday>,
    public val holidaySources: Set<EventSource>,
    public val halfDays: HalfDayPolicy = HalfDayPolicy.HALF,
    public val personalLeave: List<LeaveRange> = emptyList(),
)

/** Outcome of a workday search. */
public sealed interface WorkdayResult {
    /** The workday [jdn]. */
    public data class Found(
        public val jdn: Jdn,
    ) : WorkdayResult

    /** No workday within [searchedDays] consecutive days (e.g. every weekday is a weekend day). */
    public data class NoWorkday(
        public val searchedDays: Int,
    ) : WorkdayResult
}

/**
 * Workday arithmetic (T-504) on top of [HolidayCalendar] (T-303).
 *
 * - A day is worth 0 when it is a weekend day, a personal-leave day or a holiday of an enabled source; a day whose only
 *   holidays are half days is worth 0.5 or 1 depending on [WorkdayProfile.halfDays]; every other day is worth 1.
 * - A *workday* is a day worth more than 0.
 * - Searches give up after [MAX_SEARCH_DAYS] consecutive non-workdays and return [WorkdayResult.NoWorkday].
 */
public class WorkdayCalculator(
    lookup: EventLookup,
    public val profile: WorkdayProfile,
) {
    private val holidays = HolidayCalendar(lookup, profile.holidaySources, profile.weekend)

    /** How much of a workday [jdn] is: 0, 0.5 or 1. */
    public fun workValue(jdn: Jdn): Double {
        if (holidays.isWeekend(jdn) || profile.personalLeave.any { jdn in it }) return NONE
        val reasons = holidays.holidayReasons(jdn)
        return when {
            reasons.isEmpty() -> FULL
            reasons.all { EventFlag.HALF_DAY in it.definition.flags } -> halfDayValue()
            else -> NONE
        }
    }

    /** Whether [jdn] is a (full or half) workday. */
    public fun isWorkday(jdn: Jdn): Boolean = workValue(jdn) > NONE

    /**
     * Sum of [workValue] over the half-open interval [from] until [until]; negative when [until] is before [from]
     * (then it is minus the sum from [until] until [from]).
     */
    public fun workdaysBetween(
        from: Jdn,
        until: Jdn,
    ): Double =
        if (until < from) {
            -workdaysBetween(until, from)
        } else {
            generateSequence(from) { it + 1 }.takeWhile { it < until }.sumOf(::workValue)
        }

    /** The first workday strictly after [day]. */
    public fun nextWorkday(day: Jdn): WorkdayResult = step(day, FORWARD)

    /** The last workday strictly before [day]. */
    public fun previousWorkday(day: Jdn): WorkdayResult = step(day, BACKWARD)

    /**
     * The [workdays]-th workday after [start] (before it when negative), counting half days as workdays. With 0,
     * [start] itself when it is a workday, otherwise the next workday.
     */
    public fun addWorkdays(
        start: Jdn,
        workdays: Int,
    ): WorkdayResult {
        if (workdays == 0) return if (isWorkday(start)) WorkdayResult.Found(start) else nextWorkday(start)
        val direction = if (workdays > 0) FORWARD else BACKWARD
        var result: WorkdayResult = WorkdayResult.Found(start)
        repeat(abs(workdays)) {
            result = (result as? WorkdayResult.Found)?.let { step(it.jdn, direction) } ?: result
        }
        return result
    }

    private fun step(
        from: Jdn,
        direction: Int,
    ): WorkdayResult =
        (1..MAX_SEARCH_DAYS)
            .asSequence()
            .map { from + direction * it }
            .firstOrNull(::isWorkday)
            ?.let { WorkdayResult.Found(it) }
            ?: WorkdayResult.NoWorkday(MAX_SEARCH_DAYS)

    private fun halfDayValue(): Double = if (profile.halfDays == HalfDayPolicy.HALF) HALF else FULL

    public companion object {
        /** Longest run of non-workdays a search tolerates (ten years). */
        public const val MAX_SEARCH_DAYS: Int = 3_660

        private const val NONE = 0.0
        private const val HALF = 0.5
        private const val FULL = 1.0
        private const val FORWARD = 1
        private const val BACKWARD = -1
    }
}
