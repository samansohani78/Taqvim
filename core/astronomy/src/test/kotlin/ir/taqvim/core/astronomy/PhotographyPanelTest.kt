/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeBetween
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.longs.shouldBeInRange
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.Coordinates
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import org.junit.jupiter.api.Test

/**
 * T-407: golden/blue hour bands and Moon rise/set. The blue hour's lower edge is checked against the USNO in
 * `UsnoTwilightTest`; the golden hour's two edges have no published definition anywhere and are this app's own
 * convention (DT-017, ADR-0049), so the cases here pin the convention rather than an external time.
 */
class PhotographyPanelTest {
    private val tehran = Coordinates(35.70, 51.42)
    private val tehranMidnight = Instant.parse("2026-09-12T20:30:00Z")

    private fun sunAltitude(
        place: Coordinates,
        instant: Instant,
    ) = Sky.skyPosition(CelestialBody.SUN, instant, place).altitudeDegrees

    @Test
    fun `an ordinary day has a blue and a golden hour in the morning and in the evening`() {
        val day = PhotographyPanel.day(tehran, tehranMidnight)

        withClue(day.toString()) {
            day.blueHours shouldHaveSize 2
            day.goldenHours shouldHaveSize 2
        }
        val (morningBlue, eveningBlue) = day.blueHours
        val (morningGolden, eveningGolden) = day.goldenHours
        (morningBlue.end - morningGolden.start).absoluteValue.inWholeSeconds shouldBeLessThanOrEqual 20L
        (eveningGolden.end - eveningBlue.start).absoluteValue.inWholeSeconds shouldBeLessThanOrEqual 20L
        morningGolden.end shouldBeLessThan eveningGolden.start
        day.goldenHours.forEach { interval ->
            sunAltitude(tehran, interval.start + (interval.end - interval.start) / 2) shouldBe (1.0 plusOrMinus 5.0)
        }
        day.blueHours.forEach { interval ->
            sunAltitude(tehran, interval.start + (interval.end - interval.start) / 2) shouldBe (-5.0 plusOrMinus 1.0)
            // The Sun sinks at most 15° per hour, so 2° of blue hour takes at least 8 minutes.
            (interval.end - interval.start).inWholeMinutes shouldBeInRange 5L..40L
        }
        listOfNotNull(day.moonrise, day.moonset).forEach { it.shouldBeBetween(tehranMidnight, tehranMidnight + 1.days) }
    }

    /**
     * ADR-0049: the golden hour's edges are this app's convention, so nothing external can check them — this pins
     * the convention itself, the way `UsnoTwilightTest` pins the blue hour's sourced edge. The two bands must meet
     * at one apparent altitude and tile −6°‥+6° with no overlap and no gap.
     */
    @Test
    fun `the golden hour's edges are the convention, and the two bands tile without a gap`() {
        val day = PhotographyPanel.day(tehran, tehranMidnight)
        val (morningGolden, eveningGolden) = day.goldenHours
        val (morningBlue, eveningBlue) = day.blueHours

        // The expected altitudes are written as literals, not as the constants, so that moving an edge fails here
        // instead of silently redefining what this app calls a golden hour.
        val edges =
            mapOf(
                "morning start" to (morningGolden.start to -4.0),
                "morning end" to (morningGolden.end to 6.0),
                "evening start" to (eveningGolden.start to 6.0),
                "evening end" to (eveningGolden.end to -4.0),
            )
        edges.forEach { (name, edge) ->
            val (instant, expected) = edge
            val altitudes = Sky.altitudes(CelestialBody.SUN, instant, tehran)
            withClue("golden hour $name at $instant: $altitudes") {
                // Apparent, not geometric: only BLUE_HOUR_BOTTOM follows a published geometric definition (ADR-0047),
                // so the geometric altitude at these edges is measurably lower and must stay that way.
                altitudes.apparentDegrees shouldBe (expected plusOrMinus 0.05)
                altitudes.geometricDegrees shouldBeLessThan expected - 0.05
            }
        }
        withClue("the bands must meet at BLUE_HOUR_TOP, not overlap or leave a gap") {
            (morningGolden.start - morningBlue.end).absoluteValue shouldBeLessThan 20.seconds
            (eveningGolden.end - eveningBlue.start).absoluteValue shouldBeLessThan 20.seconds
        }
    }

    @Test
    fun `no golden or blue hour in a polar night or a polar day`() {
        val svalbard = Coordinates(78.2, 15.6)

        val polarNight = PhotographyPanel.day(svalbard, Instant.parse("2026-12-21T00:00:00Z"))
        polarNight.goldenHours.shouldBeEmpty()
        polarNight.blueHours.shouldBeEmpty()
        val polarDay = PhotographyPanel.day(svalbard, Instant.parse("2026-06-21T00:00:00Z"))
        polarDay.goldenHours.shouldBeEmpty()
        polarDay.blueHours.shouldBeEmpty()
    }

    @Test
    fun `interval search clips to its window and rejects an empty window`() {
        val start = Instant.parse("2026-01-01T00:00:00Z")
        IntervalSearch.intervals(start, start + 30.minutes, 5.minutes, 1.minutes) { true } shouldBe
            listOf(TimeInterval(start, start + 30.minutes))
        shouldThrow<IllegalArgumentException> { IntervalSearch.intervals(start, start, 5.minutes, 1.minutes) { true } }
        PhotographyPanel.GOLDEN_HOUR_TOP shouldBe 6.0
    }
}
