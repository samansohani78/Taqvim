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
import kotlin.time.Instant
import org.junit.jupiter.api.Test

/**
 * T-407: golden/blue hour bands and Moon rise/set; a published photographers' ephemeris golden is pending (DATA_TODO).
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
