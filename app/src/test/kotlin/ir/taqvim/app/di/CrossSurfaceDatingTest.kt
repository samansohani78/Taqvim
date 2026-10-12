/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.workdays.HalfDayPolicy
import ir.taqvim.core.workdays.WorkdayProfile
import ir.taqvim.data.events.EventInputs
import ir.taqvim.data.events.EventsRepository
import ir.taqvim.data.events.OfficialEventView
import ir.taqvim.data.events.toEventsSettings
import ir.taqvim.data.preferences.IslamicOverrideOrigin
import ir.taqvim.data.preferences.IslamicOverrideSetting
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.data.preferences.toProto
import kotlin.time.Clock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/**
 * The calendar screen, search and the workday calculator put the same event on the same day — proved through the
 * adapters the app binds, not through the shared object they are all supposed to use (L6).
 *
 * There were already assertions that `OfficialEventView` builds the right calendars, and they passed the entire
 * time search and the workday calculator were using `CalendarProvider.DEFAULT` instead of it: a test that builds
 * the right thing itself cannot notice that production builds the wrong one. These go through
 * [RepositoryCalendarDaySource], [OfficialEventSearchSource] and [PreferencesToolsSettingsSource], so putting any
 * one of them back on the default calendars fails them.
 */
class CrossSurfaceDatingTest {
    private val kabul = TimeZone.of("Asia/Kabul")

    /** Eid al-Fitr as Afghanistan announces it: 1 Shawwal in the tabular type II calendar (ADR-0010). */
    private val afghanEid = "af.holiday.eid-al-fitr.1"

    /** Eid al-Fitr as Iran announces it: 1 Shawwal, but in the calculation the user chose (ADR-0037). */
    private val iranEid = "ir.holiday.eid-al-fitr"

    private val base =
        UserPreferences.defaultsFor("fa").let { preferences ->
            preferences.copy(
                app =
                    preferences.app.copy(
                        enabledEventSources = setOf(EventSource.AFGHANISTAN_OFFICIAL, EventSource.IRAN_OFFICIAL),
                    ),
            )
        }

    private val startOfYear = persian(YEAR, 1, 1)
    private val endOfYear = persian(YEAR + 1, 1, 1) - 1

    private fun persian(
        year: Int,
        month: Int,
        day: Int,
    ): Jdn = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, year, month, day))

    private fun repository(preferences: UserPreferences) =
        EventsRepository(
            settings = flowOf(preferences.toEventsSettings(homeTimeZone = kabul)),
            inputs =
                EventInputs(
                    personal = { flowOf(emptyList()) },
                    ics = { flowOf(emptyList()) },
                ),
            clock = Clock.System,
            zones = flowOf(kabul),
        )

    /** The day the calendar screen puts [eventId] on, through the adapter the calendar screen is bound to. */
    private suspend fun calendarDay(
        preferences: UserPreferences,
        eventId: String,
        range: ir.taqvim.core.model.JdnRange = startOfYear..endOfYear,
    ): Jdn? =
        RepositoryCalendarDaySource(repository(preferences), flowOf("fa"))
            .days(range)
            .first()
            .firstOrNull { day -> day.events.any { it.id == eventId } }
            ?.jdn

    private suspend fun titleOf(eventId: String): String =
        RepositoryCalendarDaySource(repository(base), flowOf("fa"))
            .days(startOfYear..endOfYear)
            .first()
            .flatMap { it.events }
            .first { it.id == eventId }
            .title

    /** The day search dates [eventId] to, through the adapter the search screen is bound to. */
    private suspend fun searchDay(
        preferences: UserPreferences,
        title: String,
        eventId: String,
        from: Jdn = startOfYear,
    ): Jdn? =
        OfficialEventSearchSource(
            language = { "fa" },
            today = { from },
            settings = { preferences.toEventsSettings(homeTimeZone = kabul) },
            zone = { kabul },
        ).search(title, limit = SEARCH_LIMIT, from = from)
            .firstOrNull { it.eventId == eventId }
            ?.nextDay

    /**
     * The workday adapter, counting only [sources] as holidays and keeping no weekend.
     *
     * Both narrowings matter. With a weekend, a holiday that happens to fall on a Friday is "off" whatever the
     * calculator thinks of the event. With both countries switched on, Afghanistan's Eid sits a day either side of
     * Iran's and covers for it. Either one lets the assertion pass against a calculator reading the wrong calendar.
     */
    private fun toolsSource(
        repository: UserPreferencesRepository,
        sources: Set<EventSource>,
    ) = PreferencesToolsSettingsSource(
        preferences = repository,
        workdayProfile =
            flowOf(
                WorkdayProfile(
                    weekend = emptySet(),
                    holidaySources = sources,
                    halfDays = HalfDayPolicy.FULL_WORKDAY,
                ),
            ),
        zones = flowOf(kabul),
    )

    /** Whether the workday calculator counts [day] as off, through the adapter the tools screens are bound to. */
    private suspend fun workdayIsOff(
        preferences: UserPreferences,
        day: Jdn,
        sources: Set<EventSource>,
    ): Boolean {
        val workdays =
            toolsSource(repositoryOf(preferences), sources)
                .settings()
                .first()
                .workdays
                .shouldNotBeNull()
        return !workdays.isWorkday(day)
    }

    @Test
    fun `an Afghan official holiday falls on one day for the calendar, search and the workday calculator`(): Unit =
        runTest {
            val onCalendar = calendarDay(base, afghanEid).shouldNotBeNull()

            val inSearch = searchDay(base, titleOf(afghanEid), afghanEid).shouldNotBeNull()

            inSearch shouldBe onCalendar
            workdayIsOff(base, onCalendar, setOf(EventSource.AFGHANISTAN_OFFICIAL)) shouldBe true
        }

    @Test
    fun `Afghanistan keeps its own calendar while Iran follows the user's, so the two can disagree`(): Unit =
        runTest {
            // If both moved together, the test above would pass with every surface on the default calendars and
            // would be proving nothing. Afghan dates are announced in the tabular calendar whatever the user picks.
            val afghan = calendarDay(base, afghanEid).shouldNotBeNull()
            val iranian = eidWithAPlainDayBefore()

            val shifted = base.withShawwalMovedOneDayEarlier(iranian)
            val afghanAfter = calendarDay(shifted, afghanEid, around(afghan)).shouldNotBeNull()
            val iranianAfter = calendarDay(shifted, iranEid, around(iranian)).shouldNotBeNull()

            afghanAfter shouldBe afghan
            iranianAfter shouldNotBe iranian
        }

    @Test
    fun `an official month override moves the day on every surface at once`(): Unit =
        runTest {
            val before = eidWithAPlainDayBefore()
            val shifted = base.withShawwalMovedOneDayEarlier(before)

            val afterCalendar = calendarDay(shifted, iranEid, around(before)).shouldNotBeNull()
            val afterSearch =
                searchDay(shifted, titleOf(iranEid), iranEid, from = before - MARGIN).shouldNotBeNull()

            afterCalendar shouldBe before - 1
            afterSearch shouldBe afterCalendar
            workdayIsOff(shifted, afterCalendar, setOf(EventSource.IRAN_OFFICIAL)) shouldBe true
            // Which days are off around the Eid changes, so the override reached the workday calculator rather
            // than the calculator happening to agree. Comparing the whole window avoids having to know which of
            // the Eid's own days landed where.
            offDays(shifted, around(before)) shouldNotBe offDays(base, around(before))
        }

    @Test
    fun `a live adapter does not keep the view it built for the previous settings`(): Unit =
        runTest {
            // The workday adapter caches one view for the settings it was built from. It used to cache a lookup
            // built from the default calendars for the life of the process, so changing the Islamic calculation
            // moved the calendar screen and nothing else.
            val day = eidWithAPlainDayBefore()
            val store = InMemoryUserPrefs(base.toProto())
            val source = toolsSource(UserPreferencesRepository(store), setOf(EventSource.IRAN_OFFICIAL))

            val first =
                source
                    .settings()
                    .first()
                    .workdays
                    .shouldNotBeNull()
            first.isWorkday(day) shouldBe false

            store.updateData { base.withShawwalMovedOneDayEarlier(day).toProto() }
            val second =
                source
                    .settings()
                    .first()
                    .workdays
                    .shouldNotBeNull()

            // The same adapter instance, asked again after the settings changed, answers with the new dates.
            second.isWorkday(day - 1) shouldBe false
            (first === second) shouldBe false
        }

    /**
     * These preferences with Shawwal declared to have begun one day before [eid], the computed 1 Shawwal.
     *
     * The citation is deliberately an unusable placeholder: this is a fixture that proves a setting propagates, not
     * a claim about any announced date. Real overrides carry their primary source (ADR-0037).
     */
    private fun UserPreferences.withShawwalMovedOneDayEarlier(eid: Jdn): UserPreferences {
        // The Hijri year is read off the very calendar the user is on rather than assumed, so the table lands on
        // the month the event is actually in.
        val islamic =
            OfficialEventView(toEventsSettings(homeTimeZone = kabul))
                .calendars
                .providerFor(EventSource.IRAN_OFFICIAL)
                .calendarFor(CalendarSystem.ISLAMIC)
        val onEid = checkNotNull(islamic).fromJdn(eid)
        check(onEid.month == SHAWWAL && onEid.day == 1) { "expected 1 Shawwal, got $onEid" }
        val shawwalStart = eid - 1
        val json =
            """
            {
              "schemaVersion": 1,
              "months": [
                ${month(onEid.year, SHAWWAL, shawwalStart)},
                ${month(onEid.year, SHAWWAL + 1, shawwalStart + LONG_MONTH)}
              ]
            }
            """.trimIndent()
        return copy(islamicOverride = IslamicOverrideSetting(IslamicOverrideOrigin.IMPORTED, json))
    }

    private fun month(
        hijriYear: Int,
        hijriMonth: Int,
        start: Jdn,
    ): String {
        val date = PersianCalendarSystem.fromJdn(start)
        return """
            {
              "hijriYear": $hijriYear,
              "hijriMonth": $hijriMonth,
              "persianStart": { "year": ${date.year}, "month": ${date.month}, "day": ${date.day} },
              "citation": {
                "url": "https://example.invalid/test-fixture",
                "title": "Test fixture, not an announced date",
                "page": "-",
                "retrieved": "2026-10-10"
              }
            }
            """.trimIndent()
    }

    /**
     * An Eid al-Fitr with an ordinary workday before it.
     *
     * The first one in 1405 falls inside the Nowruz holidays, which are off whatever the lunar calendar says, so a
     * day moving onto or off it changes nothing a workday assertion can see. The lunar year drifts about eleven
     * days earlier each solar year, so a later one is clear; which one is found by asking rather than assumed.
     */
    private suspend fun eidWithAPlainDayBefore(): Jdn {
        (YEAR..YEAR + SEARCH_YEARS).forEach { year ->
            val day = calendarDay(base, iranEid, persian(year, 1, 1)..(persian(year + 1, 1, 1) - 1))
            if (day != null && !workdayIsOff(base, day - 1, setOf(EventSource.IRAN_OFFICIAL))) return day
        }
        error("no Eid al-Fitr in $YEAR..${YEAR + SEARCH_YEARS} has a plain workday before it")
    }

    /** The days of [range] the workday calculator counts as off, under Iran's calendar alone. */
    private suspend fun offDays(
        preferences: UserPreferences,
        range: ir.taqvim.core.model.JdnRange,
    ): Set<Jdn> {
        val workdays =
            toolsSource(repositoryOf(preferences), setOf(EventSource.IRAN_OFFICIAL))
                .settings()
                .first()
                .workdays
                .shouldNotBeNull()
        return range.filterNot { workdays.isWorkday(it) }.toSet()
    }

    /** A window a few days either side of [day], so an event that moves by one does not fall off the edge. */
    private fun around(day: Jdn): ir.taqvim.core.model.JdnRange = (day - MARGIN)..(day + MARGIN)

    private companion object {
        const val MARGIN = 10
        const val SEARCH_YEARS = 8
        const val YEAR = 1405
        const val SEARCH_LIMIT = 50
        const val SHAWWAL = 10
        const val LONG_MONTH = 30
    }
}
