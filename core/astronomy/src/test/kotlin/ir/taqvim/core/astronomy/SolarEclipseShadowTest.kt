/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.github.cosinekitty.astronomy.Time
import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.Coordinates
import kotlin.math.abs
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import org.junit.jupiter.api.Test

private const val EARTH_RADIUS_KM = 6378.1366
private const val SUN_DISTANCE_KM = 149_600_000.0
private const val SECONDS_PER_JULIAN_YEAR = 31_557_600L

/** A-13 non-central total and annular solar eclipses from the Moon's shadow axis (Meeus ch. 54). */
class SolarEclipseShadowTest {
    private val epoch = Time.fromMillisecondsSince1970(0L)

    /** A shadow axis parallel to x at [offsetKm] from Earth's centre, with the Moon [moonDistanceKm] away. */
    private fun parallelAxis(
        offsetKm: Double,
        moonDistanceKm: Double,
    ): ShadowAxis =
        SolarEclipseShadow.fromPositions(
            epoch,
            sun = Km3(-SUN_DISTANCE_KM, offsetKm, 0.0),
            moon = Km3(-moonDistanceKm, offsetKm, 0.0),
        )

    @Test
    fun `gamma is the axis distance and the cone sign follows the Moon's distance`() {
        val near = parallelAxis(offsetKm = 6_500.0, moonDistanceKm = 360_000.0)
        near.gamma shouldBe (6_500.0 / EARTH_RADIUS_KM plusOrMinus 1e-12)
        near.umbraRadius shouldBeGreaterThan 0.0
        near.obscuration() shouldBe 1.0

        val far = parallelAxis(offsetKm = 6_400.0, moonDistanceKm = 405_000.0)
        far.umbraRadius shouldBeLessThan 0.0
        far.obscuration() shouldBeLessThan 1.0

        val centred = parallelAxis(offsetKm = 0.0, moonDistanceKm = 384_400.0)
        centred.gamma shouldBe 0.0
        centred.umbraRadius.isFinite() shouldBe true
    }

    @Test
    fun `the cone must reach Earth for a total or annular eclipse`() {
        SolarEclipseShadow.kind(gamma = 0.4, umbraRadius = 0.01) shouldBe EclipseKind.TOTAL
        SolarEclipseShadow.kind(gamma = 0.4, umbraRadius = -0.01) shouldBe EclipseKind.ANNULAR
        SolarEclipseShadow.kind(gamma = 1.005, umbraRadius = 0.01) shouldBe EclipseKind.TOTAL
        SolarEclipseShadow.kind(gamma = 1.005, umbraRadius = -0.01) shouldBe EclipseKind.ANNULAR
        SolarEclipseShadow.kind(gamma = 1.25, umbraRadius = 0.25) shouldBe EclipseKind.TOTAL
        SolarEclipseShadow.kind(gamma = 1.25, umbraRadius = -0.25) shouldBe EclipseKind.ANNULAR
        SolarEclipseShadow.kind(gamma = 1.02, umbraRadius = 0.01) shouldBe EclipseKind.PARTIAL
        SolarEclipseShadow.kind(gamma = 1.02, umbraRadius = -0.01) shouldBe EclipseKind.PARTIAL
        SolarEclipseShadow.kind(gamma = 1.0, umbraRadius = 0.0) shouldBe EclipseKind.ANNULAR
    }

    @Test
    fun `the eight non-central eclipses of 1800 to 2050 are total or annular on the horizon`() {
        val nonCentral =
            mapOf(
                "1928-05-19" to EclipseKind.TOTAL,
                "1950-03-18" to EclipseKind.ANNULAR,
                "1957-04-30" to EclipseKind.ANNULAR,
                "1957-10-23" to EclipseKind.TOTAL,
                "1967-11-02" to EclipseKind.TOTAL,
                "2014-04-29" to EclipseKind.ANNULAR,
                "2043-04-09" to EclipseKind.TOTAL,
                "2043-10-03" to EclipseKind.ANNULAR,
            )
        nonCentral.forEach { (date, kind) ->
            val from = Instant.parse("${date}T00:00:00Z") - 1.days
            val eclipse = Eclipses.solarEclipses(from, from + 3.days).single()
            val axis = SolarEclipseShadow.at(eclipse.peak.toAstronomyTime())
            withClue(date) {
                eclipse.kind shouldBe kind
                axis.gamma shouldBeGreaterThan 1.0
                axis.gamma shouldBeLessThan 1.02
                val latitude = eclipse.peakLatitudeDegrees.shouldNotBeNull()
                val longitude = eclipse.peakLongitudeDegrees.shouldNotBeNull()
                val sun = Sky.skyPosition(CelestialBody.SUN, eclipse.peak, Coordinates(latitude, longitude))
                // Geometrically on the horizon; atmospheric refraction lifts it by about half a degree.
                sun.altitudeDegrees shouldBe (0.5 plusOrMinus 0.5)
                val obscuration = eclipse.obscuration.shouldNotBeNull()
                if (kind == EclipseKind.TOTAL) obscuration shouldBe 1.0 else obscuration shouldBeLessThan 1.0
            }
        }
    }

    @Test
    fun `eclipse types agree with the shadow axis from 2000 BCE to 4000 CE`() {
        val problems =
            (-2_000..4_000 step 250).flatMap { year ->
                val from = Instant.fromEpochSeconds((year - 1970L) * SECONDS_PER_JULIAN_YEAR)
                Eclipses.solarEclipses(from, from + 700.days).take(4).mapNotNull { eclipse ->
                    problem(eclipse)?.let { "$year ${eclipse.peak} ${eclipse.kind}: $it" }
                }
            }
        problems shouldBe emptyList()
        Eclipses
            .solarEclipses(Instant.parse("2029-01-01T00:00:00Z"), Instant.parse("2029-02-01T00:00:00Z"))
            .single()
            .peakLatitudeDegrees
            .shouldBeNull()
    }

    private fun problem(eclipse: GlobalSolarEclipse): String? {
        val axis = SolarEclipseShadow.at(eclipse.peak.toAstronomyTime())
        val reaches = axis.gamma - 1.0 <= abs(axis.umbraRadius)
        val located =
            eclipse.peakLatitudeDegrees?.isFinite() == true && eclipse.peakLongitudeDegrees?.isFinite() == true
        return when {
            !axis.gamma.isFinite() || !axis.umbraRadius.isFinite() -> {
                "shadow axis not finite"
            }

            eclipse.kind == EclipseKind.PARTIAL -> {
                "partial but the cone reaches Earth".takeIf { reaches || located }
            }

            !reaches || !located -> {
                "total or annular without a cone on Earth or a position"
            }

            axis.gamma >= 1.0 && eclipse.kind != SolarEclipseShadow.kind(axis.gamma, axis.umbraRadius) -> {
                "non-central kind disagrees with the cone"
            }

            else -> {
                null
            }
        }
    }
}
