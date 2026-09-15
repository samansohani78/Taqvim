/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import ir.taqvim.core.testing.GoldenFile
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import org.junit.jupiter.api.Test

/** Columns of `golden/usno/moon-phases-1700-2100.csv`. */
private const val QUARTER = 1
private const val INSTANT = 2

/** A-13 Moon phases against every principal phase USNO lists for 1700–2100 (golden/usno). */
class UsnoMoonPhasesTest {
    private val usno =
        GoldenFile
            .load("golden/usno/moon-phases-1700-2100.csv")
            .lines
            .drop(1)
            .map { it.split(',') }
            .map { row -> MoonPhaseEvent(MoonQuarter.valueOf(row[QUARTER].uppercase()), Instant.parse(row[INSTANT])) }

    @Test
    fun `every principal Moon phase of 1700 to 2100 matches USNO within two minutes`() {
        val computed = Sky.moonQuarters(Instant.parse("1700-01-01T00:00:00Z"), Instant.parse("2101-01-01T00:00:00Z"))

        usno shouldHaveSize 19_839
        computed shouldHaveSize usno.size
        computed.map { it.quarter } shouldBe usno.map { it.quarter }
        // USNO rounds to the minute (up to 30 s); the rest is the two ephemerides' lunar theory and ΔT. Largest
        // deviation measured on 2026-09-15 per century: 1700s 59 s, 1800s 84 s, 1900s 81 s, 2000s 80 s — so one
        // two-minute bound holds for the whole range and no looser tier is needed for early years.
        computed
            .zip(usno)
            .filter { (ours, published) -> (ours.instant - published.instant).absoluteValue >= 2.minutes }
            .shouldBeEmpty()
    }
}
