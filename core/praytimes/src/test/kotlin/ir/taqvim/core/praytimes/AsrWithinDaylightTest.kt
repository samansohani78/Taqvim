/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.praytimes

import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
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
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/**
 * R05 (review 2026-09-19): Asr must fall inside the day's daylight. A fresh property run found Asr after sunset at a
 * polar-boundary latitude, where the noon Sun is at or below the true horizon and the shadow rule has no solution.
 */
class AsrWithinDaylightTest {
    private val settings = PrayerSettings(PrayerMethod.MWL, highLatitude = HighLatitudeRule.ANGLE_BASED)

    @Test
    fun `the input the review's property run failed on keeps the times in order`() {
        // latitude=-69, longitude=-171, JDN=3342886 (29 May 4440), seed=8171453473268066201: Asr came out
        // 753.46 minutes after midnight and sunset 752.45.
        val longitude = -171
        val offset = Math.round(longitude / 15.0).toInt() * 60
        val result =
            PrayerTimesCalculator.exact(
                Jdn(REVIEW_JDN),
                Coordinates(-69.0, longitude.toDouble()),
                offset,
                settings,
                settings.method.parameters(),
            )

        val t = result.shouldBeInstanceOf<ExactResult.Times>().times
        val sequence = listOfNotNull(t.fajr, t.sunrise, t.dhuhr, t.asr, t.sunset, t.maghrib, t.isha)
        sequence.zipWithNext().forEach { (earlier, later) -> earlier shouldBeLessThan later + 1e-6 }
    }

    @Test
    fun `on 29 May 4440 at 69 degrees south Asr is unavailable instead of after sunset`() {
        val day = GregorianCalendarSystem.toJdn(CalendarDate(CalendarSystem.GREGORIAN, 4440, 5, 29))
        day shouldBe Jdn(REVIEW_JDN)

        val result = PrayerTimesCalculator.calculate(day, Coordinates(-69.0, -171.0), UTC_MINUS_11, settings)

        // The Sun still clears the refracted horizon for a few minutes, so the day has a sunrise and a sunset — but
        // at transit it is below the true horizon, where the shadow rule has no answer.
        val times = result.shouldBeInstanceOf<PrayerTimesResult.Available>().times
        times.asr.shouldBeNull()
        times.sunrise.value shouldBeLessThan times.dhuhr.value
        times.dhuhr.value shouldBeLessThan times.sunset.value
    }

    @Test
    fun `at Tromso on the edge of the polar night Asr is never after sunset`() {
        // Found by the completeness test (REVIEW R14): 27 November 2001 printed dhuhr 11:32, Asr 11:40, sunset 11:39.
        // The Sun culminates only just above the refracted horizon, below the true one, so there is no Asr.
        val day = GregorianCalendarSystem.toJdn(CalendarDate(CalendarSystem.GREGORIAN, 2001, 11, 27))
        val tromso = Coordinates(69.6492, 18.9553)
        val nearest = PrayerSettings(highLatitude = HighLatitudeRule.NEAREST_LATITUDE)

        val times =
            PrayerTimesCalculator
                .calculate(day, tromso, UTC_PLUS_1, nearest)
                .shouldBeInstanceOf<PrayerTimesResult.Available>()
                .times

        times.asr.shouldBeNull()
        times.dhuhr.value shouldBeLessThan times.sunset.value
    }

    @Test
    fun `a noon Sun barely above the true horizon never reaches the Asr altitude, so Asr is unavailable`() {
        // 23 March 2026 at 88.84° S: the Sun clears the horizon (it rises and sets) but culminates only about 0.05°
        // above the true horizon, and the afternoon altitude the shadow rule asks for is never reached.
        val day = GregorianCalendarSystem.toJdn(CalendarDate(CalendarSystem.GREGORIAN, 2026, 3, 23))

        val result = PrayerTimesCalculator.exact(day, Coordinates(-88.84, 25.0), UTC_PLUS_2, settings, parameters())

        result
            .shouldBeInstanceOf<ExactResult.Times>()
            .times.asr
            .shouldBeNull()
    }

    private fun parameters() = settings.method.parameters()

    /**
     * Deliberately unseeded: every run draws new inputs near the latitude where the noon Sun touches the true horizon
     * (φ ≈ ∓(90° − |δ|)), the class of input that exposed R05, so no fixed seed can leave it out. Branch coverage does
     * not depend on it — the deterministic cases above and in [PolarEdgeCasesTest] cover every reachable branch.
     */
    @Test
    fun `wherever Asr exists it lies between Dhuhr and sunset near the polar boundary`(): Unit =
        runBlocking {
            checkAll(
                PropertyTesting.iterations,
                Arb.long(FIRST_TESTED_JDN..LAST_TESTED_JDN),
                Arb.int(-BOUNDARY_BAND_HUNDREDTHS..BOUNDARY_BAND_HUNDREDTHS),
                Arb.int(-180..180),
                Arb.enum<AsrJuristic>(),
            ) { jdn, bandHundredths, longitude, juristic ->
                val offset = Math.round(longitude / 15.0).toInt() * 60
                val probe = SunDay.of(Jdn(jdn), Coordinates(0.0, longitude.toDouble()), offset)
                val declination = probe.declinationAt(probe.transit)
                val boundary = -sign(declination) * (RIGHT_ANGLE - abs(declination))
                val latitude = (boundary + bandHundredths / 100.0).coerceIn(-RIGHT_ANGLE, RIGHT_ANGLE)
                val juristicSettings = settings.copy(asr = juristic)
                val result =
                    PrayerTimesCalculator.exact(
                        Jdn(jdn),
                        Coordinates(latitude, longitude.toDouble()),
                        offset,
                        juristicSettings,
                        juristicSettings.method.parameters(),
                    )
                if (result is ExactResult.Times) {
                    val t = result.times
                    t.asr?.let { asr ->
                        asr shouldBeGreaterThanOrEqual t.dhuhr
                        asr shouldBeLessThanOrEqual t.sunset
                    }
                    val sequence = listOfNotNull(t.sunrise, t.dhuhr, t.asr, t.sunset)
                    sequence.zipWithNext().forEach { (earlier, later) -> earlier shouldBeLessThan later + 1e-6 }
                }
            }
        }

    private companion object {
        const val REVIEW_JDN = 3_342_886L
        const val UTC_MINUS_11 = -660
        const val UTC_PLUS_2 = 120
        const val UTC_PLUS_1 = 60
        const val RIGHT_ANGLE = 90.0

        /** Hundredths of a degree either side of the boundary latitude that the property samples (±3°). */
        const val BOUNDARY_BAND_HUNDREDTHS = 300

        /** The same span of years as the all-latitude property in [PrayerModelTest]. */
        const val FIRST_TESTED_JDN = 990_558L
        const val LAST_TESTED_JDN = 3_912_900L
    }
}
