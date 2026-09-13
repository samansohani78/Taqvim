/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.events

import ir.taqvim.core.calendar.JDN_OF_UNIX_EPOCH
import ir.taqvim.core.calendar.TabularIslamicCalendar
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import java.time.LocalDate

/** A synthetic event for tests (no real-world data). */
internal fun event(
    id: String,
    calendar: CalendarSystem,
    rule: EventRule,
    isHoliday: Boolean = false,
) = EventDefinition(
    id = EventId(id),
    calendar = calendar,
    source = EventSource.INTERNATIONAL,
    category = EventCategory.CULTURAL,
    isHoliday = isHoliday,
    title = LocalizedText(mapOf(LocalizedText.PERSIAN to "test")),
    rule = rule,
)

/** The day as a `java.time` date, for oracle comparisons. */
internal fun Jdn.toJavaDate(): LocalDate = LocalDate.ofEpochDay(value - JDN_OF_UNIX_EPOCH)

/** JDN of a `java.time` date. */
internal fun LocalDate.toJdnValue(): Long = toEpochDay() + JDN_OF_UNIX_EPOCH

/** Default calendars, but the civil tabular Islamic calendar (so ICU4J's islamic-civil can be the oracle). */
internal val TABULAR_ISLAMIC_CALENDARS =
    CalendarProvider { system ->
        if (system ==
            CalendarSystem.ISLAMIC
        ) {
            TabularIslamicCalendar.TYPE_II
        } else {
            CalendarProvider.DEFAULT.calendarFor(system)
        }
    }
