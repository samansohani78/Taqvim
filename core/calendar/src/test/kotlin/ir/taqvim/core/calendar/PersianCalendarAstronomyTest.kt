/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import io.github.cosinekitty.astronomy.seasons
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import ir.taqvim.core.testing.GoldenFile
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test

/**
 * The run-time Persian year starts (A-02 on true March equinoxes, ADR-0026) against the table ADR-0008 shipped and
 * against the library's own season search (cosinekitty/astronomy, MIT). On failure the clue prints the computed bits.
 */
class PersianCalendarAstronomyTest {
    private val persian = PersianCalendarSystem
    private val first = PersianCalendarSystem.ASTRONOMICAL_FIRST_YEAR
    private val last = PersianCalendarSystem.ASTRONOMICAL_LAST_YEAR
    private val iranTime = FixedOffsetTimeZone(PersianYearStartRule.TEHRAN_MERIDIAN_OFFSET)

    private fun marchEquinox(persianYear: Int): Instant =
        CalendarAstronomy.instantOf(seasons(persianYear + GREGORIAN_YEAR_OFFSET).marchEquinox.ut)

    private fun rows(name: String): List<List<String>> =
        GoldenFile
            .load("golden/persian/$name")
            .lines
            .drop(1)
            .map { it.split(',') }

    @Test
    fun `computed year starts reproduce the ADR-0008 table for every astronomical year`() {
        val leapFlags = (first..last).map(persian::isLeapYear)

        withClue("computed LEAP_BITS = ${hex(leapFlags)}") {
            first shouldBe PersianLeapTableSnapshot.FIRST_YEAR
            last shouldBe PersianLeapTableSnapshot.LAST_YEAR
            persian.firstDayOfYear(first).value shouldBe PersianLeapTableSnapshot.FIRST_YEAR_START_JDN
            (first..last).filter { leapFlags[it - first] != PersianLeapTableSnapshot.isLeap(it) }.shouldBeEmpty()
        }
    }

    @Test
    fun `the run-time equinox search gives the same year starts as the library's season search`() {
        (first..last)
            .filter { PersianYearStartRule.firstDayOfYear(marchEquinox(it)) != persian.firstDayOfYear(it) }
            .shouldBeEmpty()
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

    @Test
    fun `the ephemeris finds no March equinox far outside its range`() {
        CalendarAstronomy.marchEquinox(FAR_GREGORIAN_YEAR) shouldBe null
    }

    private fun hex(leapFlags: List<Boolean>): String =
        leapFlags.chunked(BITS_PER_BYTE).joinToString("") { bits ->
            "%02x".format(bits.foldIndexed(0) { index, byte, isLeap -> if (isLeap) byte or (1 shl index) else byte })
        }

    private companion object {
        const val GREGORIAN_YEAR_OFFSET = 621
        const val BITS_PER_BYTE = 8
        const val FAR_GREGORIAN_YEAR = 20_621
    }
}
