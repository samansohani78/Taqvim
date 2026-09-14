/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.automation

import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test

/** T-1103: the day-change alarm is set for the start of the next local day, also around clock changes. */
class DayChangeTimeTest {
    @Test
    fun `the next midnight in Tehran and on a day whose midnight is skipped`() {
        val tehran = TimeZone.of("Asia/Tehran")
        nextLocalMidnight(LocalDateTime.parse("2026-09-14T23:59:59").toInstant(tehran), tehran) shouldBe
            Instant.parse("2026-09-14T20:30:00Z")
        // São Paulo moved its clocks from 00:00 to 01:00 on 4 November 2018: that day began at 01:00 (UTC−2).
        val saoPaulo = TimeZone.of("America/Sao_Paulo")
        nextLocalMidnight(Instant.parse("2018-11-03T15:00:00Z"), saoPaulo) shouldBe
            Instant.parse("2018-11-04T03:00:00Z")
    }

    @Test
    fun `the alarm is always within a day and starts the following local date`(): Unit =
        runBlocking {
            val zones = listOf("Asia/Tehran", "Europe/Berlin", "America/Sao_Paulo", "Pacific/Apia", "Asia/Kabul")
            checkAll(1_000, Arb.long(FROM..UNTIL), Arb.element(zones)) { millis, id ->
                val zone = TimeZone.of(id)
                val now = Instant.fromEpochMilliseconds(millis)
                val next = nextLocalMidnight(now, zone)

                (next > now) shouldBe true
                (next - now <= 25.hours) shouldBe true
                next.toLocalDateTime(zone).date shouldBe now.toLocalDateTime(zone).date.plus(1, DateTimeUnit.DAY)
            }
        }

    private companion object {
        val FROM = Instant.parse("2010-01-01T00:00:00Z").toEpochMilliseconds()
        val UNTIL = Instant.parse("2035-01-01T00:00:00Z").toEpochMilliseconds()
    }
}
