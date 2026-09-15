/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.testing.GoldenFile
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import org.junit.jupiter.api.Test

/** JDN of 1970-01-01, the epoch of [Instant]. */
private const val JDN_OF_UNIX_EPOCH = 2_440_588L

/** Official Iran time, UTC+03:30, in which the documents print their clock times. */
private val IRAN_OFFSET: Duration = 3.hours + 30.minutes

/**
 * A-13 eclipses against the Calendar Center's official calendars 1404 and 1405 and its 1404 lunar eclipse notice
 * (golden/iran). Lunar contacts are geocentric; "visible in Iran" is checked at the 31 provincial capitals whose
 * coordinates the Institute of Geophysics' 1405 prayer-time documents state.
 */
class OfficialIranEclipsesTest {
    private data class OfficialEclipse(
        val day: Jdn,
        val weekday: String,
        val body: String,
        val kind: EclipseKind,
        val visible: Boolean,
        val contacts: List<String>,
    )

    private val eclipses =
        GoldenFile.load("golden/iran/official-eclipses-1399-1405.csv").lines.drop(1).map { line ->
            val cells = line.split(',')
            val (year, month, day) = cells[0].split('-').map { it.toInt() }
            OfficialEclipse(
                day = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, year, month, day)),
                weekday = cells[1],
                body = cells[2],
                kind = EclipseKind.valueOf(cells[3].uppercase()),
                visible = cells[4].toBooleanStrict(),
                contacts = cells.subList(6, 10).filter { it.isNotEmpty() },
            )
        }

    private val capitals =
        GoldenFile.load("golden/iran/iran-provincial-capitals-1405.csv").lines.drop(1).map { line ->
            val cells = line.split(',')
            Coordinates(cells[1].toDouble(), cells[2].toDouble())
        }

    private fun startOfIranDay(day: Jdn): Instant =
        Instant.fromEpochSeconds((day.value - JDN_OF_UNIX_EPOCH) * 1.days.inWholeSeconds) - IRAN_OFFSET

    private fun iranDay(instant: Instant): Jdn =
        Jdn(Math.floorDiv((instant + IRAN_OFFSET).epochSeconds, 1.days.inWholeSeconds) + JDN_OF_UNIX_EPOCH)

    /** The eclipse of [official]'s body whose peak falls on its day in Iran time; exactly one must exist. */
    private fun lunarOn(official: OfficialEclipse): LunarEclipse {
        val start = startOfIranDay(official.day)
        val found = Eclipses.lunarEclipses(start - 1.days, start + 2.days).filter { iranDay(it.peak) == official.day }
        found shouldHaveSize 1
        return found.single()
    }

    private fun LunarEclipse.umbralContacts(): List<Instant> =
        listOf(
            peak - partialSemiDurationMinutes.minutes,
            peak - totalSemiDurationMinutes.minutes,
            peak + totalSemiDurationMinutes.minutes,
            peak + partialSemiDurationMinutes.minutes,
        )

    @Test
    fun `the golden holds every published eclipse on its stated weekday`() {
        eclipses shouldHaveSize 9
        eclipses.forEach { withClue(it) { it.day.weekday().name shouldBe it.weekday } }
        capitals shouldHaveSize 31
    }

    @Test
    fun `lunar eclipses have the published type and contact times within two minutes`() {
        eclipses.filter { it.body == "lunar" }.forEach { official ->
            withClue(official) {
                val eclipse = lunarOn(official)
                eclipse.kind shouldBe official.kind
                official.contacts.zip(eclipse.umbralContacts()).forEach { (clock, computed) ->
                    val (hour, minute) = clock.split(':').map { it.toLong() }
                    val published = startOfIranDay(official.day) + hour.hours + minute.minutes
                    // The documents print minutes; the measured deviations are at most 45 s.
                    (computed - published).absoluteValue shouldBeLessThan 2.minutes
                }
            }
        }
    }

    @Test
    fun `lunar eclipses are visible or not in Iran as published`() {
        eclipses.filter { it.body == "lunar" }.forEach { official ->
            val eclipse = lunarOn(official)
            // The statement concerns the umbral eclipse: the penumbral edges of the eclipses stated as not visible
            // rise above the horizon of some eastern and western capitals.
            val instants = eclipse.umbralContacts() + eclipse.peak
            capitals.forEach { capital ->
                instants.forEach { instant ->
                    withClue("$official at $capital, $instant") {
                        val altitude = Sky.skyPosition(CelestialBody.MOON, instant, capital).altitudeDegrees
                        if (official.visible) altitude shouldBeGreaterThan 0.0 else altitude shouldBeLessThan 0.0
                    }
                }
            }
        }
    }

    @Test
    fun `solar eclipses have the published type and are not seen from any provincial capital`() {
        eclipses.filter { it.body == "solar" }.forEach { official ->
            withClue(official) {
                official.visible shouldBe false
                val start = startOfIranDay(official.day)
                val found =
                    Eclipses.solarEclipses(start - 1.days, start + 2.days).filter {
                        iranDay(it.peak) ==
                            official.day
                    }
                found shouldHaveSize 1
                found.single().kind shouldBe official.kind
                capitals.forEach { capital ->
                    // The local search only returns eclipses seen from the place, so the next one must come later.
                    Eclipses.nextLocalSolarEclipse(start - 1.days, capital).partialBegin.instant shouldBeGreaterThan
                        start + 1.days
                }
            }
        }
    }
}
