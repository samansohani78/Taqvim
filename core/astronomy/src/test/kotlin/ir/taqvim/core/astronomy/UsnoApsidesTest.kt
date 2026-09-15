/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import ir.taqvim.core.testing.GoldenFile
import ir.taqvim.core.testing.PropertyTesting
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** Columns of `golden/usno/apsides-1700-2100.csv`. */
private const val EVENT = 1
private const val INSTANT = 2

private const val KM_PER_AU = 149_597_870.7
private const val MILLIS_PER_DAY = 86_400_000.0
private const val J2000_EPOCH_DAY = 10_957.5
private const val DAYS_PER_CENTURY = 36_525.0

/** A-13 Earth apsides against every perihelion and aphelion USNO lists for 1700–2100 (golden/usno), and far years. */
class UsnoApsidesTest {
    private val usno =
        GoldenFile
            .load("golden/usno/apsides-1700-2100.csv")
            .lines
            .drop(1)
            .map { it.split(',') }
            .map { row -> ApsisKind.valueOf(row[EVENT].uppercase()) to Instant.parse(row[INSTANT]) }

    private val computed =
        Sky.earthApsides(Instant.parse("1700-01-01T00:00:00Z"), Instant.parse("2101-01-01T00:00:00Z"))

    @Test
    fun `every perihelion and aphelion of 1700 to 2100 matches USNO in order and time`() {
        usno shouldHaveSize 801
        computed.map { it.kind } shouldBe usno.map { it.first }
        val deviations = computed.zip(usno).map { (ours, published) -> ours.instant - published.second }
        // Near an apsis the Sun–Earth distance is nearly flat, so kilometre-level ephemeris differences move the
        // instant by hours (see the distance test below). Measured on 2026-09-15 with the same library version:
        // largest |deviation| per century 1700s 86 min, 1800s 85 min, 1900s 131 min, 2000s 121 min, 2100 49 min;
        // overall mean 26 min, RMS 33 min, 95th percentile 63 min. USNO rounds to the minute.
        deviations.filter { it.absoluteValue >= 2.5.hours }.shouldBeEmpty()
        val rmsSeconds = sqrt(deviations.sumOf { it.inWholeSeconds.toDouble() * it.inWholeSeconds } / deviations.size)
        rmsSeconds shouldBeLessThan 40.0 * 60
        shouldThrow<IllegalArgumentException> { Sky.earthApsides(Instant.DISTANT_PAST, Instant.DISTANT_PAST) }
    }

    @Test
    fun `USNO's instants lie on the same extremum of the Earth center's distance from the Sun`() {
        // An Earth–Moon barycenter apsis is up to about 1.4 days away and would miss by hundreds of kilometres; the
        // Earth-center extremum agrees with USNO's instant to within about 2 km (measured 2026-09-15).
        computed.zip(usno).forEach { (ours, published) ->
            abs(Sky.earthSunDistanceAu(published.second) - ours.distanceAu) * KM_PER_AU shouldBeLessThan 5.0
        }
    }

    /**
     * Far from the present the apsides still alternate half an anomalistic year (365.2596 d) apart, give the known
     * perihelion and aphelion distances, and occur where the Sun's geocentric longitude equals the Earth's longitude
     * of perihelion ϖ + 180° (or ϖ for aphelion), ϖ = 102.937348° + 1.7195269° T + 0.00045962° T² (Meeus, Astronomical
     * Algorithms, 2nd ed., Table 31.A, mean equinox of date). The lunar wobble shifts an apsis by up to about three
     * days, hence the tolerances.
     */
    @Test
    fun `apsides of years -2000 to 6000 alternate and follow the precessing perihelion`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.int(-2_000..6_000)) { year ->
                val start = Instant.fromEpochMilliseconds(((year - 1970) * 365.2425 * MILLIS_PER_DAY).toLong())
                val apsides = Sky.earthApsides(start, start + 1_100.days)
                apsides.size shouldBeGreaterThanOrEqualTo 5
                apsides.zipWithNext().forEach { (a, b) ->
                    b.kind shouldNotBe a.kind
                    (b.instant - a.instant).inWholeMinutes / 1_440.0 shouldBe (182.63 plusOrMinus 4.0)
                }
                apsides.windowed(3).forEach { (a, _, c) ->
                    (c.instant - a.instant).inWholeMinutes / 1_440.0 shouldBe (365.2596 plusOrMinus 4.0)
                }
                apsides.forEach(::checkOrbitGeometry)
            }
        }

    private fun checkOrbitGeometry(apsis: EarthApsis) {
        val centuries = (apsis.instant.toEpochMilliseconds() / MILLIS_PER_DAY - J2000_EPOCH_DAY) / DAYS_PER_CENTURY
        val perihelionLongitude = 102.937348 + 1.7195269 * centuries + 0.00045962 * centuries * centuries
        val sunLongitude = Sky.eclipticPosition(CelestialBody.SUN, apsis.instant).longitudeDegrees
        val (expectedSunLongitude, expectedDistance) =
            when (apsis.kind) {
                ApsisKind.PERIHELION -> perihelionLongitude + 180.0 to 0.9835
                ApsisKind.APHELION -> perihelionLongitude to 1.0165
            }
        abs(LunarLimb.signed(sunLongitude - expectedSunLongitude)) shouldBeLessThan 3.5
        apsis.distanceAu shouldBe (expectedDistance plusOrMinus 0.0035)
    }
}
