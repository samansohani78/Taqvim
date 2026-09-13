/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.praytimes

import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.model.AsrJuristic
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.PrayerMethod
import ir.taqvim.core.testing.PropertyTesting
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class PrayerTimesCalculatorTest {
    private val tehran = Coordinates(35.70, 51.42)

    private fun day(
        year: Int,
        month: Int,
        dayOfMonth: Int,
    ): Jdn = GregorianCalendarSystem.toJdn(CalendarDate(CalendarSystem.GREGORIAN, year, month, dayOfMonth))

    private fun times(
        day: Jdn,
        place: Coordinates,
        utcOffsetMinutes: Int,
        settings: PrayerSettings = PrayerSettings(),
    ): PrayerTimes {
        val result = PrayerTimesCalculator.calculate(day, place, utcOffsetMinutes, settings)
        result.shouldBeInstanceOf<PrayerTimesResult.Available>()
        return result.times
    }

    /** Minutes from [earlier] to [later] going forward across midnight. */
    private fun after(
        earlier: Int,
        later: Int,
    ): Int = Math.floorMod(later - earlier, MINUTES_PER_DAY)

    /** The time of day halfway from [start] forward to [end]. */
    private fun middle(
        start: Int,
        end: Int,
    ): Int = Math.floorMod(start + after(start, end) / 2, MINUTES_PER_DAY)

    @Test
    fun `polar day and polar night are reported, not computed`() {
        val svalbard = Coordinates(78.2, 15.6)

        PrayerTimesCalculator.calculate(day(2026, 12, 21), svalbard, 60) shouldBe
            PrayerTimesResult.Unavailable(PrayerTimesResult.Reason.POLAR_NIGHT)
        PrayerTimesCalculator.calculate(day(2026, 6, 21), svalbard, 120) shouldBe
            PrayerTimesResult.Unavailable(PrayerTimesResult.Reason.POLAR_DAY)
    }

    @Test
    fun `high-latitude rules bound Fajr and Isha in short summer nights`() {
        val helsinki = Coordinates(60.17, 24.94)
        val midsummer = day(2026, 6, 21)

        fun mwl(rule: HighLatitudeRule) =
            times(midsummer, helsinki, 180, PrayerSettings(PrayerMethod.MWL, highLatitude = rule))

        val none = mwl(HighLatitudeRule.NONE)
        none.fajr.shouldBeNull()
        none.isha.shouldBeNull()

        val sunrise = none.sunrise.value
        val sunset = none.sunset.value
        val night = after(sunset, sunrise)
        val portions =
            listOf(
                Triple(HighLatitudeRule.MIDDLE_OF_NIGHT, 0.5, 0.5),
                Triple(HighLatitudeRule.ONE_SEVENTH, 1.0 / 7, 1.0 / 7),
                Triple(HighLatitudeRule.ANGLE_BASED, 18.0 / 60, 17.0 / 60),
            )
        portions.forEach { (rule, fajrPortion, ishaPortion) ->
            val adjusted = mwl(rule)
            val fajrGap = after(adjusted.fajr.shouldNotBeNull().value, sunrise)
            val ishaGap = after(sunset, adjusted.isha.shouldNotBeNull().value)
            abs(fajrGap - (night * fajrPortion).roundToInt()) shouldBeLessThanOrEqual 2
            abs(ishaGap - (night * ishaPortion).roundToInt()) shouldBeLessThanOrEqual 2
        }

        val whiteNights =
            times(
                midsummer,
                helsinki,
                180,
                PrayerSettings(PrayerMethod.TEHRAN, highLatitude = HighLatitudeRule.GEOPHYSICS_WHITE_NIGHTS),
            )
        after(whiteNights.dhuhr.value, whiteNights.fajr.shouldNotBeNull().value) shouldBe 12 * 60 + 30
    }

    @Test
    fun `high-latitude rules leave ordinary nights alone`() {
        val june = day(2026, 6, 21)

        val plain = times(june, tehran, 210, PrayerSettings(highLatitude = HighLatitudeRule.NONE))
        times(june, tehran, 210, PrayerSettings(highLatitude = HighLatitudeRule.ANGLE_BASED)) shouldBe plain
        times(june, tehran, 210, PrayerSettings(highLatitude = HighLatitudeRule.GEOPHYSICS_WHITE_NIGHTS)) shouldBe plain
        val seventh = times(june, tehran, 210, PrayerSettings(highLatitude = HighLatitudeRule.ONE_SEVENTH))
        // A seventh of Tehran's short June night is less than the 17.7° twilight, so Fajr moves later.
        after(plain.fajr.shouldNotBeNull().value, seventh.fajr.shouldNotBeNull().value) shouldBeGreaterThan 0
    }

    @Test
    fun `method-specific rules`() {
        val makkah = Coordinates(21.4225, 39.8262)
        val today = day(2026, 9, 13)

        val ummAlQura = times(today, makkah, 180, PrayerSettings(PrayerMethod.MAKKAH))
        after(ummAlQura.maghrib.shouldNotBeNull().value, ummAlQura.isha.shouldNotBeNull().value) shouldBe 90
        ummAlQura.maghrib shouldBe ummAlQura.sunset

        val standard = times(today, makkah, 180, PrayerSettings(PrayerMethod.MWL))
        val hanafi = times(today, makkah, 180, PrayerSettings(PrayerMethod.MWL, AsrJuristic.HANAFI))
        after(standard.asr.value, hanafi.asr.value) shouldBeGreaterThan 30

        val tehranTimes = times(today, tehran, 210)
        after(tehranTimes.sunset.value, tehranTimes.maghrib.shouldNotBeNull().value) shouldBeGreaterThan 10
    }

    @Test
    fun `midnight modes use their own interval`() {
        val today = day(2026, 9, 13)
        val modes = MidnightMode.entries.associateWith { times(today, tehran, 210, PrayerSettings(midnight = it)) }
        val base = modes.getValue(MidnightMode.SUNSET_TO_SUNRISE)
        val fajr = base.fajr.shouldNotBeNull().value
        val maghrib = base.maghrib.shouldNotBeNull().value
        val expected =
            mapOf(
                MidnightMode.SUNSET_TO_SUNRISE to middle(base.sunset.value, base.sunrise.value),
                MidnightMode.SUNSET_TO_FAJR to middle(base.sunset.value, fajr),
                MidnightMode.MAGHRIB_TO_SUNRISE to middle(maghrib, base.sunrise.value),
                MidnightMode.MAGHRIB_TO_FAJR to middle(maghrib, fajr),
            )

        expected.forEach { (mode, minute) ->
            val actual =
                modes
                    .getValue(mode)
                    .midnight
                    .shouldNotBeNull()
                    .value
            minOf(after(actual, minute), after(minute, actual)) shouldBeLessThanOrEqual 1
        }

        val arcticSettings =
            PrayerSettings(highLatitude = HighLatitudeRule.NONE, midnight = MidnightMode.MAGHRIB_TO_FAJR)
        val arctic = times(day(2026, 6, 10), Coordinates(64.0, 25.0), 180, arcticSettings)
        arctic.maghrib.shouldBeNull()
        arctic.midnight.shouldBeNull()
    }

    @Test
    fun `every method gives ordered times at non-polar latitudes`(): Unit =
        runBlocking {
            checkAll(
                PropertyTesting.iterations,
                Arb.enum<PrayerMethod>(),
                Arb.int(-45..45),
                Arb.int(-179..179),
                Arb.int(0..18_262),
            ) { method, latitude, longitude, dayOffset ->
                val offset = Math.round(longitude / 15.0).toInt() * 60
                val place = Coordinates(latitude.toDouble(), longitude.toDouble())
                val t = times(Jdn(J2000_JDN + dayOffset), place, offset, PrayerSettings(method))
                val fajr = t.fajr.shouldNotBeNull().value
                val sequence =
                    listOf(
                        fajr,
                        t.sunrise.value,
                        t.dhuhr.value,
                        t.asr.value,
                        t.sunset.value,
                        t.maghrib.shouldNotBeNull().value,
                        t.isha.shouldNotBeNull().value,
                    )
                sequence.zipWithNext().forEach { (earlier, later) ->
                    after(fajr, earlier) shouldBeLessThanOrEqual after(fajr, later)
                }
                method.parameters().fajrAngle shouldBeGreaterThan 0.0
            }
        }

    private companion object {
        const val MINUTES_PER_DAY = 1_440
        const val J2000_JDN = 2_451_545L
    }
}
