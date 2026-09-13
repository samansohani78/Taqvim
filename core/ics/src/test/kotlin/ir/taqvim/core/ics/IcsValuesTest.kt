/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ics

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.testing.PropertyTesting
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.Test

class IcsValuesTest {
    private fun property(
        name: String = "DTSTART",
        parameters: Map<String, String> = emptyMap(),
    ) = ContentLine(3, name, parameters.mapValues { listOf(it.value) }, "")

    @Test
    fun `durations of section 3-3-6`() {
        mapOf(
            "PT0S" to Duration.ZERO,
            "P1W" to 7.days,
            "-P1DT2H3M4S" to -(1.days + 2.hours + 3.minutes + 4.seconds),
            "+PT15M" to 15.minutes,
            "P2D" to 2.days,
            "PT1H30M" to 90.minutes,
        ).forEach { (text, duration) -> IcsValues.duration(text) shouldBe duration }
        listOf("P", "PT", "1D", "P1H", "PT1D", "P99999999999999999999D", "P1W2D").forEach {
            IcsValues.duration(it).shouldBeNull()
        }
        mapOf(
            Duration.ZERO to "PT0S",
            14.days to "P2W",
            -1.days to "-P1D",
            90.minutes to "PT1H30M",
            1.days + 5.seconds to "P1DT5S",
        ).forEach { (duration, text) -> IcsValues.formatDuration(duration) shouldBe text }
    }

    @Test
    fun `formatted durations read back`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.long(-10_000_000L..10_000_000L)) { seconds ->
                IcsValues.duration(IcsValues.formatDuration(seconds.seconds)) shouldBe seconds.seconds
            }
        }

    @Test
    fun `date-time forms, invalid values and unknown zones`() {
        val warnings = mutableListOf<IcsProblem>()
        IcsValues.dateTime("20260321", property(parameters = mapOf("VALUE" to "DATE")), warnings) shouldBe
            IcsDateTime.Date(LocalDate(2026, 3, 21))
        IcsValues.dateTime("20261231T235960Z", property(), warnings) shouldBe
            IcsDateTime.Utc(Instant.parse("2026-12-31T23:59:59Z"))
        IcsValues.dateTime("20260321T090000", property(parameters = mapOf("TZID" to "Asia/Tehran")), warnings) shouldBe
            IcsDateTime.Zoned(LocalDateTime(2026, 3, 21, 9, 0), "Asia/Tehran")
        warnings.shouldBeEmpty()
        IcsValues.dateTime("20260321T090000", property(parameters = mapOf("TZID" to "Mars/Olympus")), warnings) shouldBe
            IcsDateTime.Floating(LocalDateTime(2026, 3, 21, 9, 0))
        warnings shouldHaveSize 1
        listOf("20261332", "2026-03-21", "20260101T250000", "20260231T000000").forEach {
            IcsValues.dateTime(it, property(), warnings).shouldBeNull()
        }
        IcsValues.dateTime("20260231", property(parameters = mapOf("VALUE" to "DATE")), warnings).shouldBeNull()
        IcsValues.untilValue("20261231") shouldBe IcsDateTime.Date(LocalDate(2026, 12, 31))
        IcsValues.untilValue("20261231T000000") shouldBe IcsDateTime.Floating(LocalDateTime(2026, 12, 31, 0, 0))
    }

    @Test
    fun `recurrence parts parse, format and reject invalid values`() {
        val warnings = mutableListOf<IcsProblem>()
        val text = "FREQ=monthly;INTERVAL=2;UNTIL=20261231T000000Z;BYDAY=MO,-1FR,+2SU;BYMONTHDAY=1,-1"
        val rule = IcsValues.recurrence(property("RRULE").copy(value = text), warnings)

        rule shouldBe
            Recurrence(
                Frequency.MONTHLY,
                interval = 2,
                until = IcsDateTime.Utc(Instant.parse("2026-12-31T00:00:00Z")),
                byDay =
                    listOf(
                        WeekdayNum(Weekday.MONDAY),
                        WeekdayNum(Weekday.FRIDAY, -1),
                        WeekdayNum(Weekday.SUNDAY, 2),
                    ),
                byMonthDay = listOf(1, -1),
            )
        IcsValues.format(Recurrence(Frequency.DAILY, count = 3)) shouldBe "FREQ=DAILY;COUNT=3"
        warnings.shouldBeEmpty()
        listOf(
            "INTERVAL=2",
            "FREQ=HOURLY",
            "FREQ=DAILY;COUNT=0",
            "FREQ=DAILY;INTERVAL=x",
            "FREQ=WEEKLY;BYDAY=XX",
            "FREQ=DAILY;UNTIL=never",
            "FREQ=DAILY;COUNT=2;UNTIL=20261231",
            "FREQ=MONTHLY;BYMONTHDAY=0",
        ).forEach { IcsValues.recurrence(property("RRULE").copy(value = it), warnings).shouldBeNull() }
    }

    @Test
    fun `model invariants`() {
        shouldThrow<IllegalArgumentException> { WeekdayNum(Weekday.MONDAY, 0) }
        shouldThrow<IllegalArgumentException> { WeekdayNum(Weekday.MONDAY, 54) }
        shouldThrow<IllegalArgumentException> { Recurrence(Frequency.DAILY, interval = 0) }
        shouldThrow<IllegalArgumentException> {
            Recurrence(Frequency.DAILY, until = IcsDateTime.Zoned(LocalDateTime(2026, 1, 1, 0, 0), "UTC"))
        }
        shouldThrow<IllegalArgumentException> { Recurrence(Frequency.DAILY, byMonthDay = listOf(32)) }
    }
}
