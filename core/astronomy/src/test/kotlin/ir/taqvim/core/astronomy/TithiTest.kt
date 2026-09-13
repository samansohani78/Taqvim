/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import org.junit.jupiter.api.Test

/** T-406 tithi: structural checks; a published panchang golden is pending (DATA_TODO). */
class TithiTest {
    private val quarters =
        Sky.moonQuarters(Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-03-15T00:00:00Z"))

    @Test
    fun `elongation boundaries`() {
        Tithi.ofElongation(0.0) shouldBe TithiPosition(1, Paksha.SHUKLA)
        Tithi.ofElongation(11.999) shouldBe TithiPosition(1, Paksha.SHUKLA)
        Tithi.ofElongation(12.0) shouldBe TithiPosition(2, Paksha.SHUKLA)
        Tithi.ofElongation(179.999) shouldBe TithiPosition(15, Paksha.SHUKLA)
        Tithi.ofElongation(180.0) shouldBe TithiPosition(16, Paksha.KRISHNA)
        Tithi.ofElongation(359.999) shouldBe TithiPosition(30, Paksha.KRISHNA)
        Tithi.ofElongation(-0.001) shouldBe TithiPosition(30, Paksha.KRISHNA)
        Tithi.ofElongation(720.0).number shouldBe 1
        TithiPosition(16, Paksha.KRISHNA).numberInPaksha shouldBe 1
        TithiPosition(30, Paksha.KRISHNA).numberInPaksha shouldBe 15
    }

    @Test
    fun `new moon ends tithi 30 and full moon ends tithi 15`() {
        val newMoon = quarters.first { it.quarter == MoonQuarter.NEW_MOON }.instant
        val fullMoon = quarters.first { it.quarter == MoonQuarter.FULL_MOON }.instant

        Tithi.at(newMoon - 10.minutes).number shouldBe 30
        Tithi.at(newMoon + 10.minutes).number shouldBe 1
        Tithi.at(fullMoon - 10.minutes).number shouldBe 15
        Tithi.at(fullMoon + 10.minutes).number shouldBe 16
    }

    @Test
    fun `a synodic month has thirty tithi changes`() {
        val newMoons = quarters.filter { it.quarter == MoonQuarter.NEW_MOON }.map { it.instant }
        val changes = Tithi.changes(newMoons[0] + 1.minutes, newMoons[1] + 1.minutes)

        changes shouldHaveSize 30
        changes.zipWithNext().forEach { (earlier, later) -> (earlier < later) shouldBe true }
        changes.forEach { Tithi.at(it - 1.minutes).number shouldNotBe Tithi.at(it + 1.minutes).number }
        shouldThrow<IllegalArgumentException> { Tithi.changes(newMoons[1], newMoons[0]) }
    }
}
