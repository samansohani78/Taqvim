/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ics

import ir.taqvim.core.model.Weekday
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

private const val MAX_WEEK_ORDINAL = 53
private const val MAX_MONTH_DAY = 31

/** A DATE or DATE-TIME value (RFC 5545 §3.3.4, §3.3.5). */
public sealed interface IcsDateTime {
    /** `VALUE=DATE`: a whole day. */
    public data class Date(
        public val date: LocalDate,
    ) : IcsDateTime

    /** Local time without a zone (§3.3.5, form #1 "floating"). */
    public data class Floating(
        public val dateTime: LocalDateTime,
    ) : IcsDateTime

    /** UTC time (§3.3.5, form #2, trailing `Z`). */
    public data class Utc(
        public val instant: Instant,
    ) : IcsDateTime

    /** Local time in the IANA zone [timeZoneId] (§3.3.5, form #3, `TZID` parameter). */
    public data class Zoned(
        public val dateTime: LocalDateTime,
        public val timeZoneId: String,
    ) : IcsDateTime
}

/** Supported RRULE frequencies (§3.3.10); sub-daily frequencies are not supported. */
public enum class Frequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
}

/** A BYDAY entry (§3.3.10): [weekday], optionally its [ordinal] occurrence in the month or year (−1 = last). */
public data class WeekdayNum(
    public val weekday: Weekday,
    public val ordinal: Int? = null,
) {
    init {
        require(ordinal == null || (ordinal in -MAX_WEEK_ORDINAL..MAX_WEEK_ORDINAL && ordinal != 0)) {
            "BYDAY ordinal must be ±1…53 (was $ordinal)"
        }
    }
}

/**
 * The supported subset of an RRULE (§3.3.10): FREQ, INTERVAL, COUNT, UNTIL, BYDAY and BYMONTHDAY. COUNT and UNTIL are
 * mutually exclusive, and UNTIL is a DATE, floating or UTC value (never zoned, which RRULE cannot express).
 */
public data class Recurrence(
    public val frequency: Frequency,
    public val interval: Int = 1,
    public val count: Int? = null,
    public val until: IcsDateTime? = null,
    public val byDay: List<WeekdayNum> = emptyList(),
    public val byMonthDay: List<Int> = emptyList(),
) {
    init {
        require(interval >= 1) { "INTERVAL must be ≥ 1 (was $interval)" }
        require(count == null || count >= 1) { "COUNT must be ≥ 1 (was $count)" }
        require(count == null || until == null) { "COUNT and UNTIL must not both be present" }
        require(until !is IcsDateTime.Zoned) { "UNTIL cannot carry a TZID" }
        require(byMonthDay.all { it != 0 && it in -MAX_MONTH_DAY..MAX_MONTH_DAY }) { "BYMONTHDAY must be ±1…31" }
    }
}

/** When an alarm fires (§3.8.6.3). */
public sealed interface AlarmTrigger {
    /** [offset] from the event start, or from its end when [relatedToEnd]. */
    public data class Relative(
        public val offset: Duration,
        public val relatedToEnd: Boolean = false,
    ) : AlarmTrigger

    /** At [instant]. */
    public data class Absolute(
        public val instant: Instant,
    ) : AlarmTrigger
}

/** A VALARM with ACTION:DISPLAY (§3.6.6). */
public data class DisplayAlarm(
    public val trigger: AlarmTrigger,
    public val description: String,
)

/** A VEVENT (§3.6.1) restricted to the properties Taqvim imports and exports. */
public data class IcsEvent(
    public val uid: String,
    public val start: IcsDateTime,
    public val end: IcsDateTime? = null,
    public val summary: String? = null,
    public val description: String? = null,
    public val recurrence: Recurrence? = null,
    public val exceptionDates: List<IcsDateTime> = emptyList(),
    public val alarms: List<DisplayAlarm> = emptyList(),
    /** RDATE values (§3.8.5.2) of the DATE or DATE-TIME type; PERIOD values are not supported. */
    public val recurrenceDates: List<IcsDateTime> = emptyList(),
    /** Non-standard `X-` properties (§3.8.8.2) by upper-case name with their TEXT value; the first of a name wins. */
    public val extensions: Map<String, String> = emptyMap(),
    /**
     * RECURRENCE-ID (§3.8.4.4): the original start of the instance of the series with the same [uid] that this
     * component overrides, or `null` for the series itself or a one-off event.
     */
    public val recurrenceId: IcsDateTime? = null,
    /** `STATUS:CANCELLED` (§3.8.1.11); a cancelled override removes its instance. */
    public val cancelled: Boolean = false,
) {
    init {
        require(extensions.keys.all { X_NAME.matches(it) }) { "extension names must be X-names (§3.1)" }
    }

    private companion object {
        val X_NAME = Regex("X-[A-Z0-9-]+")
    }
}

/** An iCalendar object (§3.4) with its PRODID and events. */
public data class IcsCalendar(
    public val productId: String,
    public val events: List<IcsEvent>,
)

/** A problem found at physical [line] (1-based) of the input. */
public data class IcsProblem(
    public val line: Int,
    public val message: String,
)

/** Outcome of reading iCalendar text; the reader never throws. */
public sealed interface IcsParseResult {
    /** The [calendar], with [warnings] about content that was ignored or approximated. */
    public data class Success(
        public val calendar: IcsCalendar,
        public val warnings: List<IcsProblem>,
    ) : IcsParseResult

    /** The input is not a readable iCalendar stream. */
    public data class Failure(
        public val errors: List<IcsProblem>,
    ) : IcsParseResult
}
