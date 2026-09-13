/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.double
import io.kotest.property.checkAll
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.testing.PropertyTesting
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/**
 * A-11 against an independent vector formulation. City coordinates are rounded sample inputs (approximate city
 * centres), not official values; the assertion is agreement between two formulations.
 */
class GreatCircleTest {
    private data class Vector(
        val x: Double,
        val y: Double,
        val z: Double,
    ) {
        infix fun dot(o: Vector) = x * o.x + y * o.y + z * o.z

        infix fun cross(o: Vector) = Vector(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)

        operator fun minus(o: Vector) = Vector(x - o.x, y - o.y, z - o.z)

        operator fun times(k: Double) = Vector(x * k, y * k, z * k)

        val length get() = sqrt(this dot this)
    }

    private fun unit(c: Coordinates): Vector {
        val lat = c.latitude * PI / 180
        val lon = c.longitude * PI / 180
        return Vector(cos(lat) * cos(lon), cos(lat) * sin(lon), sin(lat))
    }

    /** Bearing as the angle of the tangent towards [to] measured from local north towards local east. */
    private fun oracleBearing(
        from: Coordinates,
        to: Coordinates,
    ): Double {
        val a = unit(from)
        val b = unit(to)
        val lat = from.latitude * PI / 180
        val lon = from.longitude * PI / 180
        val east = Vector(-sin(lon), cos(lon), 0.0)
        val north = Vector(-sin(lat) * cos(lon), -sin(lat) * sin(lon), cos(lat))
        val tangent = b - a * (a dot b)
        return (atan2(tangent dot east, tangent dot north) * 180 / PI).mod(360.0)
    }

    private fun oracleDistanceKm(
        from: Coordinates,
        to: Coordinates,
    ): Double = atan2((unit(from) cross unit(to)).length, unit(from) dot unit(to)) * GreatCircle.EARTH_MEAN_RADIUS_KM

    private val cities =
        listOf(
            "Tehran" to Coordinates(35.69, 51.39),
            "Mashhad" to Coordinates(36.30, 59.60),
            "Kabul" to Coordinates(34.53, 69.17),
            "Istanbul" to Coordinates(41.01, 28.98),
            "Berlin" to Coordinates(52.52, 13.40),
            "Sydney" to Coordinates(-33.87, 151.21),
            "Jakarta" to Coordinates(-6.21, 106.85),
            "New York" to Coordinates(40.71, -74.01),
            "Cape Town" to Coordinates(-33.92, 18.42),
            "Kathmandu" to Coordinates(27.72, 85.32),
        )

    @TestFactory
    fun `qibla bearing and distance agree with the vector formulation for ten cities`(): List<DynamicTest> =
        cities.map { (name, city) ->
            DynamicTest.dynamicTest(name) {
                Qibla.bearingDegrees(city).shouldNotBeNull() shouldBe
                    (oracleBearing(city, Qibla.KAABA) plusOrMinus 1e-6)
                Qibla.distanceKm(city) shouldBe (oracleDistanceKm(city, Qibla.KAABA) plusOrMinus 1e-3)
            }
        }

    @Test
    fun `Tehran faces south-west`() {
        Qibla.bearingDegrees(Coordinates(35.69, 51.39)).shouldNotBeNull() shouldBe (218.4 plusOrMinus 0.2)
    }

    @Test
    fun `coincident, antipodal and polar starting points have no single bearing`() {
        val antipode = Coordinates(-Qibla.KAABA.latitude, Qibla.KAABA.longitude - 180.0)

        Qibla.bearingDegrees(Qibla.KAABA).shouldBeNull()
        Qibla.bearingDegrees(antipode).shouldBeNull()
        Qibla.distanceKm(antipode) shouldBe (PI * GreatCircle.EARTH_MEAN_RADIUS_KM plusOrMinus 1e-3)
        GreatCircle.initialBearingDegrees(Coordinates(90.0, 0.0), Qibla.KAABA).shouldBeNull()
        GreatCircle.initialBearingDegrees(Coordinates(0.0, 0.0), Coordinates(10.0, 0.0)).shouldNotBeNull() shouldBe
            (0.0 plusOrMinus 1e-9)
        GreatCircle.initialBearingDegrees(Coordinates(0.0, 0.0), Coordinates(0.0, -10.0)).shouldNotBeNull() shouldBe
            (270.0 plusOrMinus 1e-9)
    }

    @Test
    fun `random pairs agree with the oracle`(): Unit =
        runBlocking {
            val latitudes = Arb.double(-89.0..89.0, includeNaNs = false)
            val longitudes = Arb.double(-180.0..180.0, includeNaNs = false)
            checkAll(
                PropertyTesting.iterations,
                latitudes,
                longitudes,
                latitudes,
                longitudes,
            ) { lat1, lon1, lat2, lon2 ->
                val from = Coordinates(lat1, lon1)
                val to = Coordinates(lat2, lon2)
                val angle = GreatCircle.centralAngleRadians(from, to)
                GreatCircle.distanceKm(from, to) shouldBe (oracleDistanceKm(from, to) plusOrMinus 1e-3)
                if (angle > 1e-4 && PI - angle > 1e-4) {
                    val bearing = GreatCircle.initialBearingDegrees(from, to).shouldNotBeNull()
                    val difference = abs(bearing - oracleBearing(from, to))
                    minOf(difference, 360 - difference) shouldBe (0.0 plusOrMinus 1e-4)
                }
            }
        }
}
