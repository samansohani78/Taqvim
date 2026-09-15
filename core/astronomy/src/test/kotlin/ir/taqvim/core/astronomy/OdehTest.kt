/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeGreaterThan as intShouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.Coordinates
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import org.junit.jupiter.api.Test

/** Odeh's criterion against Table V and equation 2 of the paper, and real evenings around a new moon. */
class OdehTest {
    @Test
    fun `the zones of Table V lie on either side of its arcs of vision`() {
        val misplaced =
            TABLE_V.flatMap { (width, arcs) ->
                arcs.mapIndexedNotNull { index, arc ->
                    val above = Odeh.classify(Odeh.v(arc + TABLE_ROUNDING, width))
                    val below = Odeh.classify(Odeh.v(arc - TABLE_ROUNDING, width))
                    // ARCV1 starts zone C, ARCV2 zone B and ARCV3 zone A.
                    val zone = OdehZone.entries[OdehZone.entries.size - 2 - index]
                    "W=$width ARCV${index + 1}=$arc: $below/$above".takeIf {
                        above != zone || below != OdehZone.entries[zone.ordinal + 1]
                    }
                }
            }
        withClue(misplaced.joinToString("\n")) { misplaced.shouldBeEmpty() }
    }

    @Test
    fun `zone limits of equation 2 include their lower bounds`() {
        Odeh.classify(5.65) shouldBe OdehZone.A
        Odeh.classify(5.649) shouldBe OdehZone.B
        Odeh.classify(2.0) shouldBe OdehZone.B
        Odeh.classify(1.999) shouldBe OdehZone.C
        Odeh.classify(-0.96) shouldBe OdehZone.C
        Odeh.classify(-0.961) shouldBe OdehZone.D
        Odeh.v(7.1651, 0.0) shouldBe (0.0 plusOrMinus 1e-12)
        Odeh.v(10.0, 1.0) shouldBe (10.0 - (-0.1018 + 0.7319 - 6.3226 + 7.1651) plusOrMinus 1e-12)
    }

    @Test
    fun `the crescent is not visible on the evening of a new moon and visible two evenings later`() {
        val newMoon =
            Sky
                .moonQuarters(Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-03-01T00:00:00Z"))
                .first { it.quarter == MoonQuarter.NEW_MOON }
                .instant

        val sameEvening = Odeh.evening(TEHRAN, newMoon - 12.hours)
        (sameEvening == null || sameEvening.zone == OdehZone.D) shouldBe true
        val later = Odeh.evening(TEHRAN, newMoon + 2.days).shouldNotBeNull()
        later.zone shouldBeLessThanOrEqualTo OdehZone.B
        later.v shouldBe Odeh.v(later.arcVisionDegrees, later.widthArcMinutes)
        later.lagMinutes shouldBeGreaterThan 60.0
    }

    @Test
    fun `the topocentric arc of vision is Yallop's geocentric one lowered by the Moon's parallax`() {
        val start = Instant.parse("2026-08-13T00:00:00Z")
        val places = listOf(TEHRAN, Coordinates(-33.9, 18.4), Coordinates(21.4, 39.8), Coordinates(51.5, -0.1))
        val evenings =
            places.flatMap { place ->
                (0..3).mapNotNull { day ->
                    val odeh = Odeh.evening(place, start + day.days)
                    val yallop = Yallop.evening(place, start + day.days)
                    if (odeh == null || yallop == null) null else odeh to yallop
                }
            }
        evenings.size intShouldBeGreaterThan 8
        evenings.forEach { (odeh, yallop) ->
            odeh.bestTime shouldBe yallop.bestTime
            val lowering = yallop.arcVisionDegrees - odeh.arcVisionDegrees
            lowering shouldBeGreaterThan 0.8
            lowering shouldBeLessThan 1.05
            // Parallax also moves the Moon towards the set Sun: the topocentric arc of light is smaller.
            val arcLightLowering = yallop.arcLightDegrees - odeh.arcLightDegrees
            arcLightLowering shouldBeGreaterThan 0.0
            arcLightLowering shouldBeLessThan 1.05
        }
    }

    @Test
    fun `no evening without a sunset and no morning without a sunrise`() {
        Odeh.evening(Coordinates(80.0, 15.0), Instant.parse("2026-06-21T00:00:00Z")).shouldBeNull()
        Odeh.morning(Coordinates(80.0, 15.0), Instant.parse("2026-06-21T00:00:00Z")).shouldBeNull()
    }

    private companion object {
        val TEHRAN = Coordinates(35.70, 51.42)

        /** Arcs of vision are printed to 0.1°. */
        const val TABLE_ROUNDING = 0.1

        /** Table V ("New criteria"): width W in arc minutes to ARCV1, ARCV2 and ARCV3 in degrees. */
        val TABLE_V: List<Pair<Double, List<Double>>> =
            listOf(
                0.1 to listOf(5.6, 8.5, 12.2),
                0.2 to listOf(5.0, 7.9, 11.6),
                0.3 to listOf(4.4, 7.3, 11.0),
                0.4 to listOf(3.8, 6.7, 10.4),
                0.5 to listOf(3.2, 6.2, 9.8),
                0.6 to listOf(2.7, 5.6, 9.3),
                0.7 to listOf(2.1, 5.1, 8.7),
                0.8 to listOf(1.6, 4.5, 8.2),
                0.9 to listOf(1.0, 4.0, 7.6),
            )
    }
}
