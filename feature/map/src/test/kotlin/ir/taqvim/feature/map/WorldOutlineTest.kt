/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.longs.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import org.junit.jupiter.api.Test

/** T-1301 assets: the Natural Earth outline, time zones and plates match their generators' hashes and parse completely. */
class WorldOutlineTest {
    private val text = File("src/main/assets/map/world-110m.txt").readText()
    private val timeZones = File("src/main/assets/${WorldOutlineParser.TIME_ZONES_ASSET}").readText()
    private val plates = File("src/main/assets/${WorldOutlineParser.PLATES_ASSET}").readText()

    @Test
    fun `the asset body matches the hash its generator recorded`() {
        text shouldContain "# source-sha256-land: 9e0729ee253ca7d7a5c4ae9395fb1902264c5377c52e224d13dd85010e2835d9"
        text shouldContain "# license: public domain"
        WorldOutlineParser.bodyHashMatches(text) shouldBe true
        WorldOutlineParser.bodyHashMatches(text + "L 0,0 100,100\n") shouldBe false
        WorldOutlineParser.bodyHashMatches("L 0,0 100,100\n") shouldBe false
    }

    @Test
    fun `the time-zone and plate assets match their generators' hashes and name their licences`() {
        timeZones shouldContain "# source-sha256: aa52ab97e5f906693f31fe2e625c9d6be78a4e74fcd4142a7261235b355cacfe"
        timeZones shouldContain "# license: public domain"
        timeZones shouldContain "# cities-sha256: "
        plates shouldContain "# source-sha256: 06d444d22a55ff4c265199955168c537511d1d9feea958850e754e7c5759e33d"
        plates shouldContain "# license: CC BY 4.0"
        WorldOutlineParser.bodyHashMatches(timeZones) shouldBe true
        WorldOutlineParser.bodyHashMatches(plates) shouldBe true
    }

    @Test
    fun `the asset holds Natural Earth's land rings and boundary lines on the map`() {
        val outline = WorldOutlineParser.parse(text)
        outline.land.size shouldBe 128
        outline.borders.size shouldBe 333
        (outline.land + outline.borders).all { part -> part.size >= 4 && part.all { it in 0f..1f } } shouldBe true
    }

    @Test
    fun `the line assets hold the bands, their boundaries and the plate boundaries on the map`() {
        val outline = WorldOutlineParser.parse(listOf(text, timeZones, plates))
        val zones = outline.timeZones
        outline.land.size shouldBe 128
        zones.bands.size shouldBe 128
        zones.bands.count { it.zoneId != null } shouldBe 60
        zones.boundaries.size shouldBe 986
        outline.plates.size shouldBe 1_247
        val lines = zones.boundaries.map { it.line } + outline.plates.map { it.line }
        lines.all { part -> part.size >= 4 && part.all { it in 0f..1f } } shouldBe true
        zones.bands.all { it.label.x in 0.0..1.0 && it.label.y in 0.0..1.0 && it.areaKm2 > 0 } shouldBe true
        zones.bands.sumOf { it.areaKm2 } shouldBeInRange
            (EARTH_AREA_KM2 * 999 / 1_000)..(EARTH_AREA_KM2 * 1_001 / 1_000)
        outline.plates.all { it.smallerPlateAreaKm2 > 0 } shouldBe true
    }

    @Test
    fun `band, boundary and plate lines are parsed`() {
        val parsed =
            WorldOutlineParser.parse(
                "T Asia/Tehran 210 5142,3570 1600000\nT - 240 5500,2500 90000\n" +
                    "Z 0 1 -18000,9000 18000,-9000\nP 1437 0,0 18000,0\n",
            )
        val (tehran, sea) = parsed.timeZones.bands
        tehran.zoneId shouldBe "Asia/Tehran"
        tehran.offset2012Minutes shouldBe 210
        tehran.areaKm2 shouldBe 1_600_000
        tehran.label.x shouldBe ((51.42 + 180) / 360 plusOrMinus 1e-6)
        tehran.label.y shouldBe ((90 - 35.7) / 180 plusOrMinus 1e-6)
        sea.zoneId shouldBe null
        sea.offset2012Minutes shouldBe 240
        val boundary = parsed.timeZones.boundaries.single()
        (boundary.first to boundary.second) shouldBe (0 to 1)
        boundary.line.toList() shouldBe listOf(0f, 0f, 1f, 1f)
        parsed.plates.single().smallerPlateAreaKm2 shouldBe 1_437
        parsed.plates
            .single()
            .line
            .toList() shouldBe listOf(0.5f, 0.5f, 1f, 0.5f)
    }

    @Test
    fun `outline lines are validated`() {
        val parsed = WorldOutlineParser.parse("# header\n\nL -18000,9000 18000,-9000\n")
        parsed.land.single().toList() shouldBe listOf(0f, 0f, 1f, 1f)
        shouldThrow<IllegalArgumentException> { WorldOutlineParser.parse("X 0,0 1,1\n") }
        shouldThrow<IllegalArgumentException> { WorldOutlineParser.parse("L 0,0\n") }
        shouldThrow<IllegalArgumentException> { WorldOutlineParser.parse("L a,b 1,1\n") }
        shouldThrow<IllegalArgumentException> { WorldOutlineParser.parse("L0,0 1,1\n") }
        shouldThrow<IllegalArgumentException> { WorldOutlineParser.parse("T Asia/Tehran 210 0,0\n") }
        shouldThrow<IllegalArgumentException> { WorldOutlineParser.parse("T - x 0,0 1\n") }
        shouldThrow<IllegalArgumentException> { WorldOutlineParser.parse("P big 0,0 1,1\n") }
        shouldThrow<IllegalArgumentException> { WorldOutlineParser.parse("T - 0 0,0 1\nZ 0 1 0,0 1,1\n") }
    }

    private companion object {
        /** 4πR² with the IUGG mean radius 6 371.0088 km, rounded. */
        const val EARTH_AREA_KM2 = 510_065_881L
    }
}
