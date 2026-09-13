/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.testing.GoldenFile
import kotlin.math.abs
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import org.junit.jupiter.api.Test

/** Columns of `golden/yallop/yallop-1997-table4.csv`. */
private const val ARCL = 6
private const val ARCV = 7
private const val PARALLAX = 11
private const val WIDTH = 12
private const val Q = 13
private const val GROUP = 15

/** A-06 against Yallop's own Table 4 and real evenings around a new moon. */
class YallopTest {
    private val rows =
        GoldenFile
            .load("golden/yallop/yallop-1997-table4.csv")
            .lines
            .drop(1)
            .map { it.split(',') }

    @Test
    fun `q reproduces the 295 observations of Table 4`() {
        rows shouldHaveSize 295
        val mismatches =
            rows.filter { row ->
                abs(Yallop.q(row[ARCV].toDouble(), row[WIDTH].toDouble()) - row[Q].toDouble()) > ROUNDING_TOLERANCE
            }
        withClue(mismatches.take(5).joinToString("\n")) { mismatches.shouldBeEmpty() }
    }

    @Test
    fun `classes follow the table's groups away from the boundaries`() {
        val boundaries = listOf(0.216, -0.014, -0.160, -0.232, -0.293)
        val clear = rows.filter { row -> boundaries.none { abs(row[Q].toDouble() - it) <= ROUNDING_TOLERANCE } }

        clear.filter { Yallop.classify(it[Q].toDouble()).name != it[GROUP] }.shouldBeEmpty()
        Yallop.classify(0.3) shouldBe CrescentVisibilityClass.A
        Yallop.classify(-0.014) shouldBe CrescentVisibilityClass.C
        Yallop.classify(-1.0) shouldBe CrescentVisibilityClass.F
    }

    @Test
    fun `geocentric crescent width matches the table where the Moon is near the horizon`() {
        rows
            .filter { row ->
                val width = Yallop.topocentricWidthArcMinutes(row[PARALLAX].toDouble(), 0.0, row[ARCL].toDouble())
                abs(width - row[WIDTH].toDouble()) > WIDTH_TOLERANCE
            }.shouldBeEmpty()
        Yallop.topocentricWidthArcMinutes(60.0, 10.0, 12.0) shouldBeGreaterThan
            Yallop.topocentricWidthArcMinutes(60.0, 0.0, 12.0)
    }

    @Test
    fun `the crescent is invisible on the evening of a new moon and easy two evenings later`() {
        val tehran = Coordinates(35.70, 51.42)
        val newMoon =
            Sky
                .moonQuarters(Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-03-01T00:00:00Z"))
                .first { it.quarter == MoonQuarter.NEW_MOON }
                .instant

        val sameEvening = Yallop.evening(tehran, newMoon - 12.hours)
        (sameEvening == null || sameEvening.visibility == CrescentVisibilityClass.F) shouldBe true
        val later = Yallop.evening(tehran, newMoon + 2.days).shouldNotBeNull()
        later.visibility shouldBeLessThanOrEqualTo CrescentVisibilityClass.B
        later.lagMinutes shouldBeGreaterThan 60.0
        later.arcLightDegrees shouldBeGreaterThan 15.0
    }

    @Test
    fun `no evening without a sunset`() {
        Yallop.evening(Coordinates(80.0, 15.0), Instant.parse("2026-06-21T00:00:00Z")).shouldBeNull()
    }

    private companion object {
        /** ARCV and W′ are printed to 0.1; their rounding moves q by up to about 0.03. */
        const val ROUNDING_TOLERANCE = 0.035

        /** W′ printed to 0.1′ (0.01′ below 1′) and the ignored sin h sin π term. */
        const val WIDTH_TOLERANCE = 0.08
    }
}
