/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import io.github.cosinekitty.astronomy.seasons
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.comparables.shouldBeLessThan
import ir.taqvim.core.testing.GoldenFile
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test

/**
 * Recomputes [PersianLeapTable] from March equinoxes (cosinekitty/astronomy, MIT, test scope only) with
 * [PersianYearStartRule]. On failure the clue prints the regenerated `LEAP_BITS` constant.
 */
class PersianCalendarAstronomyTest {
    private val iranTime = FixedOffsetTimeZone(PersianYearStartRule.TEHRAN_MERIDIAN_OFFSET)

    private fun marchEquinox(persianYear: Int): Instant {
        val ut = seasons(persianYear + GREGORIAN_YEAR_OFFSET).marchEquinox.ut
        return Instant.fromEpochMilliseconds(
            ((ut * SECONDS_PER_DAY + J2000_UNIX_SECONDS) * MILLIS_PER_SECOND).roundToLong(),
        )
    }

    private fun rows(name: String): List<List<String>> =
        GoldenFile
            .load("golden/persian/$name")
            .lines
            .drop(1)
            .map { it.split(',') }

    @Test
    fun `the generated table reproduces the year-start rule for every tabulated year`() {
        val first = PersianCalendarSystem.TABLE_FIRST_YEAR
        val last = PersianCalendarSystem.TABLE_LAST_YEAR
        val starts = (first..last + 1).associateWith { PersianYearStartRule.firstDayOfYear(marchEquinox(it)).value }
        val leapFlags = (first..last).map { starts.getValue(it + 1) - starts.getValue(it) == LEAP_YEAR_DAYS }

        withClue("regenerated LEAP_BITS = ${hex(leapFlags)}, FIRST_YEAR_START_JDN = ${starts.getValue(first)}") {
            (first..last + 1)
                .filter { starts.getValue(it) != PersianCalendarSystem.firstDayOfYear(it).value }
                .shouldBeEmpty()
        }
    }

    @Test
    fun `the official table's dates are the March equinox dates in Iran time`() {
        rows("official-leap-years-1206-1498.csv")
            .filter { (year, _, date) -> marchEquinox(year.toInt()).toLocalDateTime(iranTime).date.toString() != date }
            .shouldBeEmpty()
    }

    @Test
    fun `computed equinoxes are within a minute of the official Nowruz instants`() {
        rows("official-nowruz-instants.csv").forEach { (year, instant) ->
            (marchEquinox(year.toInt()) - Instant.parse(instant)).absoluteValue shouldBeLessThan 1.minutes
        }
    }

    private fun hex(leapFlags: List<Boolean>): String =
        leapFlags.chunked(BITS_PER_BYTE).joinToString("") { bits ->
            "%02x".format(bits.foldIndexed(0) { index, byte, isLeap -> if (isLeap) byte or (1 shl index) else byte })
        }

    private companion object {
        const val GREGORIAN_YEAR_OFFSET = 621
        const val SECONDS_PER_DAY = 86_400.0
        const val J2000_UNIX_SECONDS = 946_728_000.0
        const val MILLIS_PER_SECOND = 1_000.0
        const val LEAP_YEAR_DAYS = 366L
        const val BITS_PER_BYTE = 8
    }
}
