/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.Coordinates
import kotlin.time.Instant
import org.junit.jupiter.api.Test

/** The shared ephemeris behind the computed calendars (ADR-0026, ADR-0027, ADR-0028). */
class CalendarAstronomyTest {
    private val astronomy = CalendarAstronomy
    private val tehran = Coordinates(35.70, 51.42)

    @Test
    fun `UT days and epoch milliseconds convert like the library`() {
        val millis = Instant.parse("2026-03-20T14:46:00Z").toEpochMilliseconds()
        astronomy.epochMillisOf(astronomy.utOfEpochMillis(millis)) shouldBe millis
        astronomy.instantOf(astronomy.utOfEpochMillis(millis)).toEpochMilliseconds() shouldBe millis
    }

    @Test
    fun `events are found in their windows and absent outside them`() {
        val march19 = astronomy.utOfEpochMillis(Instant.parse("2026-03-18T00:00:00Z").toEpochMilliseconds())
        astronomy.newMoonAfter(march19, 3.0).shouldNotBeNull()
        astronomy.newMoonAfter(march19 + 5.0, 3.0).shouldBeNull()
        val midsummer = astronomy.utOfEpochMillis(Instant.parse("2026-06-21T00:00:00Z").toEpochMilliseconds())
        astronomy.sunsetAfter(Coordinates(89.0, 0.0), midsummer, 1.0).shouldBeNull()
        val sunset = astronomy.sunsetAfter(tehran, midsummer, 1.0).shouldNotBeNull()
        astronomy.moonsetAfter(tehran, sunset, 1.0).shouldNotBeNull()
        astronomy.moonriseAfter(tehran, sunset, 1.0).shouldNotBeNull()
    }

    @Test
    fun `crescent geometry is physical`() {
        val evening = astronomy.utOfEpochMillis(Instant.parse("2026-03-20T15:00:00Z").toEpochMilliseconds())
        val sky = astronomy.crescentGeometry(tehran, evening)
        sky.moonDistanceKm shouldBeGreaterThan 356_000.0
        sky.moonDistanceKm shouldBeLessThan 407_000.0
        sky.arcOfLight shouldBeGreaterThan 0.0
        sky.arcOfLight shouldBeLessThan 180.0
        sky.sunAltitude shouldBeGreaterThan -90.0
        sky.moonAltitude shouldBeLessThan 90.0
    }
}
