/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.praytimes

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.model.AsrJuristic
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.PrayerMethod
import ir.taqvim.core.testing.PropertyTesting
import ir.taqvim.core.testing.TimingTest
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.tan
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/** ADR-0029: horizon, Asr, high latitudes, method adjustments and rounding of the prayer-time model. */
class PrayerModelTest {
    private fun day(
        year: Int,
        month: Int,
        dayOfMonth: Int,
    ): Jdn = GregorianCalendarSystem.toJdn(CalendarDate(CalendarSystem.GREGORIAN, year, month, dayOfMonth))

    private fun exact(
        day: Jdn,
        place: Coordinates,
        utcOffsetMinutes: Int,
        settings: PrayerSettings = PrayerSettings(),
    ): ExactPrayerTimes {
        val result = PrayerTimesCalculator.exact(day, place, utcOffsetMinutes, settings, settings.method.parameters())
        return result.shouldBeInstanceOf<ExactResult.Times>().times
    }

    @Test
    fun `sunrise altitude follows refraction, semi-diameter and dip`() {
        val standard = HorizonSettings()
        standard.sunriseAltitude(1.0, 0.0) shouldBe (-(34.0 + 16.0) / 60 plusOrMinus 1e-12)
        standard.sunriseAltitude(0.983, 0.0) shouldBe standard.sunriseAltitude(1.0, 0.0)
        val actual = HorizonSettings(useActualSemiDiameter = true)
        actual.sunriseAltitude(1.0, 0.0) shouldBe (-(34.0 + 959.63 / 60) / 60 plusOrMinus 1e-12)
        actual.sunriseAltitude(0.983, 0.0) shouldBe (-(34.0 + 959.63 / 60 / 0.983) / 60 plusOrMinus 1e-12)
        // Meeus ch. 16: refraction scales with P/1010 and 283/(273 + T).
        val cold = HorizonSettings(pressureHectopascals = 909.0, temperatureCelsius = -10.0)
        val scale = 909.0 / 1010 * 283 / 263
        cold.sunriseAltitude(1.0, 0.0) shouldBe (-(34.0 * scale + 16.0) / 60 plusOrMinus 1e-12)
        // Dip 1.76′√h counts only when asked for, and never below sea level.
        standard.sunriseAltitude(1.0, 100.0) shouldBe standard.sunriseAltitude(1.0, 0.0)
        val dip = HorizonSettings(useElevation = true)
        dip.sunriseAltitude(1.0, 100.0) - dip.sunriseAltitude(1.0, 0.0) shouldBe
            (-1.76 * sqrt(100.0) / 60 plusOrMinus 1e-12)
        dip.sunriseAltitude(1.0, -50.0) shouldBe dip.sunriseAltitude(1.0, 0.0)
        shouldThrow<IllegalArgumentException> { HorizonSettings(pressureHectopascals = -1.0) }
        shouldThrow<IllegalArgumentException> { HorizonSettings(temperatureCelsius = -300.0) }
        shouldThrow<IllegalArgumentException> { HorizonSettings(pressureHectopascals = Double.NaN) }
        shouldThrow<IllegalArgumentException> { HorizonSettings(temperatureCelsius = Double.POSITIVE_INFINITY) }
    }

    @Test
    fun `elevation makes sunrise earlier and sunset later by the dip`() {
        val place = Coordinates(35.70, 51.42, elevationMeters = 1_200.0)
        val sea = exact(day(2026, 3, 20), place, 210)
        val high = exact(day(2026, 3, 20), place, 210, PrayerSettings(horizon = HorizonSettings(useElevation = true)))
        // Near the equinox at 35.7° the Sun climbs about cos φ / 4 degrees per minute.
        val expectedMinutes = 1.76 * sqrt(1_200.0) / 60 / (0.25 * cos(Math.toRadians(35.7)))
        sea.sunrise - high.sunrise shouldBe (expectedMinutes plusOrMinus 0.2)
        high.sunset - sea.sunset shouldBe (expectedMinutes plusOrMinus 0.2)
        high.dhuhr shouldBe sea.dhuhr
    }

    @Test
    fun `events land on their altitudes and Asr on the shadow rule`() {
        val place = Coordinates(35.70, 51.42)
        val sky = SunDay.of(day(2026, 9, 13), place, 210)
        val times = exact(day(2026, 9, 13), place, 210)
        sky.altitudeAt(times.fajr.shouldNotBeNull()) shouldBe (-17.7 plusOrMinus 1e-6)
        sky.altitudeAt(times.maghrib.shouldNotBeNull()) shouldBe (-4.5 plusOrMinus 1e-6)
        sky.altitudeAt(times.isha.shouldNotBeNull()) shouldBe (-14.0 plusOrMinus 1e-6)
        AsrJuristic.entries.forEach { juristic ->
            val asr = exact(day(2026, 9, 13), place, 210, PrayerSettings(asr = juristic)).asr.shouldNotBeNull()
            val noonShadow = tan(Math.toRadians(abs(place.latitude - sky.declinationAt(sky.transit))))
            1 / tan(Math.toRadians(sky.altitudeAt(asr))) shouldBe (juristic.shadowFactor + noonShadow plusOrMinus 1e-6)
            asr shouldBeGreaterThan times.dhuhr
        }
        // Hand-derived: on the equator at the equinox the noon shadow is nil, so Asr (factor 1) is at 45° altitude,
        // an hour angle of 45° — three hours after transit.
        val equinox = day(2026, 3, 20)
        val equator = Coordinates(0.0, 0.0)
        val equatorSky = SunDay.of(equinox, equator, 0)
        val equatorTimes = exact(equinox, equator, 0, PrayerSettings(PrayerMethod.MWL))
        abs(equatorSky.declinationAt(equatorSky.transit)) shouldBeLessThan 0.2
        Math.toDegrees(atan(1.0)) shouldBe 45.0
        equatorTimes.asr.shouldNotBeNull() - equatorTimes.dhuhr shouldBe (180.0 plusOrMinus 1.5)
    }

    @Test
    fun `nearest latitude borrows the twilight of the closest latitude that has one`() {
        HighLatitude.referenceLatitude(60.0, 23.0, 18.0) shouldBe (49.0 plusOrMinus 1e-12)
        HighLatitude.referenceLatitude(-60.0, -23.0, 18.0) shouldBe (-49.0 plusOrMinus 1e-12)
        HighLatitude.referenceLatitude(70.0, 23.0, 18.0) shouldBe 45.0
        HighLatitude.referenceLatitude(-70.0, -23.0, 18.0) shouldBe -45.0

        val midsummer = day(2026, 6, 21)
        listOf(60.0, 65.0).forEach { latitude ->
            val place = Coordinates(latitude, 25.0)
            val settings = PrayerSettings(PrayerMethod.MWL, highLatitude = HighLatitudeRule.NEAREST_LATITUDE)
            val none = exact(midsummer, place, 180, settings.copy(highLatitude = HighLatitudeRule.NONE))
            none.fajr.shouldBeNull()
            none.isha.shouldBeNull()
            val nearest = exact(midsummer, place, 180, settings)
            val fajr = nearest.fajr.shouldNotBeNull()
            val isha = nearest.isha.shouldNotBeNull()
            isha shouldBeGreaterThan nearest.sunset
            fajr shouldBeLessThan nearest.sunrise
            // Both fall inside the night; at 65° N it is so short that Isha can pass the next morning's Fajr.
            isha shouldBeLessThan nearest.sunrise + 1_440
            fajr shouldBeGreaterThan nearest.sunset - 1_440
        }
        // Winter nights keep their computed twilight.
        val winter = day(2026, 12, 21)
        val oslo = Coordinates(60.0, 10.75)
        listOf(HighLatitudeRule.NONE, HighLatitudeRule.NEAREST_LATITUDE)
            .map { exact(winter, oslo, 60, PrayerSettings(PrayerMethod.MWL, highLatitude = it)) }
            .distinct()
            .size shouldBe 1
        // 70° N: polar day in June and polar night in December.
        val tromso = Coordinates(70.0, 19.0)
        PrayerTimesCalculator.calculate(midsummer, tromso, 120) shouldBe
            PrayerTimesResult.Unavailable(PrayerTimesResult.Reason.POLAR_DAY)
        PrayerTimesCalculator.calculate(winter, tromso, 60) shouldBe
            PrayerTimesResult.Unavailable(PrayerTimesResult.Reason.POLAR_NIGHT)
    }

    @Test
    fun `nearest latitude Isha changes smoothly into the season without twilight`() {
        val place = Coordinates(55.0, 13.0)
        val nearest = PrayerSettings(PrayerMethod.MWL, highLatitude = HighLatitudeRule.NEAREST_LATITUDE)
        val start = day(2026, 5, 1)
        val days = (0L..70L).map { start + it }
        val ishas = days.map { exact(it, place, 120, nearest).isha.shouldNotBeNull() }
        val computed = days.map { exact(it, place, 120, nearest.copy(highLatitude = HighLatitudeRule.NONE)).isha }
        computed.first().shouldNotBeNull()
        computed.last().shouldBeNull()
        // Where the twilight exists nothing changes.
        ishas.zip(computed).forEach { (isha, plain) -> if (plain != null) isha shouldBe plain }
        // Approaching the twilight-less season the computed Isha itself steepens (13.5 minutes on the last day), and
        // the first borrowed day steps once more (23.5 minutes, measured 2026-09-15) because that last computed Isha
        // is still short of lower culmination. Inside the season the borrowed time then creeps by a quarter of a
        // minute a day, instead of vanishing.
        val switch = computed.indexOfFirst { it == null }
        ishas.zipWithNext().forEachIndexed { index, (earlier, later) ->
            val step = abs(later - earlier)
            if (index < switch) step shouldBeLessThan 25.0 else step shouldBeLessThan 1.0
        }
    }

    @Test
    fun `Diyanet adds its published temkin`() {
        val istanbul = Coordinates(41.01, 28.98)
        val today = day(2026, 9, 13)
        val diyanet =
            PrayerTimesCalculator
                .calculate(today, istanbul, 180, PrayerSettings(PrayerMethod.DIYANET))
                .shouldBeInstanceOf<PrayerTimesResult.Available>()
                .times
        val plain = exact(today, istanbul, 180, PrayerSettings(PrayerMethod.MWL))

        fun minutes(value: Double) = PrayerTimesCalculator.minuteOf(value, MinuteRounding.NEAREST).value
        diyanet.fajr?.value shouldBe minutes(plain.fajr.shouldNotBeNull())
        diyanet.sunrise.value shouldBe minutes(plain.sunrise - 7)
        diyanet.dhuhr.value shouldBe minutes(plain.dhuhr + 5)
        diyanet.asr?.value shouldBe minutes(plain.asr.shouldNotBeNull() + 4)
        diyanet.sunset.value shouldBe minutes(plain.sunset)
        diyanet.maghrib?.value shouldBe minutes(plain.sunset + 7)
        diyanet.isha?.value shouldBe minutes(plain.isha.shouldNotBeNull())
    }

    @Test
    fun `minutes round as the method says`() {
        PrayerTimesCalculator.minuteOf(720.5, MinuteRounding.NEAREST).value shouldBe 721
        PrayerTimesCalculator.minuteOf(720.49, MinuteRounding.NEAREST).value shouldBe 720
        PrayerTimesCalculator.minuteOf(720.99, MinuteRounding.FLOOR).value shouldBe 720
        PrayerTimesCalculator.minuteOf(720.0 - 1e-12, MinuteRounding.FLOOR).value shouldBe 720
        PrayerTimesCalculator.minuteOf(720.01, MinuteRounding.CEILING).value shouldBe 721
        PrayerTimesCalculator.minuteOf(720.0 + 1e-12, MinuteRounding.CEILING).value shouldBe 720
        PrayerTimesCalculator.minuteOf(-1.0, MinuteRounding.NEAREST).value shouldBe 1_439
        PrayerTimesCalculator.minuteOf(1_450.2, MinuteRounding.NEAREST).value shouldBe 10
    }

    @Test
    fun `every method has parameters`() {
        PrayerMethod.entries.forEach { it.parameters().fajrAngle shouldBeGreaterThan 0.0 }
    }

    @Test
    fun `times stay ordered, finite and continuous for any year and latitude`(): Unit =
        runBlocking {
            checkAll(
                PropertyTesting.iterations,
                Arb.int(-90..90),
                Arb.int(-180..180),
                Arb.long(FIRST_TESTED_JDN..LAST_TESTED_JDN),
            ) { latitude, longitude, jdn ->
                val place = Coordinates(latitude.toDouble(), longitude.toDouble())
                val offset = Math.round(longitude / 15.0).toInt() * 60
                val settings = PrayerSettings(PrayerMethod.MWL, highLatitude = HighLatitudeRule.ANGLE_BASED)
                val parameters = settings.method.parameters()
                val result = PrayerTimesCalculator.exact(Jdn(jdn), place, offset, settings, parameters)
                if (result is ExactResult.Times) {
                    val times = result.times
                    val sequence =
                        listOfNotNull(
                            times.fajr,
                            times.sunrise,
                            times.dhuhr,
                            times.asr,
                            times.sunset,
                            times.maghrib,
                            times.isha,
                        )
                    sequence.forEach { it.isFinite() shouldBe true }
                    sequence.zipWithNext().forEach { (earlier, later) -> earlier shouldBeLessThan later + 1e-6 }
                    if (abs(latitude) <= 60) {
                        val next = PrayerTimesCalculator.exact(Jdn(jdn + 1), place, offset, settings, parameters)
                        val tomorrow = next.shouldBeInstanceOf<ExactResult.Times>().times
                        abs(tomorrow.sunrise - times.sunrise) shouldBeLessThan 10.0
                        abs(tomorrow.dhuhr - times.dhuhr) shouldBeLessThan 1.0
                        abs(tomorrow.sunset - times.sunset) shouldBeLessThan 10.0
                    }
                }
            }
        }

    @Test
    fun `days millions of years away still give times`() {
        val tehran = Coordinates(35.70, 51.42)
        listOf(-1_000_000_000_000L, 1_000_000_000_000L, Long.MAX_VALUE / 2).forEach { jdn ->
            PrayerTimesCalculator.calculate(Jdn(jdn), tehran, 210).shouldBeInstanceOf<PrayerTimesResult.Available>()
        }
    }

    @Test
    @Tag(TimingTest.TAG)
    fun `a year of times for one city takes under 50 ms`() {
        val tehran = Coordinates(35.70, 51.42)
        val start = day(2026, 3, 21)
        repeat(2) { (0L until 365L).forEach { PrayerTimesCalculator.calculate(start + it, tehran, 210) } }
        val began = System.nanoTime()
        (0L until 365L).forEach { PrayerTimesCalculator.calculate(start + it, tehran, 210) }
        (System.nanoTime() - began) / 1e6 shouldBeLessThan TimingTest.budget(50.0)
    }

    private companion object {
        /** 1 January −2000 and 31 December 6000 (proleptic Gregorian), the ephemeris range. */
        const val FIRST_TESTED_JDN = 990_558L
        const val LAST_TESTED_JDN = 3_912_900L
    }
}
