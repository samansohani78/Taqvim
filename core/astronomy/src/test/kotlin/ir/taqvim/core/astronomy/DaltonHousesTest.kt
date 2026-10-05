/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.Coordinates
import java.io.File
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import org.junit.jupiter.api.Test

/**
 * A-14 against a **published** table of houses (DT-018): Dalton's *The Spherical Basis of Astrology* (1908), the
 * page for sidereal time 13h 47m 49s, digitised in the public domain by the Library of Congress. Until this fixture
 * the Placidus code was checked only against the definition it is written from — a closed loop, however carefully
 * reasoned. This is an independent arithmetic done by someone else, on paper, before any of it was computable.
 *
 * Dalton tabulates by right ascension of the midheaven and latitude, not by instant and place, so each row is
 * reproduced by holding the latitude and searching for the moment whose [Houses.ramcDegrees] equals his ARC. The
 * instant is kept in 1908 because the obliquity of the ecliptic is a function of time and his tables assume his own
 * epoch's value; a modern instant would shift the ascendant by more than the tolerance below.
 */
class DaltonHousesTest {
    @Test
    fun `the ascendant and midheaven match Dalton's 1908 table of houses`() {
        val rows = readGolden()
        rows.size shouldBe 14

        rows.forEach { row ->
            val place = Coordinates(row.latitude, 0.0)
            val instant = instantWithRamc(row.ramc, place)
            val chart = Houses.placidus(instant, place).shouldNotBeNull()

            Houses.ramcDegrees(instant, place) shouldBe (row.ramc plusOrMinus RAMC_TOLERANCE)
            // Agreement is better than 0.7 arcminutes on every row; see [ARCMINUTES].
            chart.midheaven shouldBe (row.midheaven plusOrMinus ARCMINUTES)
            chart.cusps[0] shouldBe (row.ascendant plusOrMinus ARCMINUTES)
        }
    }

    /**
     * The moment at which [place] sees right ascension [ramc] on its meridian, within [RAMC_TOLERANCE].
     *
     * Sidereal time runs a little faster than solar time, so stepping by the error in sidereal degrees converted to
     * solar seconds converges in a few passes; the loop is bounded so a failure to converge fails the test rather
     * than hanging.
     */
    private fun instantWithRamc(
        ramc: Double,
        place: Coordinates,
    ): Instant {
        var instant = EPOCH_1908
        repeat(ITERATIONS) {
            val error = wrap180(ramc - Houses.ramcDegrees(instant, place))
            if (abs(error) < RAMC_TOLERANCE) return instant
            instant += (error / SIDEREAL_DEGREES_PER_SECOND).seconds
        }
        error("no instant near 1908 put right ascension $ramc on the meridian")
    }

    private fun wrap180(degrees: Double): Double = ((degrees + 540) % 360) - 180

    private data class Row(
        val latitude: Double,
        val ramc: Double,
        val midheaven: Double,
        val ascendant: Double,
    )

    private fun readGolden(): List<Row> =
        File(GOLDEN)
            .readLines()
            .filterNot { it.startsWith("#") || it.isBlank() || it.startsWith("latitudeDegrees") }
            .map { line ->
                val fields = line.split(",").map(String::toDouble)
                Row(latitude = fields[0], ramc = fields[1], midheaven = fields[2], ascendant = fields[3])
            }

    private companion object {
        const val GOLDEN = "src/test/resources/golden/dalton/ascendant-sid13h47m49s.csv"

        /** Dalton's epoch: his tables assume the obliquity of his own time, which [Houses] derives from the instant. */
        val EPOCH_1908: Instant = Instant.parse("1908-07-01T00:00:00Z")

        /** Degrees of right ascension the meridian sweeps per second of solar time. */
        const val SIDEREAL_DEGREES_PER_SECOND = 360.0 / 86_164.0905

        const val RAMC_TOLERANCE = 1e-6
        const val ITERATIONS = 40

        /**
         * Measured, not chosen for comfort: every row agrees to better than 0.7 arcminutes, and the test fails at
         * 0.2, so this is a real bound rather than slack. Dalton prints to the whole arcminute, so his own rounding
         * is of that order and the residue is as small as a comparison with him can be.
         */
        const val ARCMINUTES = 0.7 / 60.0
    }
}
