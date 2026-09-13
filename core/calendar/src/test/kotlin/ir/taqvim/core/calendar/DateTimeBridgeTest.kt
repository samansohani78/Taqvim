/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.comparables.shouldBeBetween
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.testing.FakeClock
import ir.taqvim.core.testing.FakeTimeZone
import ir.taqvim.core.testing.PropertyTesting
import ir.taqvim.core.testing.TimeZones
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

class DateTimeBridgeTest {
    private fun day(
        instant: String,
        zone: TimeZone,
    ): LocalDate = Instant.parse(instant).toJdn(zone).toLocalDate()

    @Test
    fun `local dates map to julian day numbers`() {
        LocalDate(1970, 1, 1).toJdn() shouldBe Jdn(JDN_OF_UNIX_EPOCH)
        LocalDate(2000, 1, 1).toJdn() shouldBe Jdn(2_451_545)
        Jdn(2_451_545).toLocalDate() shouldBe LocalDate(2000, 1, 1)
        Jdn(0).toLocalDate() shouldBe LocalDate(-4713, 11, 24)
    }

    @Test
    fun `days outside the kotlinx range are rejected`() {
        shouldThrow<IllegalArgumentException> { Jdn(1_000_000_000_000_000L).toLocalDate() }
    }

    @Test
    fun `Tehran day boundaries before and after DST was abolished`() {
        // 2021: Iran still observed DST (UTC+4:30 in June).
        day("2021-06-01T19:29:59Z", TimeZones.TEHRAN) shouldBe LocalDate(2021, 6, 1)
        day("2021-06-01T19:30:00Z", TimeZones.TEHRAN) shouldBe LocalDate(2021, 6, 2)
        // 2023: DST abolished in 2022, UTC+3:30 all year.
        day("2023-06-01T20:29:59Z", TimeZones.TEHRAN) shouldBe LocalDate(2023, 6, 1)
        day("2023-06-01T20:30:00Z", TimeZones.TEHRAN) shouldBe LocalDate(2023, 6, 2)
    }

    @Test
    fun `Kabul has a fixed offset`() {
        day("2026-01-15T19:29:59Z", TimeZones.KABUL) shouldBe LocalDate(2026, 1, 15)
        day("2026-06-15T19:30:00Z", TimeZones.KABUL) shouldBe LocalDate(2026, 6, 16)
    }

    @Test
    fun `Berlin and Los Angeles follow their DST rules`() {
        day("2026-01-15T22:59:59Z", TimeZones.BERLIN) shouldBe LocalDate(2026, 1, 15)
        day("2026-01-15T23:00:00Z", TimeZones.BERLIN) shouldBe LocalDate(2026, 1, 16)
        // Summer time began at 01:00 UTC on 2026-03-29; midnight is now 22:00 UTC.
        day("2026-03-29T21:59:59Z", TimeZones.BERLIN) shouldBe LocalDate(2026, 3, 29)
        day("2026-03-29T22:00:00Z", TimeZones.BERLIN) shouldBe LocalDate(2026, 3, 30)
        day("2026-01-15T07:59:59Z", TimeZones.LOS_ANGELES) shouldBe LocalDate(2026, 1, 14)
        day("2026-07-15T07:00:00Z", TimeZones.LOS_ANGELES) shouldBe LocalDate(2026, 7, 15)
    }

    @Test
    fun `today follows the clock and the current zone`() {
        val clock = FakeClock(Instant.parse("2026-03-20T20:00:00Z"))
        val zone = FakeTimeZone(TimeZones.TEHRAN)
        val provider = ClockTodayProvider(clock) { zone.current }

        provider.today() shouldBe LocalDate(2026, 3, 20).toJdn()
        zone.set(TimeZones.KATHMANDU)
        provider.today() shouldBe LocalDate(2026, 3, 21).toJdn()
        zone.set(TimeZones.TEHRAN)
        clock.advanceBy(kotlin.time.Duration.parse("30m"))
        provider.today() shouldBe LocalDate(2026, 3, 21).toJdn()
    }

    @Test
    fun `local date bridge agrees with the Gregorian calendar and round-trips`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.long(-100_000_000L..100_000_000L)) { value ->
                val jdn = Jdn(JDN_OF_UNIX_EPOCH + value)
                val local = jdn.toLocalDate()
                local.toJdn() shouldBe jdn
                val gregorian = GregorianCalendarSystem.fromJdn(jdn)
                listOf(local.year, local.month.ordinal + 1, local.day) shouldBe
                    listOf(gregorian.year, gregorian.month, gregorian.day)
            }
        }

    @Test
    fun `a zone never shifts the civil day by more than one from UTC`(): Unit =
        runBlocking {
            val zones =
                Arb.element(
                    TimeZones.TEHRAN,
                    TimeZones.KABUL,
                    TimeZones.KATHMANDU,
                    TimeZones.BERLIN,
                    TimeZones.LOS_ANGELES,
                )
            val millis = Arb.long(-2_000_000_000_000L..4_000_000_000_000L)
            checkAll(PropertyTesting.iterations, millis, zones) { epochMillis, zone ->
                val instant = Instant.fromEpochMilliseconds(epochMillis)
                (instant.toJdn(zone) - instant.toJdn(TimeZones.UTC)).shouldBeBetween(-1L, 1L)
            }
        }
}
