/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import ir.taqvim.core.testing.GoldenFile
import java.time.LocalDate
import kotlin.time.Instant
import org.junit.jupiter.api.Test

private const val SECONDS_PER_DAY = 86_400L

/** Columns of golden/usno/solar-eclipses-1800-2050.csv. */
private const val DATE = 0
private const val TYPE = 1

/**
 * A-13 solar eclipses against every USNO solar eclipse of 1800–2050 (golden/usno), including the non-central total and
 * annular eclipses whose shadow axis misses Earth.
 */
class UsnoSolarEclipsesTest {
    private data class UsnoEclipse(
        val date: LocalDate,
        val type: String,
    )

    private val usno: List<UsnoEclipse> =
        GoldenFile
            .load("golden/usno/solar-eclipses-1800-2050.csv")
            .lines
            .drop(1)
            .map { it.split(',') }
            .map { row -> UsnoEclipse(LocalDate.parse(row[DATE]), row[TYPE]) }

    // USNO's "Annular-Total" (hybrid) eclipses are total at their greatest eclipse.
    private fun kindOf(type: String): EclipseKind =
        when (type) {
            "Total", "Annular-Total" -> EclipseKind.TOTAL
            "Annular" -> EclipseKind.ANNULAR
            else -> EclipseKind.PARTIAL
        }

    private fun matches(
        eclipse: GlobalSolarEclipse,
        row: UsnoEclipse,
    ): Boolean {
        val date = LocalDate.ofEpochDay(Math.floorDiv(eclipse.peak.epochSeconds, SECONDS_PER_DAY))
        return date == row.date && eclipse.kind == kindOf(row.type)
    }

    @Test
    fun `every solar eclipse of 1800 to 2050 matches USNO in order, UT date and type`() {
        val computed =
            Eclipses.solarEclipses(Instant.parse("1800-01-01T00:00:00Z"), Instant.parse("2051-01-01T00:00:00Z"))

        usno shouldHaveSize 582
        computed shouldHaveSize usno.size
        computed
            .zip(usno)
            .filterNot { (eclipse, row) -> matches(eclipse, row) }
            .map { (eclipse, row) -> "${row.date} ${row.type}: computed ${eclipse.kind} peak ${eclipse.peak}" }
            .shouldBe(emptyList())
    }
}
