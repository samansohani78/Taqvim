/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events

import io.kotest.common.ExperimentalKotest
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.of
import io.kotest.property.checkAll
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.JdnRange
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/** B03 (U/P): which civil days an occurrence covers, in the event's own zone and the display zone. */
class EventDaysTest {
    /**
     * Fixed seed: the same inputs, and so the same covered branches, on every run and machine. The opt-in is for
     * `iterations`, which Kotest 6 still marks experimental.
     */
    @OptIn(ExperimentalKotest::class)
    private val propertyConfig = PropTestConfig(seed = 20_260_920L, iterations = PropertyTesting.iterations)

    private val day = LocalDate(2026, 3, 21).toJdn()
    private val tehran = TimeZone.of("Asia/Tehran")

    @Test
    fun `all-day occurrences keep their own dates in every zone`() {
        val occurrence = occurrence(days = day..day + 1, startMinute = null, endMinute = null)

        EventDays.of(occurrence, tehran) shouldBe (day..day + 1)
        EventDays.of(occurrence, TimeZone.of("America/Los_Angeles")) shouldBe (day..day + 1)
        EventDays.of(occurrence, TimeZone.UTC) shouldBe (day..day + 1)
    }

    @Test
    fun `timed occurrences move with the offset between the two zones`() {
        // 00:30 in Tokyo is the previous day in Tehran and in UTC.
        val tokyo = occurrence(startMinute = 30, endMinute = 90, zoneId = "Asia/Tokyo")
        EventDays.of(tokyo, tehran) shouldBe (day - 1..day - 1)
        EventDays.of(tokyo, TimeZone.of("Asia/Tokyo")) shouldBe (day..day)

        // 23:00 in Los Angeles is the next day in Tehran.
        val losAngeles = occurrence(startMinute = 23 * 60, endMinute = 23 * 60 + 60, zoneId = "America/Los_Angeles")
        EventDays.of(losAngeles, tehran) shouldBe (day + 1..day + 1)

        // Ending exactly at midnight stays on the day it started.
        val untilMidnight = occurrence(startMinute = 22 * 60, endMinute = 24 * 60, zoneId = tehran.id)
        EventDays.of(untilMidnight, tehran) shouldBe (day..day)

        // A zero-length occurrence keeps its day.
        val instant = occurrence(startMinute = 9 * 60, endMinute = 9 * 60, zoneId = tehran.id)
        EventDays.of(instant, tehran) shouldBe (day..day)
    }

    @Test
    fun `a wall-clock time inside a daylight-saving gap keeps one day`() {
        // Europe/Berlin skips 02:00–03:00 on 2026-03-29; the time resolves forward, still on that day.
        val berlinDay = LocalDate(2026, 3, 29).toJdn()
        val gap = occurrence(days = berlinDay..berlinDay, startMinute = 150, endMinute = 210, zoneId = "Europe/Berlin")

        EventDays.of(gap, TimeZone.of("Europe/Berlin")) shouldBe (berlinDay..berlinDay)
    }

    @Test
    fun `every timed occurrence covers one or two days near its own date`(): Unit =
        runBlocking {
            val zones = Arb.of("Asia/Tokyo", "America/Los_Angeles", "UTC", "Asia/Tehran", "Pacific/Kiritimati")
            checkAll(propertyConfig, Arb.int(0..1439), zones, zones) { minute, eventZone, displayZone ->
                val occurrence = occurrence(startMinute = minute, endMinute = minute + 60, zoneId = eventZone)

                val days = EventDays.of(occurrence, TimeZone.of(displayZone))

                // An hour-long meeting covers one day, or two when it crosses midnight of the display zone.
                (days.endInclusive - days.start) shouldBeIn listOf(0L, 1L)
                // A zone offset can move it by at most a day from the date the user typed.
                (days.start >= day - 1) shouldBe true
                (days.endInclusive <= day + 1) shouldBe true
            }
        }

    @Test
    fun `midnight and one-minute-to-midnight still cover one or two days at the widest zone offsets`() {
        // A fixed seed permanently commits checkAll to one set of draws; pin minute 0 and minute 1439 (the
        // Arb.int(0..1439) boundaries) against the widest zone-offset pairs so the day-boundary crossing this
        // property exists to check is never left untested purely by chance.
        val zonePairs =
            listOf(
                "Asia/Tokyo" to "Asia/Tokyo",
                "Pacific/Kiritimati" to "America/Los_Angeles",
                "America/Los_Angeles" to "Pacific/Kiritimati",
                "UTC" to "UTC",
            )
        listOf(0, 1439).forEach { minute ->
            zonePairs.forEach { (eventZone, displayZone) ->
                val occurrence = occurrence(startMinute = minute, endMinute = minute + 60, zoneId = eventZone)
                val days = EventDays.of(occurrence, TimeZone.of(displayZone))
                (days.endInclusive - days.start) shouldBeIn listOf(0L, 1L)
                (days.start >= day - 1) shouldBe true
                (days.endInclusive <= day + 1) shouldBe true
            }
        }
    }

    private fun occurrence(
        days: JdnRange = day..day,
        startMinute: Int? = 600,
        endMinute: Int? = 660,
        zoneId: String = "Asia/Tehran",
    ) = PersonalOccurrence(
        eventId = 1,
        title = "meeting",
        notes = "",
        calendarSystem = CalendarSystem.PERSIAN,
        days = days,
        startMinute = startMinute,
        endMinute = endMinute,
        timeZoneId = zoneId,
        colorArgb = null,
        recurring = false,
    )
}
