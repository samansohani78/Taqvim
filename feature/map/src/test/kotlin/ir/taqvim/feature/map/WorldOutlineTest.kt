/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.map

import io.kotest.assertions.throwables.shouldThrow
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
    fun `the line assets hold the time-zone and plate boundaries on the map`() {
        val outline = WorldOutlineParser.parse(listOf(text, timeZones, plates))
        outline.land.size shouldBe 128
        outline.timeZones.size shouldBe 922
        outline.plates.size shouldBe 1_246
        (outline.timeZones + outline.plates).all { part -> part.size >= 4 && part.all { it in 0f..1f } } shouldBe true
    }

    @Test
    fun `outline lines are validated`() {
        val parsed = WorldOutlineParser.parse("# header\n\nL -18000,9000 18000,-9000\n")
        parsed.land.single().toList() shouldBe listOf(0f, 0f, 1f, 1f)
        val lines = WorldOutlineParser.parse(listOf("Z -18000,9000 18000,-9000\n", "P 0,0 18000,0\n"))
        lines.timeZones.single().toList() shouldBe listOf(0f, 0f, 1f, 1f)
        lines.plates.single().toList() shouldBe listOf(0.5f, 0.5f, 1f, 0.5f)
        shouldThrow<IllegalArgumentException> { WorldOutlineParser.parse("X 0,0 1,1\n") }
        shouldThrow<IllegalArgumentException> { WorldOutlineParser.parse("L 0,0\n") }
        shouldThrow<IllegalArgumentException> { WorldOutlineParser.parse("L a,b 1,1\n") }
        shouldThrow<IllegalArgumentException> { WorldOutlineParser.parse("L0,0 1,1\n") }
    }
}
