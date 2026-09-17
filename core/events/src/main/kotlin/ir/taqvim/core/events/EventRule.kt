/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.events

import ir.taqvim.core.calendar.NepaliLunarDays
import ir.taqvim.core.calendar.TithiObservance
import ir.taqvim.core.model.Weekday
import kotlinx.datetime.TimeZone

private const val MAX_MONTH = 12
private const val MAX_WEEK_OF_MONTH = 5
private const val MAX_TITHI = NepaliLunarDays.TITHIS_IN_MONTH

private fun requireMonth(month: Int) = require(month in 1..MAX_MONTH) { "month must be in 1..12 (was $month)" }

/** Astronomical events that can anchor a rule. */
public enum class AstroKind {
    MARCH_EQUINOX,
    JUNE_SOLSTICE,
    SEPTEMBER_EQUINOX,
    DECEMBER_SOLSTICE,
    NEW_MOON,
    FULL_MOON,
}

/** How an event's days are found in a year of its calendar (docs/PLAN.md §4.2). */
public sealed interface EventRule {
    /** Every year on [month]-[day]; years without that day have no occurrence (e.g. 30 Esfand). */
    public data class Fixed(
        public val month: Int,
        public val day: Int,
    ) : EventRule {
        init {
            requireMonth(month)
            require(day >= 1) { "day must be ≥ 1 (was $day)" }
        }
    }

    /** The [n]th [weekday] of [month]; months with fewer such weekdays have no occurrence. */
    public data class NthWeekdayOfMonth(
        public val month: Int,
        public val weekday: Weekday,
        public val n: Int,
    ) : EventRule {
        init {
            requireMonth(month)
            require(n in 1..MAX_WEEK_OF_MONTH) { "n must be in 1..5 (was $n)" }
        }
    }

    /** [offsetDays] after the last [weekday] of [month] (the result may leave the month). */
    public data class LastWeekdayOfMonth(
        public val month: Int,
        public val weekday: Weekday,
        public val offsetDays: Int = 0,
    ) : EventRule {
        init {
            requireMonth(month)
        }
    }

    /** The last day of [month]. */
    public data class LastDayOfMonth(
        public val month: Int,
    ) : EventRule {
        init {
            requireMonth(month)
        }
    }

    /** Only once, on [year]-[month]-[day]. */
    public data class Single(
        public val year: Int,
        public val month: Int,
        public val day: Int,
    ) : EventRule {
        init {
            requireMonth(month)
            require(day >= 1) { "day must be ≥ 1 (was $day)" }
        }
    }

    /** Day [n] of the year (1 = first day); years with fewer days have no occurrence. */
    public data class NthDayOfYear(
        public val n: Int,
    ) : EventRule {
        init {
            require(n >= 1) { "n must be ≥ 1 (was $n)" }
        }
    }

    /** [offsetDays] after each occurrence of the event [eventId]. */
    public data class RelativeToEvent(
        public val eventId: EventId,
        public val offsetDays: Int,
    ) : EventRule

    /** [offsetDays] after the local day (in [timeZone]) of each [kind] instant. */
    public data class Astronomical(
        public val kind: AstroKind,
        public val offsetDays: Int,
        public val timeZone: String,
    ) : EventRule {
        init {
            require(runCatching { TimeZone.of(timeZone) }.isSuccess) { "unknown time zone '$timeZone'" }
        }
    }

    /**
     * A Bikram Sambat lunar festival (ADR-0038): the day of [tithi] (1‥30, 16‥30 the dark half) of amanta lunar [month]
     * (named by the solar month of its new moon), read at [observance]. It lasts until the day of the next [endTithi] (or
     * its own day without one) plus [endOffsetDays]. Only meaningful in the NEPALI calendar; see [NepaliLunarDays].
     */
    public data class LunarTithi(
        public val month: Int,
        public val tithi: Int,
        public val observance: TithiObservance = TithiObservance.SUNRISE,
        public val endTithi: Int? = null,
        public val endOffsetDays: Int = 0,
    ) : EventRule {
        init {
            requireMonth(month)
            require(tithi in 1..MAX_TITHI) { "tithi must be in 1..30 (was $tithi)" }
            require(endTithi == null || endTithi in 1..MAX_TITHI) { "endTithi must be in 1..30 (was $endTithi)" }
            require(endOffsetDays >= 0) { "endOffsetDays must be ≥ 0 (was $endOffsetDays)" }
        }
    }
}
