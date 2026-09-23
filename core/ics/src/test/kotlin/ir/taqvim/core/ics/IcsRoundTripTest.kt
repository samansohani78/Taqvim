/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ics

import io.kotest.common.ExperimentalKotest
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.testing.PropertyTesting
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.Test

/** Property: any calendar the model can express is written and read back unchanged, without warnings. */
class IcsRoundTripTest {
    /**
     * Fixed seed: the same inputs, and so the same covered branches, on every run and machine. The opt-in is for
     * `iterations`, which Kotest 6 still marks experimental.
     */
    @OptIn(ExperimentalKotest::class)
    private val propertyConfig = PropTestConfig(seed = 20_260_920L, iterations = PropertyTesting.iterations)

    private val stamp = Instant.parse("2026-09-13T12:00:00Z")

    private val text =
        Arb.string(
            0..90,
            Arb.element(
                listOf('a', 'Z', '7', ' ', ';', ',', ':', '"', '\\', '\n').map { Codepoint(it.code) } +
                    listOf(0x0634, 0x06CC, 0x1F600).map(::Codepoint),
            ),
        )

    private val localDateTime =
        arbitrary {
            LocalDateTime(
                Arb.int(1970..2100).bind(),
                Arb.int(1..12).bind(),
                Arb.int(1..28).bind(),
                Arb.int(0..23).bind(),
                Arb.int(0..59).bind(),
                Arb.int(0..59).bind(),
            )
        }

    private val instant = Arb.long(0L..4_000_000_000L).map { Instant.fromEpochSeconds(it) }

    private val dateTime: Arb<IcsDateTime> =
        arbitrary {
            when (Arb.int(0..3).bind()) {
                0 -> {
                    IcsDateTime.Date(
                        LocalDate(Arb.int(1970..2100).bind(), Arb.int(1..12).bind(), Arb.int(1..28).bind()),
                    )
                }

                1 -> {
                    IcsDateTime.Floating(localDateTime.bind())
                }

                2 -> {
                    IcsDateTime.Utc(instant.bind())
                }

                else -> {
                    IcsDateTime.Zoned(localDateTime.bind(), Arb.element(ZONES).bind())
                }
            }
        }

    private val recurrence: Arb<Recurrence> =
        arbitrary {
            val until = if (Arb.boolean().bind()) dateTime.bind().takeIf { it !is IcsDateTime.Zoned } else null
            Recurrence(
                frequency = Arb.enum<Frequency>().bind(),
                interval = Arb.int(1..5).bind(),
                count = if (until == null) Arb.int(1..20).orNull().bind() else null,
                until = until,
                byDay =
                    Arb
                        .list(arbitrary { WeekdayNum(Arb.enum<Weekday>().bind(), Arb.element(ORDINALS).bind()) }, 0..3)
                        .bind(),
                byMonthDay = Arb.list(Arb.element((1..31) + (-31..-1)), 0..3).bind(),
            )
        }

    private val alarm: Arb<DisplayAlarm> =
        arbitrary {
            val trigger =
                if (Arb.boolean().bind()) {
                    AlarmTrigger.Relative(Arb.long(-864_000L..864_000L).bind().seconds, Arb.boolean().bind())
                } else {
                    AlarmTrigger.Absolute(instant.bind())
                }
            DisplayAlarm(trigger, text.bind())
        }

    private val event: Arb<IcsEvent> =
        arbitrary {
            IcsEvent(
                uid = "uid-" + Arb.long(0L..Long.MAX_VALUE).bind() + "@taqvim.test",
                start = dateTime.bind(),
                end = dateTime.orNull().bind(),
                summary = text.orNull().bind(),
                description = text.orNull().bind(),
                recurrence = recurrence.orNull().bind(),
                exceptionDates = Arb.list(dateTime, 0..3).bind(),
                alarms = Arb.list(alarm, 0..2).bind(),
                recurrenceId = dateTime.orNull().bind(),
                cancelled = Arb.boolean().bind(),
            )
        }

    @Test
    fun `write then read returns the same calendar`(): Unit =
        runBlocking {
            checkAll(propertyConfig, text, Arb.list(event, 0..4)) { productId, events ->
                val calendar = IcsCalendar(productId, events)
                val written = IcsWriter.write(calendar, stamp)

                IcsReader.read(written) shouldBe IcsParseResult.Success(calendar, emptyList())
                written shouldEndWith "END:VCALENDAR\r\n"
                written.split("\r\n").forEach { it.toByteArray(Charsets.UTF_8).size shouldBeLessThanOrEqual 75 }
            }
        }

    private fun assertRoundTrips(calendar: IcsCalendar) {
        val written = IcsWriter.write(calendar, stamp)
        IcsReader.read(written) shouldBe IcsParseResult.Success(calendar, emptyList())
        written shouldEndWith "END:VCALENDAR\r\n"
        written.split("\r\n").forEach { it.toByteArray(Charsets.UTF_8).size shouldBeLessThanOrEqual 75 }
    }

    @Test
    fun `an empty calendar and one with the longest event list round-trip`() {
        // A fixed seed permanently commits Arb.list(event, 0..4) to one length per draw; pin both list-length edges
        // (an empty calendar and a full 4-event one) so they are never left untested purely by chance.
        assertRoundTrips(IcsCalendar("", emptyList()))
        assertRoundTrips(IcsCalendar("empty-product-id", List(4) { minimalEvent(it) }))
    }

    @Test
    fun `an event with every optional field and every list at its longest still round-trips`() {
        // A fixed seed permanently commits every list-valued Arb (exceptionDates, alarms, byDay, byMonthDay) to one
        // length per draw; pin all of them at their maximum together, and every optional field present at once, so
        // this densest shape is never left untested purely by chance.
        val recurrence =
            Recurrence(
                frequency = Frequency.MONTHLY,
                interval = 5,
                until = IcsDateTime.Utc(Instant.fromEpochSeconds(4_000_000_000L)),
                byDay =
                    listOf(WeekdayNum(Weekday.MONDAY, 1), WeekdayNum(Weekday.TUESDAY, -1), WeekdayNum(Weekday.SUNDAY)),
                byMonthDay = listOf(1, -1, 15),
            )
        val maximal =
            IcsEvent(
                uid = "uid-${Long.MAX_VALUE}@taqvim.test",
                start = IcsDateTime.Zoned(LocalDateTime(2026, 12, 31, 23, 59, 59), ZONES.last()),
                end = IcsDateTime.Zoned(LocalDateTime(2027, 1, 1, 0, 59, 59), ZONES.last()),
                summary = "a".repeat(90),
                description = "شی".repeat(23).take(90),
                recurrence = recurrence,
                exceptionDates =
                    List(3) { IcsDateTime.Date(LocalDate(1970 + it, 1 + it, 1 + it)) },
                alarms =
                    listOf(
                        DisplayAlarm(AlarmTrigger.Relative((-864_000L).seconds, true), "a".repeat(90)),
                        DisplayAlarm(AlarmTrigger.Absolute(Instant.fromEpochSeconds(4_000_000_000L)), ""),
                    ),
                recurrenceId = IcsDateTime.Date(LocalDate(2026, 1, 1)),
                cancelled = true,
            )
        assertRoundTrips(IcsCalendar("dense-product-id", listOf(maximal)))
    }

    private fun minimalEvent(index: Int): IcsEvent =
        IcsEvent(uid = "uid-$index@taqvim.test", start = IcsDateTime.Date(LocalDate(2026, 1, 1 + index)))

    private companion object {
        val ZONES = listOf("Asia/Tehran", "Asia/Kabul", "Europe/Berlin", "America/Los_Angeles", "UTC")
        val ORDINALS = listOf(null, 1, 2, -1, 53)
    }
}
