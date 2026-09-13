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
import ir.taqvim.core.calendar.IranIslamicCalendar
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
import ir.taqvim.data.database.IcsEventCacheEntity
import ir.taqvim.data.database.PersonalEventEntity
import ir.taqvim.data.devicecalendar.DeviceEvent
import ir.taqvim.data.devicecalendar.DeviceEventMapping
import ir.taqvim.data.devicecalendar.InstantWindow
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/** T-305 (U): dataset, personal, device and iCalendar events per day, and re-emission on preference changes. */
class EventsRepositoryTest {
    private val tehran = TimeZone.of("Asia/Tehran")
    private val nowruz = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, 1405, 1, 1))
    private val farFromTable = LocalDate(2030, 6, 1).toJdn()
    private val now = Instant.parse("2026-03-01T00:00:00Z")
    private val clock =
        object : Clock {
            override fun now(): Instant = now
        }

    private val settings = MutableStateFlow(settings())
    private val personal = MutableStateFlow<List<PersonalEventRecord>>(emptyList())
    private val device = MutableStateFlow<List<DeviceEvent>>(emptyList())
    private val ics = MutableStateFlow<List<IcsEventCacheEntity>>(emptyList())
    private val icsWindows = mutableListOf<InstantWindow>()
    private val repository =
        EventsRepository(
            settings = settings,
            inputs =
                EventInputs(
                    personal = PersonalEventsSource { personal },
                    device = DeviceEventsSource { device },
                    ics =
                        IcsEventsSource { window ->
                            icsWindows += window
                            ics
                        },
                ),
            clock = clock,
            zone = { tehran },
            computeDispatcher = Dispatchers.Unconfined,
        )

    private fun settings(
        enabled: Set<EventSource> = setOf(EventSource.IRAN_OFFICIAL),
        variant: IslamicVariant = IslamicVariant.IRAN_OFFICIAL,
        offset: HijriOffset? = null,
    ) = EventsSettings(
        preferences = EventPreferences(enabledSources = enabled, homeTimeZone = tehran, islamicVariant = variant),
        weekend = setOf(Weekday.FRIDAY),
        hijriOffset = offset,
    )

    private fun event(
        id: Long,
        start: Jdn,
        end: Jdn = start,
        system: CalendarSystem = CalendarSystem.PERSIAN,
        startMinute: Int? = null,
    ) = PersonalEventEntity(
        id = id,
        title = "event $id",
        calendarSystem = system,
        startJdn = start.value,
        startMinute = startMinute,
        endJdn = end.value,
        timeZoneId = tehran.id,
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
            newYear.islamicDate shouldBe IranIslamicCalendar().fromJdn(nowruz)
        }

    @Test
    fun `a preference change re-emits with the new sources, identical results do not`(): Unit =
        runTest {
            repository.day(nowruz).test {
                awaitItem().isHoliday shouldBe true

                // The dataset has no ancient Iranian festivals yet (D-06), so nothing is shown.
                settings.value = settings(enabled = setOf(EventSource.ANCIENT_IRAN))
                val disabled = awaitItem()
                disabled.isHoliday shouldBe false
                disabled.official.shouldBeEmpty()

                device.value = listOf(deviceEvent(nowruz + 3))
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `the Hijri date follows official table, then user offset, then estimate`(): Unit =
        runTest {
            val offset = HijriOffset(1, now - 1.days)
            val estimate = IranIslamicCalendar()

            repository
                .day(farFromTable)
                .first()
                .hijri
                ?.source shouldBe HijriDateSource.TABULAR_ESTIMATE

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

            week.getValue(nowruz).map { it.eventId } shouldBe listOf(1L, 2L)
            week.getValue(nowruz).first().days shouldBe (nowruz - 1..nowruz + 1)
            week.getValue(nowruz + 1).map { it.eventId } shouldBe listOf(1L)
            week.getValue(nowruz + 2).shouldBeEmpty()
            // All-day occurrences come before timed ones on the same day.
            week.getValue(nowruz + 7).map { it.eventId to it.recurring } shouldBe listOf(4L to false, 2L to true)
        }

    private fun nepali(
        id: Long,
        start: Jdn,
    ) = event(id, start, system = CalendarSystem.NEPALI)

    @Test
    fun `device and iCalendar events are dated in the collection zone`(): Unit =
        runTest {
            val range = nowruz - 1..nowruz
            device.value = listOf(deviceEvent(nowruz))
            ics.value =
                listOf(
                    icsEvent("all-day", "2026-03-21T00:00:00Z", "2026-03-22T00:00:00Z", allDay = true),
                    icsEvent("late", "2026-03-20T21:00:00Z", "2026-03-20T21:30:00Z", allDay = false),
                    icsEvent("eve", "2026-03-20T12:00:00Z", "2026-03-20T13:00:00Z", allDay = false),
                )

            val (eve, newYear) = repository.days(range).first()

            newYear.device.map { it.eventId } shouldBe listOf(7L)
            newYear.ics.map { it.uid } shouldBe listOf("all-day", "late")
            eve.ics.map { it.uid } shouldBe listOf("eve")
            eve.device.shouldBeEmpty()
            icsWindows shouldBe listOf(DeviceEventMapping.window(range, tehran))
        }

    @Test
    fun `an empty day range is rejected`() {
        shouldThrow<IllegalArgumentException> { repository.days(JdnRange(Jdn(10), Jdn(5))) }
    }

    private fun deviceEvent(day: Jdn) =
        DeviceEvent(
            eventId = 7,
            calendarId = 1,
            title = "device",
            begin = now,
            end = now,
            allDay = true,
            colorArgb = null,
            days = day..day,
        )

    private fun icsEvent(
        uid: String,
        start: String,
        end: String,
        allDay: Boolean,
    ) = IcsEventCacheEntity(
        subscriptionId = 1,
        uid = uid,
        startEpochMillis = Instant.parse(start).toEpochMilliseconds(),
        endEpochMillis = Instant.parse(end).toEpochMilliseconds(),
        allDay = allDay,
        summary = uid,
    )
}
