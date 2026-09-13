/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ics

import ir.taqvim.core.model.Weekday
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** DATE, DATE-TIME, DURATION and RECUR values (RFC 5545 §3.3.4, §3.3.5, §3.3.6, §3.3.10). */
internal object IcsValues {
    private val DATE = Regex("""(\d{4})(\d{2})(\d{2})""")
    private val DATE_TIME = Regex("""(\d{4})(\d{2})(\d{2})T(\d{2})(\d{2})(\d{2})(Z?)""")
    private val DURATION = Regex("""([+-]?)P(?:(\d+)W|(?:(\d+)D)?(?:T(?=\d)(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?)?)""")
    private val WEEKDAY_NUM = Regex("""([+-]?\d{1,2})?(MO|TU|WE|TH|FR|SA|SU)""")
    private val WEEKDAY_CODES =
        mapOf(
            "MO" to Weekday.MONDAY,
            "TU" to Weekday.TUESDAY,
            "WE" to Weekday.WEDNESDAY,
            "TH" to Weekday.THURSDAY,
            "FR" to Weekday.FRIDAY,
            "SA" to Weekday.SATURDAY,
            "SU" to Weekday.SUNDAY,
        )
    private val SUPPORTED_RECUR_PARTS = setOf("FREQ", "INTERVAL", "COUNT", "UNTIL", "BYDAY", "BYMONTHDAY")
    private const val MAX_SECOND = 59
    private const val DATE_FIELDS = 3
    private const val WEEKS = 0
    private const val DAYS = 1
    private const val HOURS = 2
    private const val MINUTES = 3
    private const val SECONDS = 4
    private const val DAYS_PER_WEEK = 7L
    private const val SECONDS_PER_MINUTE = 60L
    private const val SECONDS_PER_HOUR = 3_600L
    private const val SECONDS_PER_DAY = 86_400L

    /** The value of a date-time [text] under [property]'s VALUE and TZID parameters; `null` if invalid. */
    fun dateTime(
        text: String,
        property: ContentLine,
        warnings: MutableList<IcsProblem>,
    ): IcsDateTime? =
        if (property.parameter("VALUE").equals("DATE", ignoreCase = true)) {
            date(text)?.let { IcsDateTime.Date(it) }
        } else {
            localOrUtc(text, property.parameter("TZID"), property.line, warnings)
        }

    /** A value whose type is recognised from its shape alone (RRULE UNTIL): DATE, UTC or floating. */
    fun untilValue(text: String): IcsDateTime? =
        if (DATE.matches(text)) date(text)?.let { IcsDateTime.Date(it) } else localOrUtc(text, null, 0, mutableListOf())

    private fun date(text: String): LocalDate? =
        DATE.matchEntire(text)?.destructured?.let { (year, month, day) ->
            runCatching { LocalDate(year.toInt(), month.toInt(), day.toInt()) }.getOrNull()
        }

    private fun localOrUtc(
        text: String,
        timeZoneId: String?,
        line: Int,
        warnings: MutableList<IcsProblem>,
    ): IcsDateTime? {
        val groups = DATE_TIME.matchEntire(text)?.groupValues?.drop(1) ?: return null
        val (year, month, day) = groups.map { it.toIntOrNull() ?: 0 }
        val (hour, minute, second) = groups.drop(DATE_FIELDS).map { it.toIntOrNull() ?: 0 }
        val local =
            runCatching { LocalDateTime(year, month, day, hour, minute, minOf(second, MAX_SECOND)) }.getOrNull()
                ?: return null
        return when {
            groups.last() == "Z" -> {
                IcsDateTime.Utc(local.toInstant(TimeZone.UTC))
            }

            timeZoneId == null -> {
                IcsDateTime.Floating(local)
            }

            runCatching { TimeZone.of(timeZoneId) }.isSuccess -> {
                IcsDateTime.Zoned(local, timeZoneId)
            }

            else -> {
                warnings += IcsProblem(line, "unknown TZID '$timeZoneId'; treated as floating time")
                IcsDateTime.Floating(local)
            }
        }
    }

    /** Property parameters (`;VALUE=DATE` or `;TZID=…`) and value text of [value]. */
    fun format(value: IcsDateTime): Pair<String, String> =
        when (value) {
            is IcsDateTime.Date -> ";VALUE=DATE" to basicDate(value.date)
            is IcsDateTime.Floating -> "" to basicDateTime(value.dateTime)
            is IcsDateTime.Utc -> "" to basicDateTime(value.instant.toLocalDateTime(TimeZone.UTC)) + "Z"
            is IcsDateTime.Zoned -> ";TZID=${value.timeZoneId}" to basicDateTime(value.dateTime)
        }

    private fun basicDate(date: LocalDate): String = date.toString().replace("-", "")

    private fun basicDateTime(dateTime: LocalDateTime): String =
        basicDate(dateTime.date) + "T" + "%02d%02d%02d".format(dateTime.hour, dateTime.minute, dateTime.second)

    /** A DURATION value (§3.3.6) in whole seconds; `null` when malformed. */
    fun duration(text: String): Duration? {
        val match = DURATION.matchEntire(text) ?: return null
        val texts = match.groupValues.drop(1)
        val parts = texts.drop(1).map { if (it.isEmpty()) 0L else it.toLongOrNull() }
        if (parts.any { it == null } || texts.drop(1).all { it.isEmpty() }) return null
        val value = { index: Int -> parts[index] ?: 0L }
        val total =
            (value(WEEKS) * DAYS_PER_WEEK).days + value(DAYS).days + value(HOURS).hours + value(MINUTES).minutes +
                value(SECONDS).seconds
        return if (texts.first() == "-") -total else total
    }

    /** [duration] as a DURATION value, in weeks when exact, otherwise days, hours, minutes and seconds. */
    fun formatDuration(duration: Duration): String {
        val sign = if (duration.isNegative()) "-" else ""
        val total = duration.absoluteValue.inWholeSeconds
        if (total == 0L) return "PT0S"
        if (total % (DAYS_PER_WEEK * SECONDS_PER_DAY) ==
            0L
        ) {
            return "${sign}P${total / (DAYS_PER_WEEK * SECONDS_PER_DAY)}W"
        }
        val days = total / SECONDS_PER_DAY
        val hours = total % SECONDS_PER_DAY / SECONDS_PER_HOUR
        val minutes = total % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
        val seconds = total % SECONDS_PER_MINUTE
        val time =
            listOf(hours to "H", minutes to "M", seconds to "S")
                .filter {
                    it.first > 0
                }.joinToString("") { "${it.first}${it.second}" }
        return sign + "P" + (if (days > 0) "${days}D" else "") + (if (time.isEmpty()) "" else "T$time")
    }

    /** The supported parts of an RRULE; `null` when FREQ is missing or unsupported or a part is invalid. */
    fun recurrence(
        property: ContentLine,
        warnings: MutableList<IcsProblem>,
    ): Recurrence? {
        val parts =
            property.value
                .split(';')
                .filter { it.isNotEmpty() }
                .associate { it.substringBefore('=').uppercase() to it.substringAfter('=', "") }
        val unsupported = parts.keys - SUPPORTED_RECUR_PARTS
        if (unsupported.isNotEmpty()) {
            warnings +=
                IcsProblem(property.line, "unsupported RRULE parts ignored: ${unsupported.sorted().joinToString()}")
        }
        val frequency = Frequency.entries.firstOrNull { it.name == parts["FREQ"]?.uppercase() } ?: return null
        return runCatching {
            Recurrence(
                frequency = frequency,
                interval = parts["INTERVAL"]?.toInt() ?: 1,
                count = parts["COUNT"]?.toInt(),
                until = parts["UNTIL"]?.let { requireNotNull(untilValue(it)) { "invalid UNTIL" } },
                byDay = parts["BYDAY"]?.split(',')?.map(::weekdayNum).orEmpty(),
                byMonthDay = parts["BYMONTHDAY"]?.split(',')?.map(String::toInt).orEmpty(),
            )
        }.getOrNull()
    }

    private fun weekdayNum(text: String): WeekdayNum {
        val match = requireNotNull(WEEKDAY_NUM.matchEntire(text.uppercase())) { "invalid BYDAY '$text'" }
        val (ordinal, code) = match.destructured
        return WeekdayNum(WEEKDAY_CODES.getValue(code), ordinal.takeIf { it.isNotEmpty() }?.toInt())
    }

    /** [recurrence] as an RRULE value. */
    fun format(recurrence: Recurrence): String =
        buildList {
            add("FREQ=${recurrence.frequency.name}")
            if (recurrence.interval != 1) add("INTERVAL=${recurrence.interval}")
            recurrence.count?.let { add("COUNT=$it") }
            recurrence.until?.let { add("UNTIL=${format(it).second}") }
            if (recurrence.byDay.isNotEmpty()) add("BYDAY=" + recurrence.byDay.joinToString(",", transform = ::code))
            if (recurrence.byMonthDay.isNotEmpty()) add("BYMONTHDAY=" + recurrence.byMonthDay.joinToString(","))
        }.joinToString(";")

    private fun code(day: WeekdayNum): String =
        (day.ordinal?.toString() ?: "") + WEEKDAY_CODES.entries.first { it.value == day.weekday }.key
}
