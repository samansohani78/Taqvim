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
            val place = placeWithRamc(row.ramc, row.latitude)
            val chart = Houses.placidus(EPOCH_1908, place).shouldNotBeNull()

            Houses.ramcDegrees(EPOCH_1908, place) shouldBe (row.ramc plusOrMinus RAMC_TOLERANCE)
            // Agreement is better than 0.7 arcminutes on every row; see [ARCMINUTES].
            chart.midheaven shouldBe (row.midheaven plusOrMinus ARCMINUTES)
            chart.cusps[0] shouldBe (row.ascendant plusOrMinus ARCMINUTES)
        }
    }

    /**
     * The intermediate cusps too, at the one latitude Dalton prints in full.
     *
     * His columns are compressed — a value shows only the digits that changed from the row above — so cusps 11, 12,
     * 2 and 3 cannot be read in isolation partway down a block. Latitude 22 is the first row of every block and is
     * printed whole, which is why this fixture is one latitude across three sidereal times rather than one sidereal
     * time across many latitudes. Together with the ascendant fixture it covers every cusp the table publishes.
     */
    @Test
    fun `all five cusps match Dalton at the latitude he prints in full`() {
        val rows = readCuspGolden()
        rows.size shouldBe 3

        rows.forEach { row ->
            val place = placeWithRamc(row.ramc, row.latitude)
            val chart = Houses.placidus(EPOCH_1908, place).shouldNotBeNull()

            chart.midheaven shouldBe (row.midheaven plusOrMinus ARCMINUTES)
            chart.cusps[0] shouldBe (row.ascendant plusOrMinus ARCMINUTES)
            // Cusps 11, 12, 2 and 3 are printed to a tenth of a degree, so they are compared at that resolution.
            chart.cusps[10] shouldBe (row.cusp11 plusOrMinus TENTH_DEGREE)
            chart.cusps[11] shouldBe (row.cusp12 plusOrMinus TENTH_DEGREE)
            chart.cusps[1] shouldBe (row.cusp2 plusOrMinus TENTH_DEGREE)
            chart.cusps[2] shouldBe (row.cusp3 plusOrMinus TENTH_DEGREE)
        }
    }

    private data class CuspRow(
        val latitude: Double,
        val ramc: Double,
        val midheaven: Double,
        val cusp11: Double,
        val cusp12: Double,
        val ascendant: Double,
        val cusp2: Double,
        val cusp3: Double,
    )

    private fun readCuspGolden(): List<CuspRow> =
        File(CUSP_GOLDEN)
            .readLines()
            .filterNot { it.startsWith("#") || it.isBlank() || it.startsWith("latitudeDegrees") }
            .map { line ->
                val f = line.split(",").map(String::toDouble)
                CuspRow(
                    latitude = f[0],
                    ramc = f[1],
                    midheaven = f[2],
                    cusp11 = f[3],
                    cusp12 = f[4],
                    ascendant = f[5],
                    cusp2 = f[6],
                    cusp3 = f[7],
                )
            }

    /**
     * A place at [latitude] whose meridian carries right ascension [ramc] at [EPOCH_1908].
     *
     * Dalton tabulates by right ascension of the midheaven, not by instant and place, so each row has to be put back
     * into those terms. No search is needed: RAMC is the sidereal angle plus the longitude, so the longitude that
     * produces a given RAMC at a given moment follows directly. Fixing the moment in 1908 also fixes the obliquity
     * of the ecliptic, which his tables assume at his own epoch and which [Houses] derives from the instant.
     */
    private fun placeWithRamc(
        ramc: Double,
        latitude: Double,
    ): Coordinates {
        val atGreenwich = Houses.ramcDegrees(EPOCH_1908, Coordinates(latitude, 0.0))
        return Coordinates(latitude, wrap180(ramc - atGreenwich))
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
        const val CUSP_GOLDEN = "src/test/resources/golden/dalton/cusps-lat22.csv"

        /** Dalton's epoch: his tables assume the obliquity of his own time, which [Houses] derives from the instant. */
        val EPOCH_1908: Instant = Instant.parse("1908-07-01T00:00:00Z")

        const val RAMC_TOLERANCE = 1e-9

        /**
         * Measured, not chosen for comfort: every row agrees to better than 0.7 arcminutes, and the test fails at
         * 0.2, so this is a real bound rather than slack. Dalton prints to the whole arcminute, so his own rounding
         * is of that order and the residue is as small as a comparison with him can be.
         */
        const val ARCMINUTES = 0.7 / 60.0

        /**
         * Measured: every cusp agrees to better than 0.04°, and the test fails at 0.02. Dalton prints these columns
         * to a tenth of a degree, so his own rounding is +/-0.05° and the residue is inside it.
         */
        const val TENTH_DEGREE = 0.04
    }
}
