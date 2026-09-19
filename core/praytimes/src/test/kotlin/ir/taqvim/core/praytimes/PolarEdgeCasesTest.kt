/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.praytimes

import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.PrayerMethod
import org.junit.jupiter.api.Test

/**
 * T-601 (U): the edges of the polar seasons and of the nearest-latitude rule, with fixed places and dates. These cases
 * were reached only by chance through the property tests' random latitudes, so coverage of them varied from run to run.
 */
class PolarEdgeCasesTest {
    private fun day(
        year: Int,
        month: Int,
        dayOfMonth: Int,
    ): Jdn = GregorianCalendarSystem.toJdn(CalendarDate(CalendarSystem.GREGORIAN, year, month, dayOfMonth))

    private fun exact(
        day: Jdn,
        place: Coordinates,
        utcOffsetMinutes: Int,
        settings: PrayerSettings,
        parameters: MethodParameters = settings.method.parameters(),
    ): ExactResult = PrayerTimesCalculator.exact(day, place, utcOffsetMinutes, settings, parameters)

    private fun times(result: ExactResult): ExactPrayerTimes = result.shouldBeInstanceOf<ExactResult.Times>().times

    @Test
    fun `a day with a sunset but no sunrise near the South Pole is a polar day`() {
        val result = exact(day(2026, 3, 20), Coordinates(-89.0, 25.0), UTC_PLUS_2, PrayerSettings())

        result shouldBe ExactResult.Missing(PrayerTimesResult.Reason.POLAR_DAY)
    }

    @Test
    fun `a day with a sunrise but no sunset near the South Pole is a polar day`() {
        val result = exact(day(2026, 9, 24), Coordinates(-88.5, 25.0), UTC_PLUS_2, PrayerSettings())

        result shouldBe ExactResult.Missing(PrayerTimesResult.Reason.POLAR_DAY)
    }

    @Test
    fun `at the pole at the equinox there is no Asr and the night starts at this evening's sunset`() {
        // The Sun circles at the horizon, so there is no noon shadow for Asr to lengthen (R05: no time is invented in
        // its place); the day before had no sunset, so this day's own sunset stands in for it and Fajr is still found.
        val t = times(exact(day(2026, 3, 22), Coordinates(-89.5, 25.0), UTC_PLUS_2, PrayerSettings()))

        t.asr.shouldBeNull()
        t.fajr.shouldNotBeNull() shouldBeLessThan t.sunrise
        t.isha.shouldNotBeNull() shouldBeGreaterThan t.sunset
        // Tehran's Maghrib is 4.5° below the horizon, which the Sun does not reach that evening.
        t.maghrib.shouldBeNull()
    }

    @Test
    fun `when the next day has no sunrise this day's own sunrise closes the night`() {
        // At 89° S the Sun has risen for the season by 25 March, so the night after 24 March ends at this morning's
        // sunrise instead.
        val t = times(exact(day(2026, 3, 24), Coordinates(-89.0, 25.0), UTC_PLUS_2, PrayerSettings()))

        t.isha.shouldNotBeNull() shouldBeGreaterThan t.sunset
        t.midnight.shouldNotBeNull() shouldBeGreaterThan t.sunset
    }

    @Test
    fun `sunset-to-Fajr midnight is undefined on a white night without a Fajr`() {
        val settings =
            PrayerSettings(
                PrayerMethod.MWL,
                highLatitude = HighLatitudeRule.NONE,
                midnight = MidnightMode.SUNSET_TO_FAJR,
            )

        val t = times(exact(day(2026, 6, 21), OULU, UTC_PLUS_3, settings))

        t.fajr.shouldBeNull()
        t.maghrib.shouldNotBeNull()
        t.midnight.shouldBeNull()
    }

    @Test
    fun `the nearest-latitude rule gives Tromso a Fajr and Isha where the Sun never reaches 18 degrees`() {
        val none = times(exact(day(2026, 4, 20), TROMSO, UTC_PLUS_2, mwl(HighLatitudeRule.NONE)))
        none.fajr.shouldBeNull()
        none.isha.shouldBeNull()

        val nearest = times(exact(day(2026, 4, 20), TROMSO, UTC_PLUS_2, mwl(HighLatitudeRule.NEAREST_LATITUDE)))

        nearest.fajr.shouldNotBeNull() shouldBeLessThan nearest.sunrise
        nearest.isha.shouldNotBeNull() shouldBeGreaterThan nearest.sunset
    }

    @Test
    fun `an angle the reference parallel never reaches leaves Fajr and Isha undefined rather than guessed`() {
        // Beyond 66° the rule borrows the 45° parallel's twilight. On 1 June the Sun goes no deeper than about 23°
        // below the horizon there, so a 24° method has no twilight to borrow; MWL's 18° does.
        val deep = MethodParameters(24.0, IshaRule.Angle(24.0), MaghribRule.AtSunset, MidnightMode.SUNSET_TO_SUNRISE)
        val settings = mwl(HighLatitudeRule.NEAREST_LATITUDE)

        val unreachable = times(exact(day(2026, 6, 1), ROVANIEMI, UTC_PLUS_3, settings, deep))
        unreachable.fajr.shouldBeNull()
        unreachable.isha.shouldBeNull()

        val reachable = times(exact(day(2026, 6, 1), ROVANIEMI, UTC_PLUS_3, settings))
        reachable.fajr.shouldNotBeNull()
        reachable.isha.shouldNotBeNull()
    }

    private fun mwl(rule: HighLatitudeRule) = PrayerSettings(PrayerMethod.MWL, highLatitude = rule)

    private companion object {
        const val UTC_PLUS_2 = 120
        const val UTC_PLUS_3 = 180
        val TROMSO = Coordinates(69.65, 18.96)
        val OULU = Coordinates(65.01, 25.47)
        val ROVANIEMI = Coordinates(66.50, 25.73)
    }
}
