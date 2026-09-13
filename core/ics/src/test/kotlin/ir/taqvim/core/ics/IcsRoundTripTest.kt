/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ics

import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.property.Arb
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
            )
        }

    @Test
    fun `write then read returns the same calendar`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, text, Arb.list(event, 0..4)) { productId, events ->
                val calendar = IcsCalendar(productId, events)
                val written = IcsWriter.write(calendar, stamp)

                IcsReader.read(written) shouldBe IcsParseResult.Success(calendar, emptyList())
                written shouldEndWith "END:VCALENDAR\r\n"
                written.split("\r\n").forEach { it.toByteArray(Charsets.UTF_8).size shouldBeLessThanOrEqual 75 }
            }
        }

    private companion object {
        val ZONES = listOf("Asia/Tehran", "Asia/Kabul", "Europe/Berlin", "America/Los_Angeles", "UTC")
        val ORDINALS = listOf(null, 1, 2, -1, 53)
    }
}
