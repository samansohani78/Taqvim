/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

/** T-1400 time-zone board: local times, UTC offsets, day shifts against the home zone and the zone search. */
class TimeZoneBoardTest {
    private val zones = listOf("Asia/Kabul", "America/Los_Angeles", "Asia/Tehran", "Not/AZone", "Pacific/Kiritimati")

    @Test
    fun `rows show local times, offsets and day shifts`() {
        // 2026-06-21T20:00Z: 23:30 in Tehran, 00:30 next day in Kabul, 13:00 (PDT) in Los Angeles and 10:00 next day
        // in Kiritimati.
        val board = TimeZoneBoardBuilder.build(ToolsFixtures.NOW, ToolsFixtures.settings("en"), zones, query = "")
        board.rows.map { it.id } shouldBe
            listOf("Asia/Tehran", "Asia/Kabul", "America/Los_Angeles", "Pacific/Kiritimati")
        board.rows.map { it.time } shouldBe listOf("23:30", "00:30", "13:00", "10:00")
        board.rows.map { it.offset } shouldBe listOf("+03:30", "+04:30", "-07:00", "+14:00")
        board.rows.map { it.dayShift } shouldBe listOf(0, 1, 0, 1)
        board.rows.map { it.isHome } shouldBe listOf(true, false, false, false)
        board.rows.map { it.city } shouldBe listOf("Tehran", "Kabul", "Los Angeles", "Kiritimati")
        board.rows[0].name shouldContain "Iran"
        board.suggestions shouldBe emptyList()
    }

    @Test
    fun `digits follow the language`() {
        val persian = ToolsFixtures.settings("fa")
        val row = TimeZoneBoardBuilder.build(ToolsFixtures.NOW, persian, emptyList(), "").rows.single()
        row.time shouldBe "۲۳:۳۰"
        row.offset shouldBe "+۰۳:۳۰"
        TimeZoneBoardBuilder.offset(-(9 * 3_600 + 1_800), ToolsFixtures.language("en")) shouldBe "-09:30"
        TimeZoneBoardBuilder.offset(0, ToolsFixtures.language("en")) shouldBe "+00:00"
    }

    @Test
    fun `the search suggests zones that are not on the board yet`() {
        TimeZoneBoardBuilder.suggestions("new york", emptyList()) shouldContain "America/New_York"
        TimeZoneBoardBuilder.suggestions("NEW_YORK", listOf("America/New_York")) shouldBe emptyList()
        TimeZoneBoardBuilder.suggestions(" ", emptyList()) shouldBe emptyList()
        TimeZoneBoardBuilder.suggestions("a", emptyList()) shouldHaveSize TimeZoneBoardBuilder.MAX_SUGGESTIONS
        TimeZoneBoardBuilder.cityOf("America/Argentina/Buenos_Aires") shouldBe "Buenos Aires"
        TimeZoneBoardBuilder.cityOf("UTC") shouldBe "UTC"
    }
}
