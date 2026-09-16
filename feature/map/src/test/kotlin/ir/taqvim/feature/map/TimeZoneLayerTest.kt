/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import ir.taqvim.core.i18n.NumeralSystem
import kotlin.time.Instant
import org.junit.jupiter.api.Test

/**
 * T-1301: time-zone offsets computed from the tz rules of the JVM running the tests (tzdb 2026b in the build's JDK), and
 * the plate boundaries shown per zoom. Only offsets settled for years are asserted.
 */
class TimeZoneLayerTest {
    private val outline = MapFixtures.worldOutline()
    private val january = Instant.parse("2026-01-15T12:00:00Z")
    private val july = Instant.parse("2026-07-15T12:00:00Z")

    @Test
    fun `bands of the asset take their offsets from the tz rules, not from the 2012 source`() {
        val bands = outline.timeZones.bands
        bands.mapNotNull { it.zoneId } shouldContainAll
            listOf("Asia/Tehran", "America/Caracas", "Europe/Moscow", "Europe/Paris")

        fun offset(
            zone: String,
            at: Instant,
        ): Int? = TimeZoneOffsets.offsets(bands, at)[bands.indexOfFirst { it.zoneId == zone }]

        offset("Asia/Tehran", january) shouldBe 210
        // Iran has kept standard time all year since 2022.
        offset("Asia/Tehran", july) shouldBe 210
        // Venezuela moved from −4:30 to −4 in 2016, Moscow from +4 to +3 in 2014; the source still has the old offsets.
        offset("America/Caracas", january) shouldBe -240
        bands.first { it.zoneId == "America/Caracas" }.offset2012Minutes shouldBe -270
        offset("Europe/Moscow", january) shouldBe 180
        bands.first { it.zoneId == "Europe/Moscow" }.offset2012Minutes shouldBe 240
        offset("Europe/Paris", january) shouldBe 60
        offset("Europe/Paris", july) shouldBe 120
    }

    @Test
    fun `a boundary shows where the offsets differ, or the 2012 offsets for a band without a known zone`() {
        val bands =
            listOf(
                band("Asia/Tehran", 210),
                band("Asia/Dubai", 240),
                band("Asia/Muscat", 240),
                band(null, 240),
                band(null, 240),
                band(null, 300),
                band("Europe/Nowhere", 300),
            )
        val offsets = TimeZoneOffsets.offsets(bands, january)
        offsets shouldBe listOf(210, 240, 240, null, null, null, null)

        fun shown(
            first: Int,
            second: Int,
        ) = TimeZoneOffsets.isShown(ZoneBoundary(first, second, LINE), bands, offsets)

        shown(0, 1) shouldBe true
        shown(1, 2) shouldBe false
        shown(3, 4) shouldBe false
        shown(3, 5) shouldBe true
        shown(1, 3) shouldBe false
        shown(5, 6) shouldBe false
        shown(0, 6) shouldBe true
        TimeZoneOffsets.minutesAt("Europe/Nowhere", january) shouldBe null
    }

    @Test
    fun `daylight saving at the shown moment decides which boundaries show`() {
        val bands = listOf(band("Europe/London", 0), band("Africa/Abidjan", 0), band("Europe/Lisbon", 0))
        val zones = TimeZoneBands(bands, listOf(ZoneBoundary(0, 1, LINE), ZoneBoundary(0, 2, LINE)))
        TimeZoneOffsets.overlay(zones, january, NumeralSystem.LATIN).boundaries.size shouldBe 0
        TimeZoneOffsets.overlay(zones, july, NumeralSystem.LATIN).boundaries.size shouldBe 1
        TimeZoneOffsets
            .overlay(zones, july, NumeralSystem.LATIN)
            .labels
            .map { it.text } shouldBe listOf("+1", "+0", "+1")
    }

    @Test
    fun `offsets are written with a sign and the language's digits`() {
        TimeZoneOffsets.text(210, NumeralSystem.LATIN) shouldBe "+3:30"
        TimeZoneOffsets.text(345, NumeralSystem.LATIN) shouldBe "+5:45"
        TimeZoneOffsets.text(-240, NumeralSystem.LATIN) shouldBe "−4"
        TimeZoneOffsets.text(-570, NumeralSystem.LATIN) shouldBe "−9:30"
        TimeZoneOffsets.text(0, NumeralSystem.LATIN) shouldBe "+0"
        TimeZoneOffsets.text(210, NumeralSystem.PERSIAN) shouldBe "+۳:۳۰"
    }

    @Test
    fun `the asset's overlay labels every band with a zone, largest first, and hides boundaries of equal offsets`() {
        val overlay = TimeZoneOffsets.overlay(outline.timeZones, january, NumeralSystem.LATIN)
        overlay.labels.size shouldBe outline.timeZones.bands.count { it.zoneId != null }
        overlay.labels.zipWithNext().all { (larger, smaller) -> larger.areaKm2 >= smaller.areaKm2 } shouldBe true
        val tehran = outline.timeZones.bands.first { it.zoneId == "Asia/Tehran" }
        overlay.labels.single { it.point == tehran.label }.text shouldBe "+3:30"
        overlay.boundaries.shouldNotBeEmpty()
        overlay.boundaries.size shouldBeLessThan outline.timeZones.boundaries.size
    }

    @Test
    fun `smaller plates' boundaries show as the zoom grows and every one shows at the maximum zoom`() {
        PlateBoundaries.minimumAreaKm2(1.0) shouldBe PlateBoundaries.AREA_AT_ZOOM_ONE_KM2
        PlateBoundaries.minimumAreaKm2(3.0) shouldBe 10_000.0
        PlateBoundaries.minimumAreaKm2(0.5) shouldBe PlateBoundaries.AREA_AT_ZOOM_ONE_KM2
        PlateBoundaries.minimumAreaKm2(Double.NaN) shouldBe PlateBoundaries.AREA_AT_ZOOM_ONE_KM2
        val counts =
            listOf(1.0, 2.0, 4.0, MapViewport.MAX_ZOOM).map { zoom ->
                outline.plates.count { PlateBoundaries.isShown(it, zoom) }
            }
        counts.zipWithNext().all { (lower, higher) -> lower < higher } shouldBe true
        counts.last() shouldBe outline.plates.size
        val smallest = outline.plates.minBy { it.smallerPlateAreaKm2 }
        PlateBoundaries.isShown(smallest, 1.0) shouldBe false
        PlateBoundaries.isShown(smallest, MapViewport.MAX_ZOOM) shouldBe true
    }

    @Test
    fun `the drawn zoom is the flat map's or the globe's`() {
        MapUiState(viewport = MapViewport(zoom = 2.0)).zoom shouldBe 2.0
        MapUiState(projection = MapProjection.GLOBE, globe = GlobeView(zoom = 3.0)).zoom shouldBe 3.0
    }

    private fun band(
        zone: String?,
        offset2012: Int,
    ) = TimeZoneBand(zone, offset2012, MapPoint(0.5, 0.5), 1)

    private companion object {
        val LINE = floatArrayOf(0f, 0f, 1f, 1f)
    }
}
