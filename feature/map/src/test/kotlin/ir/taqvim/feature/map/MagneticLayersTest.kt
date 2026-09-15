/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import android.hardware.GeomagneticField
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.testing.PropertyTesting
import kotlin.math.roundToInt
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** T-1301 magnetic layers: every cell of the grids holds the platform World Magnetic Model at its center. */
@RunWith(RobolectricTestRunner::class)
class MagneticLayersTest {
    private val from = Instant.parse("2025-01-01T00:00:00Z").toEpochMilliseconds()
    private val until = Instant.parse("2029-12-31T00:00:00Z").toEpochMilliseconds()

    @Test
    fun inclinationStrengthAndDeclinationGridsMatchGeomagneticFieldAtEveryCell(): Unit =
        runBlocking {
            checkAll(minOf(PropertyTesting.iterations, INSTANTS), Arb.long(from..until)) { millis ->
                val instant = Instant.fromEpochMilliseconds(millis)
                val inclination = LayerGrids.inclination(PlatformMagneticModel, instant)
                val strength = LayerGrids.intensity(PlatformMagneticModel, instant)
                val declination = LayerGrids.declination(PlatformMagneticModel, instant)
                for (row in 0 until inclination.rows) {
                    for (column in 0 until inclination.columns) {
                        val field =
                            GeomagneticField(
                                ShadeGrid.latitudeOf(row, inclination.rows).toFloat(),
                                ShadeGrid.longitudeOf(column, inclination.columns).toFloat(),
                                0f,
                                millis,
                            )
                        inclination[column, row] shouldBe field.inclination.toDouble().roundToInt()
                        strength[column, row] shouldBe field.fieldStrength.toDouble().roundToInt()
                        declination[column, row] shouldBe field.declination.toDouble().roundToInt()
                    }
                }
            }
        }

    @Test
    fun theFieldDipsDownInTheNorthUpInTheSouthAndStaysWithinEarthsRange() {
        val instant = Instant.parse("2026-09-14T00:00:00Z")
        val inclination = LayerGrids.inclination(PlatformMagneticModel, instant)
        val strength = LayerGrids.intensity(PlatformMagneticModel, instant)

        for (column in 0 until inclination.columns) {
            inclination[column, 0] shouldBeGreaterThan 60
            inclination[column, inclination.rows - 1] shouldBeLessThan -40
            for (row in 0 until inclination.rows) {
                inclination[column, row] shouldBeInRange -90..90
                strength[column, row] shouldBeInRange 20_000..70_000
            }
        }
    }

    private companion object {
        /** Each instant builds three 72 × 36 grids and checks all 2 592 cells. */
        const val INSTANTS = 4
    }
}
