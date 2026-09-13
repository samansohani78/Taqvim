/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.testing.GoldenFile
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import org.junit.jupiter.api.Test

/** Columns of the NASA catalog fixtures. */
private const val DATE = 1
private const val TD = 2
private const val DELTA_T = 3
private const val TYPE = 4

/** Index of catalog row 09571 (2029-01-14, partial) among the 2024–2030 solar eclipses. */
private const val PARTIAL_2029_ROW = 10

/** A-13 eclipses against NASA GSFC's Five Millennium Catalogs, 2024–2030 (golden/nasa). */
class EclipsesTest {
    private val from = Instant.parse("2024-01-01T00:00:00Z")
    private val until = Instant.parse("2031-01-01T00:00:00Z")

    private data class CatalogEclipse(
        val peakUt: Instant,
        val typeCode: String,
    )

    private fun catalog(name: String): List<CatalogEclipse> =
        GoldenFile
            .load("golden/nasa/$name")
            .lines
            .drop(1)
            .map { it.split(',') }
            .map { row ->
                CatalogEclipse(Instant.parse("${row[DATE]}T${row[TD]}Z") - row[DELTA_T].toLong().seconds, row[TYPE])
            }

    private fun solarKind(code: String): EclipseKind =
        when (code.first()) {
            'T' -> EclipseKind.TOTAL
            'A' -> EclipseKind.ANNULAR
            else -> EclipseKind.PARTIAL
        }

    private fun lunarKind(code: String): EclipseKind =
        when (code.first()) {
            'T' -> EclipseKind.TOTAL
            'P' -> EclipseKind.PARTIAL
            else -> EclipseKind.PENUMBRAL
        }

    @Test
    fun `solar eclipses 2024 to 2030 match the NASA catalog`() {
        val expected = catalog("solar-eclipses-2024-2030.csv")
        val computed = Eclipses.solarEclipses(from, until)

        computed shouldHaveSize expected.size
        computed.zip(expected).forEach { (eclipse, row) ->
            withClue("${row.peakUt} ${row.typeCode}") {
                eclipse.kind shouldBe solarKind(row.typeCode)
                (eclipse.peak - row.peakUt).absoluteValue shouldBeLessThan 5.minutes
            }
        }
    }

    @Test
    fun `lunar eclipses 2024 to 2030 match the NASA catalog`() {
        val expected = catalog("lunar-eclipses-2024-2030.csv")
        val computed = Eclipses.lunarEclipses(from, until)

        computed shouldHaveSize expected.size
        computed.zip(expected).forEach { (eclipse, row) ->
            withClue("${row.peakUt} ${row.typeCode}") {
                eclipse.kind shouldBe lunarKind(row.typeCode)
                (eclipse.peak - row.peakUt).absoluteValue shouldBeLessThan 5.minutes
                eclipse.penumbralSemiDurationMinutes shouldBeGreaterThan 0.0
            }
        }
    }

    @Test
    fun `the 2024 total eclipse is total near the catalog's point of greatest eclipse`() {
        // Catalog row 09561: greatest eclipse at 25N 104W (rounded to whole degrees).
        val local = Eclipses.nextLocalSolarEclipse(Instant.parse("2024-04-01T00:00:00Z"), Coordinates(25.0, -104.0))
        val catalogPeak = catalog("solar-eclipses-2024-2030.csv").first().peakUt

        (local.peak.instant - catalogPeak).absoluteValue shouldBeLessThan 10.minutes
        local.obscuration shouldBeGreaterThan 0.95
        local.partialBegin.instant shouldBeLessThan local.peak.instant
        local.peak.instant shouldBeLessThan local.partialEnd.instant
        local.peak.sunAltitudeDegrees shouldBeGreaterThan 0.0
        if (local.kind == EclipseKind.TOTAL) local.totalBegin.shouldNotBeNull()
    }

    @Test
    fun `a globally partial eclipse is partial locally, without total contacts`() {
        // Catalog row 09571: 2029-01-14 is a partial eclipse, greatest at 64N 114W.
        val local = Eclipses.nextLocalSolarEclipse(Instant.parse("2029-01-01T00:00:00Z"), Coordinates(64.0, -114.0))

        local.kind shouldBe EclipseKind.PARTIAL
        local.totalBegin.shouldBeNull()
        local.totalEnd.shouldBeNull()
        (
            local.peak.instant -
                catalog(
                    "solar-eclipses-2024-2030.csv",
                )[PARTIAL_2029_ROW].peakUt
        ).absoluteValue shouldBeLessThan
            30.minutes
    }
}
