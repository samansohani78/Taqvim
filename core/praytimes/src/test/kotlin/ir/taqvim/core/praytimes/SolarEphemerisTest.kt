/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.praytimes

import io.kotest.common.ExperimentalKotest
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.testing.PropertyTesting
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** ADR-0029: the ephemeris behind prayer times, against NOAA and far from the present. */
class SolarEphemerisTest {
    /**
     * Fixed seed: the same inputs, and so the same covered branches, on every run and machine. The opt-in is for
     * `iterations`, which Kotest 6 still marks experimental.
     */
    @OptIn(ExperimentalKotest::class)
    private val propertyConfig = PropTestConfig(seed = 20_260_919L, iterations = PropertyTesting.iterations)

    @Test
    fun `declination and equation of time agree with NOAA over 1900 to 2100`() {
        var maxDeclination = 0.0
        var maxEquationOfTime = 0.0
        (JDN_1900..JDN_2100 step 3).forEach { jdn ->
            val ut = jdn - 0.5 - J2000_JULIAN_DAY
            val noaa = NoaaSolarCalculator.parameters(jdn - 0.5)
            maxDeclination = maxOf(maxDeclination, abs(SolarEphemeris.sun(ut).declination - noaa.declinationDegrees))
            maxEquationOfTime =
                maxOf(maxEquationOfTime, abs(SolarEphemeris.equationOfTimeMinutes(ut) - noaa.equationOfTimeMinutes))
        }
        // Measured 2026-09-15: at most 0.0032° and 0.065 min (3.9 s); NOAA states its series for 1901–2099.
        maxDeclination shouldBeLessThan NOAA_DECLINATION_BOUND
        maxEquationOfTime shouldBeLessThan NOAA_EQUATION_OF_TIME_BOUND
    }

    @Test
    fun `days outside the range move by whole Gregorian cycles`(): Unit =
        runBlocking {
            SolarEphemeris.cyclesOutOfRange(0) shouldBe 0
            val range = SolarEphemeris.RANGE_CYCLES * SolarEphemeris.GREGORIAN_CYCLE_DAYS
            SolarEphemeris.cyclesOutOfRange(range) shouldBe 0
            SolarEphemeris.cyclesOutOfRange(range + 1) shouldBe 1
            SolarEphemeris.cyclesOutOfRange(-range) shouldBe 0
            SolarEphemeris.cyclesOutOfRange(-range - 1) shouldBe -1
            SolarEphemeris.cyclesOutOfRange(Long.MAX_VALUE) shouldBeGreaterThanOrEqual 1
            checkAll(propertyConfig, Arb.long(-FAR_DAYS..FAR_DAYS)) { days ->
                val folded = days - SolarEphemeris.cyclesOutOfRange(days) * SolarEphemeris.GREGORIAN_CYCLE_DAYS
                abs(folded.toDouble()) shouldBeLessThanOrEqual range.toDouble()
                Math.floorMod(folded - days, SolarEphemeris.GREGORIAN_CYCLE_DAYS) shouldBe 0
                val sun = SolarEphemeris.sun(SolarEphemeris.localMidnight(days + J2000_JDN, 0))
                abs(sun.declination) shouldBeLessThan MAX_DECLINATION
                sun.distanceAu.isFinite() shouldBe true
            }
        }

    private companion object {
        const val J2000_JDN = 2_451_545L
        const val J2000_JULIAN_DAY = 2_451_545.0
        const val JDN_1900 = 2_415_021L
        const val JDN_2100 = 2_488_070L
        const val NOAA_DECLINATION_BOUND = 0.005
        const val NOAA_EQUATION_OF_TIME_BOUND = 0.1
        const val MAX_DECLINATION = 24.5

        /** About ±2.7 million years of days. */
        const val FAR_DAYS = 1_000_000_000L
    }
}
