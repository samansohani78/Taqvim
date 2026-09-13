/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events

import ir.taqvim.core.calendar.ResolvedHijriDate
import ir.taqvim.core.events.Occurrence
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.data.devicecalendar.DeviceEvent
import kotlin.time.Instant

/** One occurrence of a personal event covering [days]; `null` minutes mark an all-day event. */
data class PersonalOccurrence(
    val eventId: Long,
    val title: String,
    val notes: String,
    val calendarSystem: CalendarSystem,
    val days: JdnRange,
    val startMinute: Int?,
    val endMinute: Int?,
    val timeZoneId: String,
    val colorArgb: Int?,
    /** Whether the occurrence was expanded from a recurrence rule. */
    val recurring: Boolean,
)

/** One cached occurrence of a subscribed iCalendar feed, covering [days] (all-day ones by their UTC dates). */
data class IcsOccurrence(
    val subscriptionId: Long,
    val uid: String,
    val summary: String,
    val location: String,
    val begin: Instant,
    val end: Instant,
    val allDay: Boolean,
    val days: JdnRange,
)

/** Everything shown for one civil day [jdn] (T-305). */
data class DayEvents(
    val jdn: Jdn,
    /** The lunar Hijri date in the user's Islamic variant. */
    val islamicDate: CalendarDate,
    /** Source-aware date for the Iranian official variant (official table > user offset > estimate), else `null`. */
    val hijri: ResolvedHijriDate?,
    /** Visible dataset occurrences, holidays first (`EventLookup.DAY_ORDER`). */
    val official: List<Occurrence>,
    /** Whether an enabled source makes the day a holiday. */
    val isHoliday: Boolean,
    /** Whether the day is one of the user's weekend days. */
    val isWeekend: Boolean,
    /** Personal occurrences, all-day ones first, then by start time. */
    val personal: List<PersonalOccurrence>,
    val device: List<DeviceEvent>,
    val ics: List<IcsOccurrence>,
)
