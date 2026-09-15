/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.testing.PropertyTesting
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sin
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-403 lunar tilt: Meeus Example 48.a, parallactic-angle invariants and the quarter-Moon orientation. */
class LunarLimbTest {
    private val tehran = Coordinates(35.69, 51.39)
    private val sydney = Coordinates(-33.87, 151.21)
    private val start = Instant.parse("2026-01-01T00:00:00Z")

    private fun angleBetween(
        a: Double,
        b: Double,
    ): Double = abs(LunarLimb.signed(a - b))

    @Test
    fun `bright limb position angle matches Meeus Example 48 a`() {
        // 1992 April 12, 0h TD: Sun α0 = 20.6579°, δ0 = +8.6964°; Moon α = 134.6885°, δ = +13.7684°; χ = 285.0°.
        LunarLimb.brightLimbPositionAngle(20.6579, 8.6964, 134.6885, 13.7684) shouldBe (285.0 plusOrMinus 0.1)
    }

    @Test
    fun `parallactic angle is zero on the meridian and follows the hour angle`(): Unit =
        runBlocking {
            val degrees = Arb.int(-80..80)
            checkAll(PropertyTesting.iterations, Arb.int(-179..180), degrees, degrees) { hour, latitude, declination ->
                val q = LunarLimb.parallacticAngle(hour.toDouble(), latitude.toDouble(), declination.toDouble())
                q shouldBeGreaterThanOrEqual -180.0
                q shouldBeLessThanOrEqual 180.0
                if (hour != 0 && hour != 180) q.sign shouldBe sin(Math.toRadians(hour.toDouble())).sign
                if (latitude != declination) {
                    val meridian = LunarLimb.parallacticAngle(0.0, latitude.toDouble(), declination.toDouble())
                    abs(meridian) shouldBe if (declination < latitude) 0.0 else 180.0
                }
            }
        }

    @Test
    fun `angles stay finite and in range`(): Unit =
        runBlocking {
            val hours = Arb.long(0L..(10 * 365 * 24).toLong())
            checkAll(
                PropertyTesting.iterations / 10,
                hours,
                Arb.int(-890..890),
                Arb.int(-1_800..1_800),
            ) { h, lat, lon ->
                val instant = Instant.parse("2020-01-01T00:00:00Z") + h.hours
                val tilt = Sky.moonTilt(instant, Coordinates(lat / 10.0, lon / 10.0))
                tilt.positionAngleDegrees shouldBeGreaterThanOrEqual 0.0
                tilt.positionAngleDegrees shouldBeLessThan 360.0
                tilt.tiltDegrees shouldBeGreaterThan -180.0
                tilt.tiltDegrees shouldBeLessThanOrEqual 180.0
                angleBetween(tilt.tiltDegrees, tilt.positionAngleDegrees - tilt.parallacticAngleDegrees) shouldBe
                    (0.0 plusOrMinus 1e-9)
            }
            LunarLimb.signed(-180.0) shouldBe 180.0
            LunarLimb.signed(540.0) shouldBe 180.0
            LunarLimb.signed(-190.0) shouldBe (170.0 plusOrMinus 1e-9)
        }

    @Test
    fun `the bright limb faces west at first quarter and east at last quarter`() {
        Sky.moonQuarters(start, start + 365.days).forEach { event ->
            val positionAngle = Sky.moonTilt(event.instant, tehran).positionAngleDegrees
            when (event.quarter) {
                MoonQuarter.FIRST_QUARTER -> angleBetween(positionAngle, 270.0) shouldBeLessThan 30.0
                MoonQuarter.THIRD_QUARTER -> angleBetween(positionAngle, 90.0) shouldBeLessThan 30.0
                else -> Unit
            }
        }
    }

    @Test
    fun `at transit the tilt is mirrored in the southern hemisphere like the Moon image`() {
        val firstQuarter = Sky.moonQuarters(start, start + 40.days).first { it.quarter == MoonQuarter.FIRST_QUARTER }
        listOf(tehran to -90.0, sydney to 90.0).forEach { (place, expected) ->
            val from = firstQuarter.instant - 12.hours
            val transit = Sky.riseSetTransit(CelestialBody.MOON, place, from).transit.shouldNotBeNull()
            val tilt = Sky.moonTilt(transit, place)
            angleBetween(tilt.parallacticAngleDegrees, if (place.latitude > 0) 0.0 else 180.0) shouldBeLessThan 2.0
            angleBetween(tilt.tiltDegrees, expected) shouldBeLessThan 35.0
            (tilt.tiltDegrees < 0) shouldBe Sky.moonAppearance(transit, place).brightLimbOnRight
        }
    }
}
