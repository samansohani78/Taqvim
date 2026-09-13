/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.testing.PropertyTesting
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/**
 * A-14 checked against the definitions themselves: the ascendant rises on the horizon, the midheaven culminates, and
 * each Placidus cusp sits at a third of its semi-arc. Published charts are not available yet (DATA_TODO).
 */
class HousesTest {
    private fun rad(degrees: Double) = degrees * PI / 180

    private fun deg(radians: Double) = radians * 180 / PI

    private fun wrap180(degrees: Double) = ((degrees + 540) % 360) - 180

    /** Equatorial coordinates (degrees) of the ecliptic point at [longitude]. */
    private fun equatorial(
        longitude: Double,
        obliquity: Double,
    ): Pair<Double, Double> {
        val l = rad(longitude)
        val e = rad(obliquity)
        return deg(atan2(sin(l) * cos(e), cos(l))) to deg(asin(sin(e) * sin(l)))
    }

    private fun altitude(
        hourAngle: Double,
        declination: Double,
        latitude: Double,
    ): Double {
        val vertical = sin(rad(latitude)) * sin(rad(declination))
        val horizontal = cos(rad(latitude)) * cos(rad(declination)) * cos(rad(hourAngle))
        val sine = vertical + horizontal
        return deg(asin(sine))
    }

    private val firstMillis = Instant.parse("1950-01-01T00:00:00Z").toEpochMilliseconds()
    private val lastMillis = Instant.parse("2050-01-01T00:00:00Z").toEpochMilliseconds()
    private val instants = Arb.long(firstMillis..lastMillis)

    @Test
    fun `the ascendant rises on the eastern horizon and the midheaven culminates`(): Unit =
        runBlocking {
            checkAll(
                PropertyTesting.iterations,
                instants,
                Arb.int(-60..60),
                Arb.int(-179..179),
            ) { millis, latitude, longitude ->
                val instant = Instant.fromEpochMilliseconds(millis)
                val place = Coordinates(latitude.toDouble(), longitude.toDouble())
                val chart = Houses.placidus(instant, place).shouldNotBeNull()
                val obliquity = Houses.obliquityDegrees(instant)
                val ramc = Houses.ramcDegrees(instant, place)

                val (ascRa, ascDec) = equatorial(chart.ascendant, obliquity)
                val ascHourAngle = wrap180(ramc - ascRa)
                altitude(ascHourAngle, ascDec, place.latitude) shouldBe (0.0 plusOrMinus 1e-6)
                ascHourAngle shouldBeLessThan 0.0

                val (mcRa, _) = equatorial(chart.midheaven, obliquity)
                wrap180(ramc - mcRa) shouldBe (0.0 plusOrMinus 1e-6)
            }
        }

    @Test
    fun `Placidus cusps trisect the semi-arcs and follow in zodiacal order`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, instants, Arb.int(-60..60)) { millis, latitude ->
                val instant = Instant.fromEpochMilliseconds(millis)
                val place = Coordinates(latitude.toDouble(), 51.42)
                val chart = Houses.placidus(instant, place).shouldNotBeNull()
                val obliquity = Houses.obliquityDegrees(instant)
                val ramc = Houses.ramcDegrees(instant, place)

                listOf(11 to 1.0 / 3, 12 to 2.0 / 3).forEach { (cusp, fraction) ->
                    val (ra, dec) = equatorial(chart.cusps[cusp - 1], obliquity)
                    val semiArc = deg(acos(-tan(rad(latitude.toDouble())) * tan(rad(dec))))
                    wrap180(ra - ramc) shouldBe (fraction * semiArc plusOrMinus 1e-6)
                }
                chart.cusps[0] shouldBe chart.ascendant
                chart.cusps[9] shouldBe (chart.midheaven plusOrMinus 1e-9)
                (0 until 6).forEach { chart.cusps[it + 6] shouldBe ((chart.cusps[it] + 180) % 360 plusOrMinus 1e-9) }
                chart.cusps.indices.forEach { index ->
                    val step = (chart.cusps[(index + 1) % 12] - chart.cusps[index] + 360) % 360
                    step shouldBeGreaterThan 0.0
                    step shouldBeLessThan 180.0
                }
            }
        }

    @Test
    fun `Placidus is undefined inside the polar circles`() {
        Houses.placidus(Instant.parse("2026-06-21T12:00:00Z"), Coordinates(70.0, 25.0)).shouldBeNull()
        Houses.placidus(Instant.parse("2026-06-21T12:00:00Z"), Coordinates(-67.0, 25.0)).shouldBeNull()
    }

    @Test
    fun `lots swap their formulas between day and night charts`() {
        val day = Houses.lots(ascendant = 100.0, sunLongitude = 10.0, moonLongitude = 40.0, dayChart = true)
        day.fortune shouldBe (130.0 plusOrMinus 1e-9)
        day.spirit shouldBe (70.0 plusOrMinus 1e-9)
        val night = Houses.lots(ascendant = 100.0, sunLongitude = 10.0, moonLongitude = 40.0, dayChart = false)
        night.fortune shouldBe (70.0 plusOrMinus 1e-9)
        night.spirit shouldBe (130.0 plusOrMinus 1e-9)
        Houses.lots(ascendant = 350.0, sunLongitude = 300.0, moonLongitude = 10.0, dayChart = true).fortune shouldBe
            (60.0 plusOrMinus 1e-9)

        val tehran = Coordinates(35.70, 51.42)
        val noon = Instant.parse("2026-09-13T08:30:00Z")
        val chart = Houses.placidus(noon, tehran).shouldNotBeNull()
        Houses.lots(noon, tehran, chart.ascendant).dayChart shouldBe true
        Houses.lots(Instant.parse("2026-09-13T20:30:00Z"), tehran, chart.ascendant).dayChart shouldBe false
    }
}
