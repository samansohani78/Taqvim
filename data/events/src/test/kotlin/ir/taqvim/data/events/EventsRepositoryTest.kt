/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events

import app.cash.turbine.test
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.HijriDateSource
import ir.taqvim.core.calendar.HijriOffset
import ir.taqvim.core.calendar.IranCrescentCalendar
import ir.taqvim.core.calendar.IranIslamicCalendar
import ir.taqvim.core.calendar.IslamicMonthOverrides
import ir.taqvim.core.calendar.IslamicMonthTable
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.UmmAlQuraCalendar
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.events.EventPreferences
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.ics.Frequency
import ir.taqvim.core.ics.RecurrenceRule
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.core.model.Weekday
import ir.taqvim.data.database.PersonalEventEntity
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/** T-305 (U): dataset, personal and iCalendar events per day, and re-emission on preference changes. */
class EventsRepositoryTest {
    private val tehran = TimeZone.of("Asia/Tehran")
    private val nowruz = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, 1405, 1, 1))
    private val farFromTable = LocalDate(2030, 6, 1).toJdn()
    private val now = Instant.parse("2026-03-01T00:00:00Z")
    private val clock =
        object : Clock {
            override fun now(): Instant = now
        }

    private val officialMonths: IslamicMonthTable =
        IslamicMonthOverrides.parse(IslamicMonthOverrides.bundledIranOfficialText().orEmpty()).getOrThrow().table

    private val settings = MutableStateFlow(settings())
    private val personal = MutableStateFlow<List<PersonalEventRecord>>(emptyList())
    private val repository =
        EventsRepository(
            settings = settings,
            inputs =
                EventInputs(personal = PersonalEventsSource { personal }),
            clock = clock,
            zones = flowOf(tehran),
            computeDispatcher = Dispatchers.Unconfined,
        )

    private fun settings(
        enabled: Set<EventSource> = setOf(EventSource.IRAN_OFFICIAL),
        variant: IslamicVariant = IslamicVariant.IRAN_OFFICIAL,
        offset: HijriOffset? = null,
        overrides: IslamicMonthTable? = officialMonths,
    ) = EventsSettings(
        preferences =
            EventPreferences(
                enabledSources = enabled,
                homeTimeZone = tehran,
                islamicVariant = variant,
                islamicOverrides = overrides,
            ),
        weekend = setOf(Weekday.FRIDAY),
        hijriOffset = offset,
    )

    private fun event(
        id: Long,
        start: Jdn,
        end: Jdn = start,
        system: CalendarSystem = CalendarSystem.PERSIAN,
        startMinute: Int? = null,
        endMinute: Int? = startMinute?.plus(60),
        zoneId: String = tehran.id,
    ) = PersonalEventEntity(
        id = id,
        title = "event $id",
        calendarSystem = system,
        startJdn = start.value,
        startMinute = startMinute,
        endMinute = endMinute,
        endJdn = end.value,
        timeZoneId = zoneId,
        createdAtEpochMillis = 0,
        updatedAtEpochMillis = 0,
    )

    @Test
    fun `official holidays, weekend and the official Hijri date of Nowruz 1405`(): Unit =
        runTest {
            val days = repository.days(nowruz - 1..nowruz).first()

            val (friday, newYear) = days
            friday.isWeekend shouldBe true
            newYear.isWeekend shouldBe false
            newYear.official.map { it.definition.id.value } shouldContain "ir.holiday.nowruz-1"
            newYear.isHoliday shouldBe true
            newYear.hijri?.source shouldBe HijriDateSource.OFFICIAL_TABLE
            newYear.islamicDate shouldBe IranIslamicCalendar(officialMonths).fromJdn(nowruz)
        }

    @Test
    fun `without the official override every Hijri date is computed`(): Unit =
        runTest {
            settings.value = settings(overrides = null)
            val newYear = repository.day(nowruz).first()

            newYear.hijri?.source shouldBe HijriDateSource.CRESCENT_ESTIMATE
            newYear.islamicDate shouldBe IranCrescentCalendar.fromJdn(nowruz)
        }

    @Test
    fun `a preference change re-emits with the new sources, identical results do not`(): Unit =
        runTest {
            repository.day(nowruz).test {
                awaitItem().isHoliday shouldBe true

                // No ancient Iranian festival (D-06) falls on 1 Farvardin, so nothing is shown.
                settings.value = settings(enabled = setOf(EventSource.ANCIENT_IRAN))
                val disabled = awaitItem()
                disabled.isHoliday shouldBe false
                disabled.official.shouldBeEmpty()

                // An input change on a day this flow does not show must not re-emit it.
                personal.value = listOf(PersonalEventRecord(event(9, nowruz + 7), recurrence = null))
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `the Hijri date follows official table, then user offset, then estimate`(): Unit =
        runTest {
            val offset = HijriOffset(1, now - 1.days)
            val estimate = IranIslamicCalendar(officialMonths)

            repository
                .day(farFromTable)
                .first()
                .hijri
                ?.source shouldBe HijriDateSource.CRESCENT_ESTIMATE

            settings.value = settings(offset = offset)
            val corrected = repository.day(farFromTable).first()
            corrected.hijri?.source shouldBe HijriDateSource.USER_OFFSET
            corrected.islamicDate shouldBe estimate.fromJdn(farFromTable + 1)
            repository
                .day(nowruz)
                .first()
                .hijri
                ?.source shouldBe HijriDateSource.OFFICIAL_TABLE

            settings.value = settings(variant = IslamicVariant.UMM_AL_QURA, offset = offset)
            val ummAlQura = repository.day(farFromTable).first()
            ummAlQura.hijri.shouldBeNull()
            ummAlQura.islamicDate shouldBe UmmAlQuraCalendar.fromJdn(farFromTable)
        }

    @Test
    fun `personal events cover their days and recurring events expand in their calendar`(): Unit =
        runTest {
            personal.value =
                listOf(
                    PersonalEventRecord(event(1, nowruz - 1, nowruz + 1), recurrence = null),
                    PersonalEventRecord(event(2, nowruz - 14, startMinute = 600), RecurrenceRule(Frequency.WEEKLY)),
                    PersonalEventRecord(nepali(3, nowruz), RecurrenceRule(Frequency.DAILY)),
                    PersonalEventRecord(nepali(4, nowruz + 7), recurrence = null),
                )

            val week = repository.days(nowruz..nowruz + 7).first().associate { it.jdn to it.personal }

            // Bikram Sambat events expand like any other (T-105): the daily one (3) is on every day.
            week.getValue(nowruz).map { it.eventId } shouldBe listOf(1L, 3L, 2L)
            week.getValue(nowruz).first().days shouldBe (nowruz - 1..nowruz + 1)
            week.getValue(nowruz + 1).map { it.eventId } shouldBe listOf(1L, 3L)
            week.getValue(nowruz + 2).map { it.eventId } shouldBe listOf(3L)
            // All-day occurrences come before timed ones on the same day.
            week.getValue(nowruz + 7).map { it.eventId to it.recurring } shouldBe
                listOf(3L to true, 4L to false, 2L to true)
        }

    @Test
    fun `timed personal events are dated in the display zone, all-day ones by their own dates`(): Unit =
        runTest {
            val ranges = mutableListOf<JdnRange>()
            val repository = repository(zone = tehran, personalRanges = ranges)
            personal.value =
                listOf(
                    // 00:30 in Tokyo is 21:00 of the previous day in Tehran.
                    PersonalEventRecord(event(1, nowruz, startMinute = 30, zoneId = "Asia/Tokyo"), recurrence = null),
                    // 23:00 in Los Angeles is 09:30 of the next day in Tehran.
                    PersonalEventRecord(
                        event(2, nowruz, startMinute = 23 * 60, zoneId = "America/Los_Angeles"),
                        recurrence = null,
                    ),
                    // A UTC event late in the day, as imported feeds store them (E2), also moves.
                    PersonalEventRecord(event(3, nowruz, startMinute = 22 * 60, zoneId = "UTC"), recurrence = null),
                    PersonalEventRecord(event(4, nowruz), recurrence = null),
                )

            val days = repository.personalIds(nowruz - 1..nowruz + 1)

            days.getValue(nowruz - 1) shouldBe listOf(1L)
            days.getValue(nowruz) shouldBe listOf(4L)
            // 22:00 UTC and 23:00 in Los Angeles both fall on the next Tehran day.
            days.getValue(nowruz + 1) shouldBe listOf(3L, 2L)
            // Sources are read one day wider, so conversions into the shown range are not missed.
            ranges.first() shouldBe (nowruz - 2..nowruz + 2)
        }

    @Test
    fun `a device zone change re-dates timed events without re-reading the settings`(): Unit =
        runTest {
            val zones = MutableStateFlow(tehran)
            var settingsReads = 0
            val repository =
                EventsRepository(
                    settings = settings.onStart { settingsReads++ },
                    inputs =
                        EventInputs(
                            personal = PersonalEventsSource { personal },
                        ),
                    clock = clock,
                    zones = zones,
                    computeDispatcher = Dispatchers.Unconfined,
                )
            // 00:30 in Tokyo: the previous day in Tehran, the same day in Tokyo.
            personal.value =
                listOf(
                    PersonalEventRecord(event(1, nowruz, startMinute = 30, zoneId = "Asia/Tokyo"), recurrence = null),
                )

            repository.days(nowruz - 1..nowruz).test {
                awaitItem().map { day -> day.personal.map { it.eventId } } shouldBe listOf(listOf(1L), emptyList())
                zones.value = TimeZone.of("Asia/Tokyo")
                awaitItem().map { day -> day.personal.map { it.eventId } } shouldBe listOf(emptyList(), listOf(1L))
                // The same zone again changes nothing.
                zones.value = TimeZone.of("Asia/Tokyo")
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
            // The settings stream stays subscribed across the zone change.
            settingsReads shouldBe 1
        }

    @Test
    fun `a timed event spanning midnight in the display zone covers both days`(): Unit =
        runTest {
            val repository = repository(zone = tehran)
            personal.value =
                listOf(
                    PersonalEventRecord(
                        // 23:00 on the first day to 00:30 on the next, as the editor stores a span across midnight.
                        event(1, nowruz, nowruz + 1, startMinute = 23 * 60, endMinute = 30, zoneId = tehran.id),
                        recurrence = null,
                    ),
                )

            val days = repository.personalIds(nowruz..nowruz + 1)

            days.getValue(nowruz) shouldBe listOf(1L)
            days.getValue(nowruz + 1) shouldBe listOf(1L)
        }

    /** The personal event ids shown on each day of [range]. */
    private suspend fun EventsRepository.personalIds(range: JdnRange): Map<Jdn, List<Long>> =
        days(range).first().associate { day -> day.jdn to day.personal.map { it.eventId } }

    private fun repository(
        zone: TimeZone,
        personalRanges: MutableList<JdnRange> = mutableListOf(),
    ) = EventsRepository(
        settings = settings,
        inputs =
            EventInputs(
                personal =
                    PersonalEventsSource { days ->
                        personalRanges += days
                        personal
                    },
            ),
        clock = clock,
        zones = flowOf(zone),
        computeDispatcher = Dispatchers.Unconfined,
    )

    private fun nepali(
        id: Long,
        start: Jdn,
    ) = event(id, start, system = CalendarSystem.NEPALI)

    @Test
    fun `an empty day range is rejected`() {
        shouldThrow<IllegalArgumentException> { repository.days(JdnRange(Jdn(10), Jdn(5))) }
    }
}
