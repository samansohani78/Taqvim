/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.events.Occurrence
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.praytimes.PrayerSettings
import ir.taqvim.data.devicecalendar.DeviceEvent
import ir.taqvim.data.events.DayEvents
import ir.taqvim.data.events.IcsOccurrence
import ir.taqvim.data.events.PersonalOccurrence
import ir.taqvim.data.events.generated.OfficialEvents
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.feature.timeline.TimelineDay
import ir.taqvim.feature.timeline.TimelineEvent
import ir.taqvim.feature.timeline.TimelineEventKind
import ir.taqvim.feature.timeline.TimelineNow
import ir.taqvim.feature.timeline.TimelinePlace
import ir.taqvim.feature.timeline.TimelineSettings
import ir.taqvim.feature.times.TimesSettings
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/** T-900 wiring: day events become all-day items and minute spans split at midnight in the device zone. */
class TimelineAdaptersTest {
    private val utc = TimeZone.UTC
    private val tehran = TimeZone.of("Asia/Tehran")
    private val day = LocalDate(2026, 3, 21).toJdn()

    @Test
    fun `a span inside one day keeps its minutes`() {
        minuteSpanOn(day, at("2026-03-21T09:15:00Z"), at("2026-03-21T10:45:00Z"), utc) shouldBe (555 until 645)
    }

    @Test
    fun `an event crossing midnight is split between both days`() {
        val begin = at("2026-03-21T22:00:00Z")
        val end = at("2026-03-22T01:30:00Z")

        minuteSpanOn(day, begin, end, utc) shouldBe (1_320 until 1_440)
        minuteSpanOn(day + 1, begin, end, utc) shouldBe (0 until 90)
        minuteSpanOn(day + 2, begin, end, utc).shouldBeNull()
        minuteSpanOn(day - 1, begin, end, utc).shouldBeNull()
    }

    @Test
    fun `an event ending at midnight stays on its day and a zero-length event keeps a minute`() {
        val begin = at("2026-03-21T23:00:00Z")
        val midnight = at("2026-03-22T00:00:00Z")

        minuteSpanOn(day, begin, midnight, utc) shouldBe (1_380 until 1_440)
        minuteSpanOn(day + 1, begin, midnight, utc).shouldBeNull()
        minuteSpanOn(day, begin, begin, utc) shouldBe (1_380 until 1_381)
        minuteSpanOn(day, at("2026-03-21T23:59:30Z"), midnight, utc) shouldBe (1_439 until 1_440)
    }

    @Test
    fun `spans follow the device zone`() {
        // 20:00–21:00 UTC is 23:30–00:30 in Tehran (UTC+03:30).
        val begin = at("2026-03-21T20:00:00Z")
        val end = at("2026-03-21T21:00:00Z")

        minuteSpanOn(day, begin, end, tehran) shouldBe (1_410 until 1_440)
        minuteSpanOn(day + 1, begin, end, tehran) shouldBe (0 until 30)
    }

    @Test
    fun `day events keep their order with dataset events all day`() {
        val nowruz = OfficialEvents.ALL.first { it.id.value == "ir.holiday.nowruz-1" }
        val trip =
            DeviceEvent(
                3,
                1,
                "Trip",
                at("2026-03-20T00:00:00Z"),
                at("2026-03-23T00:00:00Z"),
                true,
                null,
                day..day,
            )
        val talk =
            IcsOccurrence(
                2,
                "u",
                "Talk",
                "",
                at("2026-03-21T06:00:00Z"),
                at("2026-03-21T07:00:00Z"),
                false,
                day..day,
            )
        val events =
            dayEvents(
                official =
                    listOf(
                        Occurrence(nowruz, day, CalendarDate(CalendarSystem.PERSIAN, 1405, 1, 1), true, 1405),
                    ),
                personal = listOf(personal(7, null, null), personal(8, 600, 690)),
                device = listOf(trip),
                ics = listOf(talk),
            )

        events.toTimelineDay("en", tehran).events shouldBe
            listOf(
                TimelineEvent(nowruz.id.value, TimelineEventKind.OFFICIAL, nowruz.title.forLanguage("en"), true, true),
                TimelineEvent("7", TimelineEventKind.PERSONAL, "Birthday", false, true),
                TimelineEvent("8", TimelineEventKind.PERSONAL, "Birthday", false, false, 600, 690),
                TimelineEvent("3", TimelineEventKind.DEVICE, "Trip", false, true),
                TimelineEvent("2:u", TimelineEventKind.SUBSCRIPTION, "Talk", false, false, 570, 630),
            )
    }

    @Test
    fun `personal times are moved from the event's zone to the device zone`() {
        val events = dayEvents(personal = listOf(personal(9, 600, 660)))

        // 10:00–11:00 in Tehran is 06:30–07:30 UTC.
        events.toTimelineDay("en", utc).events.single() shouldBe
            TimelineEvent("9", TimelineEventKind.PERSONAL, "Birthday", false, false, 390, 450)
    }

    @Test
    fun `settings, place and days follow their sources`(): Unit =
        runTest {
            val preferences = UserPreferences.defaultsFor("fa")
            PreferencesTimelineSettingsSource(repositoryOf(preferences)).settings().first() shouldBe
                TimelineSettings(preferences.calendars, preferences.weekStart, preferences.islamicVariant, "fa")

            val times = MutableStateFlow<TimesSettings?>(null)
            val place = TimesTimelinePlaceSource { times }
            place.place().first().shouldBeNull()
            val language = requireNotNull(LanguageTable.forCode("fa"))
            val settings = TimesSettings("Tehran", Coordinates(35.69, 51.42), tehran, PrayerSettings(), language)
            times.value = settings
            place.place().first() shouldBe TimelinePlace(settings.place, tehran, settings.prayer)

            val holiday = dayEvents(isHoliday = true)
            val source = RepositoryTimelineDaysSource({ flowOf(listOf(holiday)) }, flowOf("fa"), flowOf(tehran))
            source.days(day..day).first() shouldBe listOf(TimelineDay(day, true, false, emptyList()))
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `the clock emits the device-zone minute and again at the next minute`(): Unit =
        runTest {
            val start = at("2026-03-21T20:29:40Z").toEpochMilliseconds()
            val clock =
                object : Clock {
                    override fun now(): Instant = Instant.fromEpochMilliseconds(start + currentTime)
                }

            val values = DeviceTimelineClockSource(clock, flowOf(tehran)).now().take(2).toList()

            // 20:29:40 UTC is 23:59:40 in Tehran; twenty seconds later the next day begins.
            values shouldBe listOf(TimelineNow(day, 1_439), TimelineNow(day + 1, 0))
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `the clock follows a device zone change at once`(): Unit =
        runTest {
            val clock =
                object : Clock {
                    override fun now(): Instant = at("2026-03-21T20:29:40Z")
                }
            val zones = MutableStateFlow(tehran)

            val values = async { DeviceTimelineClockSource(clock, zones).now().take(2).toList() }
            runCurrent()
            zones.value = TimeZone.of("Asia/Tokyo")

            // 20:29 UTC is 05:29 the next morning in Tokyo; it shows without waiting for the next minute.
            values.await() shouldBe listOf(TimelineNow(day, 1_439), TimelineNow(day + 1, 5 * 60 + 29))
            currentTime shouldBe 0L
        }

    private fun at(text: String): Instant = Instant.parse(text)

    private fun dayEvents(
        official: List<Occurrence> = emptyList(),
        isHoliday: Boolean = false,
        personal: List<PersonalOccurrence> = emptyList(),
        device: List<DeviceEvent> = emptyList(),
        ics: List<IcsOccurrence> = emptyList(),
    ): DayEvents =
        DayEvents(
            jdn = day,
            islamicDate = CalendarDate(CalendarSystem.ISLAMIC, 1447, 10, 1),
            hijri = null,
            official = official,
            isHoliday = isHoliday,
            isWeekend = false,
            personal = personal,
            device = device,
            ics = ics,
        )

    private fun personal(
        id: Long,
        start: Int?,
        end: Int?,
    ): PersonalOccurrence =
        PersonalOccurrence(
            eventId = id,
            title = "Birthday",
            notes = "",
            calendarSystem = CalendarSystem.PERSIAN,
            days = day..day,
            startMinute = start,
            endMinute = end,
            timeZoneId = "Asia/Tehran",
            colorArgb = null,
            recurring = false,
        )
}
