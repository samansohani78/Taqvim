/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.core.model.PrayerMethod
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.praytimes.PrayerSettings
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

/** The civil day of a Gregorian date. */
internal fun gregorian(
    year: Int,
    month: Int,
    day: Int,
): Jdn = LocalDate(year, month, day).toJdn()

internal val PERSIAN_FIRST =
    CalendarSettings(
        calendars = listOf(CalendarSystem.PERSIAN, CalendarSystem.GREGORIAN, CalendarSystem.ISLAMIC),
        weekStart = Weekday.SATURDAY,
        islamicVariant = IslamicVariant.IRAN_OFFICIAL,
        languageCode = "fa",
    )

/** Tehran with its official prayer method (rounded sample coordinates, not official data). */
internal val TEHRAN =
    CalendarPlace("Tehran", Coordinates(35.69, 51.42), TimeZone.of("Asia/Tehran"), PrayerSettings(PrayerMethod.TEHRAN))

/** An instant for tests that do not look at the time of day: 18 March 2026, 12:00 in Tehran. */
internal val TEST_NOW: Instant = Instant.parse("2026-03-18T08:30:00Z")

internal class FakePlaceSource(
    initial: CalendarPlace?,
) : CalendarPlaceSource {
    val state = MutableStateFlow(initial)

    override fun place(): Flow<CalendarPlace?> = state
}

internal class FakeNowSource(
    initial: Instant,
) : NowSource {
    val state = MutableStateFlow(initial)

    override fun now(): Flow<Instant> = state
}

internal class FakeSettingsSource(
    initial: CalendarSettings,
) : CalendarSettingsSource {
    val state = MutableStateFlow(initial)

    override fun settings(): Flow<CalendarSettings> = state
}

internal class FakeTodaySource(
    initial: Jdn?,
) : TodaySource {
    val state = MutableStateFlow(initial)

    override fun today(): Flow<Jdn> = state.filterNotNull()
}

/** One official event per day; days in [holidays] are holidays. Also serves ranges of days (T-801). */
internal class FakeDaySource :
    CalendarDaySource,
    CalendarMonthSource {
    val holidays = MutableStateFlow(emptySet<Jdn>())

    /** Every range requested through [days], in order. */
    val requestedRanges = mutableListOf<JdnRange>()

    override fun day(jdn: Jdn): Flow<CalendarDay> = holidays.map { dayOf(jdn, it) }

    override fun days(range: JdnRange): Flow<List<CalendarDay>> {
        requestedRanges += range
        return holidays.map { holidays -> range.map { dayOf(it, holidays) } }
    }

    private fun dayOf(
        jdn: Jdn,
        holidays: Set<Jdn>,
    ): CalendarDay {
        val holiday = jdn in holidays
        return CalendarDay(jdn, holiday, isWeekend = false, listOf(eventOn(jdn, holiday)))
    }

    companion object {
        fun eventOn(
            jdn: Jdn,
            holiday: Boolean = false,
        ): DayEventItem = DayEventItem("event-${jdn.value}", DayEventKind.OFFICIAL, "Event ${jdn.value}", holiday)
    }
}

internal class FakeSearchSource(
    private val results: Map<String, List<EventSearchResult>>,
) : EventSearchSource {
    val queries = mutableListOf<Pair<String, Int>>()

    override suspend fun search(
        text: String,
        limit: Int,
    ): List<EventSearchResult> {
        queries += text to limit
        return results[text].orEmpty()
    }
}
