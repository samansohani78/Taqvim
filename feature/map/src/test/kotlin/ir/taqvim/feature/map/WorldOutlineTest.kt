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

/** T-1301 asset: the Natural Earth outline matches its generator's recorded hash and parses completely. */
class WorldOutlineTest {
    private val text = File("src/main/assets/map/world-110m.txt").readText()

    @Test
    fun `the asset body matches the hash its generator recorded`() {
        text shouldContain "# source-sha256-land: 9e0729ee253ca7d7a5c4ae9395fb1902264c5377c52e224d13dd85010e2835d9"
        text shouldContain "# license: public domain"
        WorldOutlineParser.bodyHashMatches(text) shouldBe true
        WorldOutlineParser.bodyHashMatches(text + "L 0,0 100,100\n") shouldBe false
        WorldOutlineParser.bodyHashMatches("L 0,0 100,100\n") shouldBe false
    }

    @Test
    fun `the asset holds Natural Earth's land rings and boundary lines on the map`() {
        val outline = WorldOutlineParser.parse(text)
        outline.land.size shouldBe 128
        outline.borders.size shouldBe 333
        (outline.land + outline.borders).all { part -> part.size >= 4 && part.all { it in 0f..1f } } shouldBe true
    }

    @Test
    fun `outline lines are validated`() {
        val parsed = WorldOutlineParser.parse("# header\n\nL -18000,9000 18000,-9000\n")
        parsed.land.single().toList() shouldBe listOf(0f, 0f, 1f, 1f)
        shouldThrow<IllegalArgumentException> { WorldOutlineParser.parse("X 0,0 1,1\n") }
        shouldThrow<IllegalArgumentException> { WorldOutlineParser.parse("L 0,0\n") }
        shouldThrow<IllegalArgumentException> { WorldOutlineParser.parse("L a,b 1,1\n") }
        shouldThrow<IllegalArgumentException> { WorldOutlineParser.parse("L0,0 1,1\n") }
    }
}
