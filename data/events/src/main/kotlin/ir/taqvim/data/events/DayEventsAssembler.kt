/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.HijriDateResolver
import ir.taqvim.core.calendar.IranIslamicCalendar
import ir.taqvim.core.events.AstronomicalEventSource
import ir.taqvim.core.events.CalendarProvider
import ir.taqvim.core.events.EventDefinition
import ir.taqvim.core.events.EventLookup
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.events.EventVisibilityPolicy
import ir.taqvim.core.events.HolidayCalendar
import ir.taqvim.core.events.IslamicCalendarSelection
import ir.taqvim.core.events.Occurrence
import ir.taqvim.core.ics.RecurrenceEngine
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.data.database.IcsEventCacheEntity
import ir.taqvim.data.devicecalendar.DeviceEvent
import ir.taqvim.data.devicecalendar.DeviceEventMapping
import ir.taqvim.data.events.generated.OfficialEvents
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

/** The dataset [definitions] and the [astronomy] their `Astronomical` rules need (none in the dataset so far). */
data class OfficialCatalog(
    val definitions: List<EventDefinition> = OfficialEvents.ALL,
    val astronomy: AstronomicalEventSource? = null,
)

/** Dataset lookups for one [settings] value; rebuilt only when the settings change. */
internal class OfficialView(
    catalog: OfficialCatalog,
    val settings: EventsSettings,
) {
    private val selection = IslamicCalendarSelection(settings.preferences.islamicVariant)
    private val lookup = EventLookup(catalog.definitions, selection, catalog.astronomy)
    private val policy = EventVisibilityPolicy(settings.preferences, selection)
    private val holidays = HolidayCalendar(lookup, settings.preferences.enabledSources, settings.weekend)

    /** Calendars of personal events: the user's Islamic variant. */
    val personalCalendars: CalendarProvider = selection.providerFor(EventSource.USER)

    /** Arithmetic of the user's Islamic variant. */
    val islamicCalendar: CalendarArithmetic = IslamicCalendarSelection.calendarFor(selection.preferredVariant)

    fun visibleOn(
        jdn: Jdn,
        zone: TimeZone,
    ): List<Occurrence> = policy.visible(lookup.eventsOn(jdn, settings.preferences.enabledSources), zone)

    fun isHoliday(jdn: Jdn): Boolean = holidays.isHoliday(jdn)

    fun isWeekend(jdn: Jdn): Boolean = holidays.isWeekend(jdn)
}

/** The latest value of every input of a day range. */
internal data class Snapshot(
    val official: OfficialView,
    val personal: List<PersonalEventRecord>,
    val device: List<DeviceEvent>,
    val ics: List<IcsEventCacheEntity>,
)

/** Builds [DayEvents] from a [Snapshot]; timed events are dated in [zone]. */
internal class DayEventsAssembler(
    clock: Clock,
    private val zone: TimeZone,
) {
    private val iranIslamic = IranIslamicCalendar()
    private val resolver = HijriDateResolver(clock, iranIslamic)

    fun assemble(
        days: JdnRange,
        snapshot: Snapshot,
    ): List<DayEvents> {
        val official = snapshot.official
        val personal =
            snapshot.personal
                .flatMap { PersonalExpansion.expand(it, days, official.personalCalendars) }
                .sortedWith(PERSONAL_ORDER)
        val ics = snapshot.ics.map { it.toOccurrence(zone) }
        return days.map { jdn ->
            val hijri = resolveIranian(official, jdn)
            DayEvents(
                jdn = jdn,
                islamicDate = hijri?.date ?: official.islamicCalendar.fromJdn(jdn),
                hijri = hijri,
                official = official.visibleOn(jdn, zone),
                isHoliday = official.isHoliday(jdn),
                isWeekend = official.isWeekend(jdn),
                personal = personal.filter { jdn in it.days },
                device = snapshot.device.filter { jdn in it.days },
                ics = ics.filter { jdn in it.days },
            )
        }
    }

    private fun resolveIranian(
        official: OfficialView,
        jdn: Jdn,
    ) = if (official.settings.preferences.islamicVariant == IslamicVariant.IRAN_OFFICIAL) {
        resolver.resolve(jdn, official.settings.hijriOffset)
    } else {
        null
    }

    private companion object {
        val PERSONAL_ORDER: Comparator<PersonalOccurrence> =
            compareBy<PersonalOccurrence> { it.days.start }
                .thenBy { it.startMinute ?: -1 }
                .thenBy { it.eventId }
    }
}

/** Occurrences of personal events in a day range (RRULE-lite in the event's own calendar, T-503). */
internal object PersonalExpansion {
    /** Occurrences of [record] overlapping [days]; recurring events in unavailable calendars have none. */
    fun expand(
        record: PersonalEventRecord,
        days: JdnRange,
        calendars: CalendarProvider,
    ): List<PersonalOccurrence> {
        val event = record.event
        val length = maxOf(0L, event.endJdn - event.startJdn)
        return starts(record, calendars)
            .takeWhile { it <= days.endInclusive }
            .filter { it + length >= days.start }
            .map { start ->
                PersonalOccurrence(
                    eventId = event.id,
                    title = event.title,
                    notes = event.notes,
                    calendarSystem = event.calendarSystem,
                    days = start..(start + length),
                    startMinute = event.startMinute,
                    endMinute = event.endMinute,
                    timeZoneId = event.timeZoneId,
                    colorArgb = event.colorArgb,
                    recurring = record.recurrence != null,
                )
            }.toList()
    }

    private fun starts(
        record: PersonalEventRecord,
        calendars: CalendarProvider,
    ): Sequence<Jdn> {
        val first = Jdn(record.event.startJdn)
        val rule = record.recurrence ?: return sequenceOf(first)
        val calendar = calendars.calendarFor(record.event.calendarSystem) ?: return emptySequence()
        return RecurrenceEngine(calendar).occurrences(calendar.fromJdn(first), rule)
    }
}

/** This cache row as an [IcsOccurrence]; all-day rows are UTC-midnight bounded like device instances. */
internal fun IcsEventCacheEntity.toOccurrence(zone: TimeZone): IcsOccurrence =
    IcsOccurrence(
        subscriptionId = subscriptionId,
        uid = uid,
        summary = summary,
        location = location,
        begin = Instant.fromEpochMilliseconds(startEpochMillis),
        end = Instant.fromEpochMilliseconds(endEpochMillis),
        allDay = allDay,
        days = DeviceEventMapping.days(startEpochMillis, endEpochMillis, allDay, zone),
    )
