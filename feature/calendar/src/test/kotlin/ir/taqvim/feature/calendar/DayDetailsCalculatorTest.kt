/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.astronomy.ZodiacSign
import ir.taqvim.core.calendar.DatePeriod
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.PrayerMethod
import ir.taqvim.core.praytimes.PrayerSettings
import ir.taqvim.core.praytimes.PrayerTimesResult
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/**
 * T-802: the Calendars and Times tab contents, checked against their definitions and known dates (Nowruz 1405 on
 * Saturday 21 March 2026; the new moon of the 12 August 2026 and the full moon of the 28 August 2026 eclipses).
 */
class DayDetailsCalculatorTest {
    /** 1 Farvardin 1405. */
    private val nowruz = gregorian(2026, 3, 21)

    /** 27 Esfand 1404 (1404 is a common year, so Esfand has 29 days). */
    private val beforeNowruz = gregorian(2026, 3, 18)
    private val calendars = CalendarCalendars(PERSIAN_FIRST)

    @Test
    fun `Nowruz 1405 opens the week, the year and spring`() {
        val overview = DayDetailsCalculator.overview(nowruz, beforeNowruz, calendars, TEHRAN)

        overview.day shouldBe nowruz
        overview.daysFromToday shouldBe 3
        overview.period shouldBe DatePeriod(0, 0, 3)
        overview.dayOfWeek shouldBe 1
        overview.weekOfYear shouldBe 1
        overview.season shouldBe SeasonName.SPRING
        overview.dayOfSeason shouldBe 1
        // Farvardin, Ordibehesht and Khordad have 31 days each.
        overview.seasonLength shouldBe 93
        // The March 2026 equinox was on 20 March, so the Sun is just past 0° at noon the next day.
        overview.sunSign shouldBe ZodiacSign.ARIES
    }

    @Test
    fun `days before today count backwards and southern places swap the seasons`() {
        val sydney =
            TEHRAN.copy(
                name = "Sydney",
                coordinates = Coordinates(-33.87, 151.21),
                timeZone = TimeZone.of("Australia/Sydney"),
            )
        val overview = DayDetailsCalculator.overview(beforeNowruz, nowruz, calendars, sydney)

        overview.daysFromToday shouldBe -3
        overview.period shouldBe DatePeriod(0, 0, -3)
        overview.season shouldBe SeasonName.SUMMER
        DayDetailsCalculator.overview(nowruz, nowruz, calendars, sydney).season shouldBe SeasonName.AUTUMN
    }

    @Test
    fun `the Moon is new at the August 2026 solar eclipse and full at the lunar eclipse`() {
        val newMoon = DayDetailsCalculator.overview(gregorian(2026, 8, 12), nowruz, calendars, place = null).moon
        newMoon.phase shouldBe MoonPhaseName.NEW_MOON
        newMoon.illuminatedFraction shouldBeLessThan 0.05f

        val fullMoon = DayDetailsCalculator.overview(gregorian(2026, 8, 28), nowruz, calendars, place = null).moon
        fullMoon.phase shouldBe MoonPhaseName.FULL_MOON
        fullMoon.illuminatedFraction shouldBeGreaterThan 0.95f
    }

    @Test
    fun `phase names are 45 degree sectors centred on the principal phases`() {
        DayDetailsCalculator.phaseName(0.0) shouldBe MoonPhaseName.NEW_MOON
        DayDetailsCalculator.phaseName(22.4) shouldBe MoonPhaseName.NEW_MOON
        DayDetailsCalculator.phaseName(22.6) shouldBe MoonPhaseName.WAXING_CRESCENT
        DayDetailsCalculator.phaseName(90.0) shouldBe MoonPhaseName.FIRST_QUARTER
        DayDetailsCalculator.phaseName(180.0) shouldBe MoonPhaseName.FULL_MOON
        DayDetailsCalculator.phaseName(270.0) shouldBe MoonPhaseName.THIRD_QUARTER
        DayDetailsCalculator.phaseName(337.4) shouldBe MoonPhaseName.WANING_CRESCENT
        DayDetailsCalculator.phaseName(337.6) shouldBe MoonPhaseName.NEW_MOON
        DayDetailsCalculator.phaseName(-10.0) shouldBe MoonPhaseName.NEW_MOON
        DayDetailsCalculator.phaseName(720.0 + 135.0) shouldBe MoonPhaseName.WAXING_GIBBOUS
    }

    @Test
    fun `today's times mark the next primary time and the Sun's progress`() {
        // 10:30 in Tehran: after sunrise, before noon.
        val times = DayDetailsCalculator.times(nowruz, TEHRAN, Instant.parse("2026-03-21T07:00:00Z"))

        times.placeName shouldBe "Tehran"
        times.method shouldBe PrayerMethod.TEHRAN
        times.unavailable.shouldBeNull()
        times.entries.map { it.kind } shouldBe PrayerTimeKind.entries
        val clock = times.entries.map { it.time.shouldNotBeNull() }
        clock.zipWithNext().take(5).forEach { (earlier, later) -> earlier.value shouldBeLessThan later.value }
        times.next shouldBe PrayerTimeKind.DHUHR
        val progress = times.sunProgress.shouldNotBeNull()
        progress shouldBeGreaterThan 0f
        progress shouldBeLessThan 0.5f
    }

    @Test
    fun `other days and late evenings have no next time`() {
        val morning = Instant.parse("2026-03-21T07:00:00Z")
        DayDetailsCalculator.times(nowruz + 1, TEHRAN, morning).let {
            it.next.shouldBeNull()
            it.sunProgress.shouldBeNull()
            it.entries.size shouldBe PrayerTimeKind.entries.size
        }
        // 23:00 in Tehran: after Isha, the Sun has set.
        DayDetailsCalculator.times(nowruz, TEHRAN, Instant.parse("2026-03-21T19:30:00Z")).let {
            it.next.shouldBeNull()
            it.sunProgress.shouldBeNull()
        }
    }

    @Test
    fun `a polar day has no times`() {
        val tromso =
            CalendarPlace(
                "Tromsø",
                Coordinates(69.65, 18.96),
                TimeZone.of("Europe/Oslo"),
                PrayerSettings(PrayerMethod.MWL),
            )
        val times = DayDetailsCalculator.times(gregorian(2026, 6, 21), tromso, Instant.parse("2026-06-21T10:00:00Z"))

        times.unavailable shouldBe PrayerTimesResult.Reason.POLAR_DAY
        times.entries shouldBe emptyList()
        times.next.shouldBeNull()
    }

    @Test
    fun `every day-details value has a string resource`() {
        DayDetailsLabels.TABLES.forEach { (ids, count) ->
            ids.size shouldBe count
            ids.toSet().size shouldBe ids.size
        }
    }
}
