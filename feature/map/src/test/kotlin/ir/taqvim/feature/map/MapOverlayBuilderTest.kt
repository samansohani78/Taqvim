/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import org.junit.jupiter.api.Test

/** T-1301 overlays: magnetic grids and crescent grids per criterion are computed once per local day. */
class MapOverlayBuilderTest {
    private val magneticLayers =
        setOf(MapLayer.MAGNETIC_DECLINATION, MapLayer.MAGNETIC_INCLINATION, MapLayer.MAGNETIC_INTENSITY)

    @Test
    fun `magnetic grids are computed once per local day`() {
        var calls = 0
        val model = MagneticModel { place, instant -> MapFixtures.MAGNETIC.elements(place, instant).also { calls++ } }
        val builder = MapOverlayBuilder(model, MapFixtures.CRESCENT)
        val cells = LayerGrids.DECLINATION_COLUMNS * LayerGrids.DECLINATION_ROWS

        val noon = builder.build(inputs(magneticLayers)).overlays
        calls shouldBe 3 * cells
        val evening = builder.build(inputs(magneticLayers, hours = 6)).overlays
        calls shouldBe 3 * cells
        evening.inclination shouldBeSameInstanceAs noon.inclination
        builder.build(inputs(magneticLayers, hours = 24))
        calls shouldBe 6 * cells

        val inclination = noon.inclination.shouldNotBeNull()
        val strength = noon.intensity.shouldNotBeNull()
        val row = LayerGrids.DECLINATION_ROWS - 1
        inclination[0, row] shouldBe ShadeGrid.latitudeOf(row, LayerGrids.DECLINATION_ROWS).roundToInt()
        strength[0, row] shouldBe (30_000 + 200 * ShadeGrid.latitudeOf(row, LayerGrids.DECLINATION_ROWS)).roundToInt()
        inclination[0, 0] shouldBe ShadeGrid.NONE
        noon.crescent.shouldBeNull()
    }

    @Test
    fun `each crescent criterion keeps its own day grid`() {
        val seen = mutableListOf<CrescentCriterion>()
        val observer =
            CrescentObserver { place, from, criterion ->
                MapFixtures.CRESCENT.visibility(place, from, criterion).also { seen += criterion }
            }
        val builder = MapOverlayBuilder(MapFixtures.MAGNETIC, observer)
        val crescent = setOf(MapLayer.CRESCENT_VISIBILITY)
        val cells = LayerGrids.CRESCENT_COLUMNS * LayerGrids.CRESCENT_ROWS

        val yallop =
            builder
                .build(inputs(crescent))
                .overlays.crescent
                .shouldNotBeNull()
        val odeh = builder.build(inputs(crescent, criterion = CrescentCriterion.ODEH)).overlays.crescent
        builder.build(inputs(crescent, hours = 1)).overlays.crescent shouldBeSameInstanceAs yallop
        builder
            .build(
                inputs(crescent, hours = 1, criterion = CrescentCriterion.ODEH),
            ).overlays.crescent shouldBeSameInstanceAs
            odeh
        seen.count { it == CrescentCriterion.YALLOP } shouldBe cells
        seen.count { it == CrescentCriterion.ODEH } shouldBe cells
    }

    private fun inputs(
        layers: Set<MapLayer>,
        hours: Int = 0,
        criterion: CrescentCriterion = CrescentCriterion.YALLOP,
    ): MapInputs =
        MapInputs(
            MapFixtures.settings(),
            MapFixtures.NOON + if (hours >= 24) (hours / 24).days else hours.hours,
            true,
            layers,
            null,
            criterion,
        )
}
