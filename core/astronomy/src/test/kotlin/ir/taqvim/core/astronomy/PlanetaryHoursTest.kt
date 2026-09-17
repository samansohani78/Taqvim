/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.Weekday
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import org.junit.jupiter.api.Test

class PlanetaryHoursTest {
    @Test
    fun `every weekday starts with its planet and the Chaldean sequence runs into the next day`() {
        Weekday.entries.forEach { weekday ->
            val rulers = PlanetaryHours.rulers(weekday)
            rulers shouldHaveSize 24
            rulers.first() shouldBe PlanetaryHours.rulerOfDay(weekday)
            rulers.zipWithNext().forEach { (earlier, later) ->
                Math.floorMod(later.ordinal - earlier.ordinal, 7) shouldBe 1
            }
            ClassicalPlanet.entries[(rulers.last().ordinal + 1) % 7] shouldBe PlanetaryHours.rulerOfDay(weekday + 1)
        }
    }

    @Test
    fun `day and night hours are twelve equal parts each`() {
        val sunrise = Instant.parse("2026-06-21T05:00:00Z")
        val sunset = Instant.parse("2026-06-21T20:00:00Z")
        val nextSunrise = Instant.parse("2026-06-22T05:00:00Z")

        val hours = PlanetaryHours.hours(Weekday.SUNDAY, sunrise, sunset, nextSunrise)

        hours shouldHaveSize 24
        hours.first().start shouldBe sunrise
        hours[11].end shouldBe sunset
        hours.last().end shouldBe nextSunrise
        hours.zipWithNext().forEach { (earlier, later) -> later.start shouldBe earlier.end }
        (hours[0].end - hours[0].start) shouldBe 75.minutes
        (hours[12].end - hours[12].start) shouldBe 45.minutes
        hours.count { it.daytime } shouldBe 12
        hours.map { it.number } shouldBe (1..24).toList()
        hours.first().ruler shouldBe ClassicalPlanet.SUN
        shouldThrow<IllegalArgumentException> { PlanetaryHours.hours(Weekday.SUNDAY, sunset, sunrise, nextSunrise) }
    }

    @Test
    fun `hours for a real place, and none in a polar night`() {
        val tehran =
            PlanetaryHours.forDay(Coordinates(35.70, 51.42), Instant.parse("2026-09-12T20:30:00Z"), Weekday.SUNDAY)
        tehran.shouldBeInstanceOf<PlanetaryHoursResult.Available>()
        tehran.hours shouldHaveSize 24
        tehran.hours.zipWithNext().forEach { (earlier, later) -> later.start shouldBe earlier.end }

        PlanetaryHours.forDay(Coordinates(78.2, 15.6), Instant.parse("2026-12-21T00:00:00Z"), Weekday.MONDAY) shouldBe
            PlanetaryHoursResult.Unavailable
    }

    @Test
    fun `no hours where the Sun only grazes the horizon at the edge of a polar day or night`() {
        val tromso = Coordinates(69.6492, 18.9553)
        // 15 May 2000: the set search from the sunrise finds the evening set, and the rise search from that set finds
        // the same instant again (the start of the midnight Sun). 15 January 2001: the set search from the sunrise
        // returns the sunrise itself (the end of the polar night).
        PlanetaryHours.forDay(tromso, Instant.parse("2000-05-14T22:00:00Z"), Weekday.SUNDAY) shouldBe
            PlanetaryHoursResult.Unavailable
        PlanetaryHours.forDay(tromso, Instant.parse("2001-01-14T23:00:00Z"), Weekday.MONDAY) shouldBe
            PlanetaryHoursResult.Unavailable
        PlanetaryHours
            .forDay(tromso, Instant.parse("2001-03-20T23:00:00Z"), Weekday.WEDNESDAY)
            .shouldBeInstanceOf<PlanetaryHoursResult.Available>()
    }
}
