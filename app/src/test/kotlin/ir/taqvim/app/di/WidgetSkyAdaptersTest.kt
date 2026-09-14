/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.events.EventCategory
import ir.taqvim.core.events.EventDefinition
import ir.taqvim.core.events.EventId
import ir.taqvim.core.events.EventRule
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.events.LocalizedText
import ir.taqvim.core.events.Occurrence
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.testing.FakeClock
import ir.taqvim.data.events.DayEvents
import ir.taqvim.data.preferences.ChosenPlace
import ir.taqvim.data.preferences.PlaceSource
import ir.taqvim.data.preferences.StoredCountdown
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.feature.map.Equirectangular
import ir.taqvim.feature.map.WorldOutline
import ir.taqvim.feature.map.WorldOutlineSource
import ir.taqvim.feature.widgets.CountdownMode
import ir.taqvim.feature.widgets.CountdownUnit
import ir.taqvim.feature.widgets.WidgetConfig
import ir.taqvim.feature.widgets.WidgetContentBuilder
import ir.taqvim.feature.widgets.WidgetCountdown
import ir.taqvim.feature.widgets.WidgetKind
import ir.taqvim.feature.widgets.WidgetView
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/** T-1209 polar days, T-1210 Moon, T-1211 map and T-1212 countdown content from the preferences. */
class WidgetSkyAdaptersTest {
    private val persian = UserPreferences.defaultsFor("fa")
    private val berlin = TimeZone.of("Europe/Berlin")
    private val tehran = ChosenPlace(PlaceSource.CITY, 1, "Tehran", Coordinates(35.69, 51.42), "Asia/Tehran")
    private val sydney = ChosenPlace(PlaceSource.CITY, 2, "Sydney", Coordinates(-33.87, 151.21), "Australia/Sydney")
    private val longyearbyen =
        ChosenPlace(PlaceSource.CITY, 3, "Longyearbyen", Coordinates(78.22, 15.65), "Arctic/Longyearbyen")

    private fun events(range: JdnRange): Flow<List<DayEvents>> = flowOf(range.map { emptyDay(it) })

    private fun emptyDay(jdn: Jdn) =
        DayEvents(
            jdn = jdn,
            islamicDate = CalendarDate(CalendarSystem.ISLAMIC, 1448, 3, 1),
            hijri = null,
            official = emptyList(),
            isHoliday = false,
            isWeekend = false,
            personal = emptyList(),
            device = emptyList(),
            ics = emptyList(),
        )

    /** Counts outline loads; a `null` outline cannot be read. */
    private class CountingOutline(
        private val outline: WorldOutline?,
    ) : WorldOutlineSource {
        var loads = 0

        override suspend fun load(): WorldOutline {
            loads++
            return checkNotNull(outline) { "outline asset unreadable" }
        }
    }

    private fun source(
        place: ChosenPlace?,
        outline: WorldOutlineSource = CountingOutline(null),
    ) = PreferencesWidgetDataSource(
        repositoryOf(persian.copy(place = place)),
        ::events,
        { it.name },
        WidgetSkyParts(outline),
    ) { berlin }

    @Test
    fun `a polar day at the chosen place is not reported as a missing place`(): Unit =
        runTest {
            val midsummer = Instant.parse("2026-06-21T10:00:00Z")

            val polar = source(longyearbyen).load(WidgetKind.SUN_ARC, WidgetConfig(), midsummer, WidgetView())
            polar.sun.shouldBeNull()
            polar.daylightUnavailable shouldBe true

            val ordinary = source(tehran).load(WidgetKind.SUN_ARC, WidgetConfig(), midsummer, WidgetView())
            ordinary.sun.shouldNotBeNull()
            ordinary.daylightUnavailable shouldBe false

            val nowhere = source(null).load(WidgetKind.SUN_ARC, WidgetConfig(), midsummer, WidgetView())
            nowhere.sun.shouldBeNull()
            nowhere.daylightUnavailable shouldBe false
        }

    @Test
    fun `the Moon is dated in the user's calendar and digits and mirrored in the south`(): Unit =
        runTest {
            val at = Instant.parse("2026-09-13T12:00:00Z")

            val moon = source(tehran).load(WidgetKind.MOON, WidgetConfig(), at, WidgetView()).moon.shouldNotBeNull()
            moon.rotationDegrees shouldBe 0f
            moon.illumination.all { it in '۰'..'۹' }.shouldBeTrue()
            moon.nextFullMoon.shouldNotBeNull()
            source(sydney).load(WidgetKind.MOON, WidgetConfig(), at, WidgetView()).moon?.rotationDegrees shouldBe 180f
            source(tehran).load(WidgetKind.DATE_1X1, WidgetConfig(), at, WidgetView()).moon.shouldBeNull()
        }

    @Test
    fun `the map shades night, marks the place and loads the outline once`(): Unit =
        runTest {
            val ring = floatArrayOf(0.1f, 0.1f, 0.3f, 0.1f, 0.3f, 0.3f)
            val outline = CountingOutline(WorldOutline(listOf(ring, floatArrayOf(0.5f, 0.5f)), emptyList()))
            val widgets = source(tehran, outline)
            // Noon UTC: day at the prime meridian, night at the date line.
            val noon = Instant.parse("2026-09-13T12:00:00Z")

            val map = widgets.load(WidgetKind.MAP, WidgetConfig(), noon, WidgetView()).map.shouldNotBeNull()
            map.land.size shouldBe 1
            val shade = map.shade.shouldNotBeNull()
            shade.columns shouldBe 72
            shade.rows shouldBe 36
            shade.darkness.all { it in 0f..1f }.shouldBeTrue()
            shade.darkness[18 * 72 + 36] shouldBe 0f
            shade.darkness[18 * 72] shouldBe 1f
            val expected = Equirectangular.project(tehran.coordinates)
            val marker = map.marker.shouldNotBeNull()
            marker.x shouldBe (expected.x.toFloat() plusOrMinus 0.0001f)
            marker.y shouldBe (expected.y.toFloat() plusOrMinus 0.0001f)

            widgets.load(WidgetKind.MAP, WidgetConfig(), noon, WidgetView())
            outline.loads shouldBe 1

            val unreadable = CountingOutline(null)
            val withoutOutline = source(null, unreadable)
            val bare = withoutOutline.load(WidgetKind.MAP, WidgetConfig(), noon, WidgetView()).map.shouldNotBeNull()
            bare.land.shouldBeEmpty()
            bare.marker.shouldBeNull()
            withoutOutline.load(WidgetKind.MAP, WidgetConfig(), noon, WidgetView())
            unreadable.loads shouldBe 2
        }

    @Test
    fun `countdowns count in their own calendar with the user's digits`(): Unit =
        runTest {
            val trip = WidgetCountdown(CalendarSystem.GREGORIAN, 2026, 9, 30, startJdn = 0, title = "Trip")
            val at = Instant.parse("2026-09-13T08:00:00Z")

            val view =
                source(tehran)
                    .load(WidgetKind.COUNTDOWN, WidgetConfig(countdown = trip), at, WidgetView())
                    .countdown
                    .shouldNotBeNull()
            view.headline.unit shouldBe CountdownUnit.DAYS
            view.headline.text shouldBe "۱۷"
            view.date shouldBe LocalDate(2026, 9, 30)
            source(tehran).load(WidgetKind.COUNTDOWN, WidgetConfig(), at, WidgetView()).countdown.shouldBeNull()
        }

    @Test
    fun `countdown options offer the computable calendars and upcoming dataset events`(): Unit =
        runTest {
            val nowruz = definition("ir.nowruz-1", EventRule.Fixed(1, 1), "نوروز", "Nowruz")
            val secondSunday = EventRule.NthWeekdayOfMonth(5, Weekday.SUNDAY, 2)
            val mothers = definition("int.mothers-day", secondSunday, "روز مادر", null)
            val today = LocalDate(2026, 9, 13).toJdn()
            // Both events inside the 120 days the source looks ahead; Nowruz repeats and is offered once.
            val laterDay = today + 30
            val days = { range: JdnRange ->
                flowOf(
                    range.map { jdn ->
                        val official =
                            when (jdn) {
                                laterDay -> listOf(occurrence(nowruz, jdn), occurrence(mothers, jdn))
                                today + 1 -> listOf(occurrence(nowruz, jdn))
                                else -> emptyList()
                            }
                        emptyDay(jdn).copy(official = official)
                    },
                )
            }
            val english = UserPreferences.defaultsFor("en").copy(place = tehran)
            val clock = FakeClock(Instant.parse("2026-09-13T08:00:00Z"))

            val options = PreferencesWidgetCountdownSource(repositoryOf(english), days, clock) { berlin }.options()
            options.today shouldBe today
            options.language.code shouldBe "en"
            options.calendars.map { it.system } shouldContain CalendarSystem.PERSIAN
            options.calendars.map { it.system }.distinct() shouldBe options.calendars.map { it.system }
            options.occasions.map { it.title } shouldBe listOf("Nowruz", "روز مادر")
            val first = options.occasions.first()
            first.repeatsYearly shouldBe true
            first.dateText shouldBe WidgetContentBuilder.dayTitle(PersianCalendarSystem, today + 1, options.language)
            options.occasions.last().repeatsYearly shouldBe false
        }

    private fun definition(
        id: String,
        rule: EventRule,
        persianTitle: String,
        englishTitle: String?,
    ) = EventDefinition(
        id = EventId(id),
        calendar = CalendarSystem.PERSIAN,
        source = EventSource.IRAN_OFFICIAL,
        category = EventCategory.NATIONAL,
        isHoliday = false,
        title = LocalizedText(listOfNotNull("fa" to persianTitle, englishTitle?.let { "en" to it }).toMap()),
        rule = rule,
    )

    private fun occurrence(
        definition: EventDefinition,
        jdn: Jdn,
    ): Occurrence {
        val date = PersianCalendarSystem.fromJdn(jdn)
        return Occurrence(definition, jdn, date, isHoliday = false, year = date.year)
    }

    @Test
    fun `stored countdowns map to widget countdowns and back`() {
        val countdown =
            WidgetCountdown(CalendarSystem.PERSIAN, 1403, 12, 30, startJdn = 7, CountdownMode.SINCE, true, "Nowruz eve")
        val config = WidgetConfig(countdown = countdown)

        config.toStored().toWidgetConfig() shouldBe config
        config.toStored().countdown shouldBe
            StoredCountdown("Nowruz eve", CalendarSystem.PERSIAN, 1403, 12, 30, "SINCE", true, 7)
        StoredCountdown("", CalendarSystem.GREGORIAN, 2026, 1, 1, "RETIRED", false, 0).toWidgetCountdown().mode shouldBe
            CountdownMode.UNTIL
    }
}
