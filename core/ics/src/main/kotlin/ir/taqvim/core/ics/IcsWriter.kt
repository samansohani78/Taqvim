/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ics

import kotlin.time.Instant

/** Writes [IcsCalendar] as RFC 5545 text: CRLF line breaks, lines folded at 75 octets, TEXT values escaped. */
public object IcsWriter {
    private const val CRLF = "\r\n"

    /** [calendar] as iCalendar text; [stamp] is written as every event's DTSTAMP (required by §3.6.1). */
    public fun write(
        calendar: IcsCalendar,
        stamp: Instant,
    ): String =
        buildList {
            add("BEGIN:VCALENDAR")
            add("VERSION:2.0")
            add("PRODID:${ContentLines.escapeText(calendar.productId)}")
            calendar.events.forEach { addAll(event(it, stamp)) }
            add("END:VCALENDAR")
        }.flatMap(ContentLines::fold).joinToString(separator = CRLF, postfix = CRLF)

    private fun event(
        event: IcsEvent,
        stamp: Instant,
    ): List<String> =
        buildList {
            add("BEGIN:VEVENT")
            add("UID:${ContentLines.escapeText(event.uid)}")
            add(dateTime("DTSTAMP", IcsDateTime.Utc(stamp)))
            add(dateTime("DTSTART", event.start))
            event.end?.let { add(dateTime("DTEND", it)) }
            event.summary?.let { add("SUMMARY:${ContentLines.escapeText(it)}") }
            event.description?.let { add("DESCRIPTION:${ContentLines.escapeText(it)}") }
            event.recurrence?.let { add("RRULE:${IcsValues.format(it)}") }
            event.exceptionDates.forEach { add(dateTime("EXDATE", it)) }
            event.alarms.forEach { addAll(alarm(it)) }
            add("END:VEVENT")
        }

    private fun dateTime(
        name: String,
        value: IcsDateTime,
    ): String {
        val (parameters, text) = IcsValues.format(value)
        return "$name$parameters:$text"
    }

    private fun alarm(alarm: DisplayAlarm): List<String> =
        listOf(
            "BEGIN:VALARM",
            "ACTION:DISPLAY",
            "DESCRIPTION:${ContentLines.escapeText(alarm.description)}",
            trigger(alarm.trigger),
            "END:VALARM",
        )

    private fun trigger(trigger: AlarmTrigger): String =
        when (trigger) {
            is AlarmTrigger.Relative -> {
                val related = if (trigger.relatedToEnd) ";RELATED=END" else ""
                "TRIGGER$related:${IcsValues.formatDuration(trigger.offset)}"
            }

            is AlarmTrigger.Absolute -> {
                "TRIGGER;VALUE=DATE-TIME:${IcsValues.format(IcsDateTime.Utc(trigger.instant)).second}"
            }
        }
}
