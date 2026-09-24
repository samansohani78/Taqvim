/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.events

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class OccurrenceCalculatorTest {
    private val base = event("test.base", CalendarSystem.GREGORIAN, EventRule.Fixed(3, 1), isHoliday = true)

    @Test
    fun `relative events chain and carry their own calendar, year and holiday flag`() {
        val next = event("test.next", CalendarSystem.GREGORIAN, EventRule.RelativeToEvent(base.id, 1))
        val nextButOne = event("test.next-but-one", CalendarSystem.PERSIAN, EventRule.RelativeToEvent(next.id, 1))
        val calculator = OccurrenceCalculator(listOf(nextButOne, next, base))

        val occurrence = calculator.occurrences(nextButOne, 1404).single()

        occurrence.jdn shouldBe GregorianCalendarSystem.toJdn(CalendarDate(CalendarSystem.GREGORIAN, 2026, 3, 3))
        occurrence.date shouldBe CalendarDate(CalendarSystem.PERSIAN, 1404, 12, 12)
        occurrence.year shouldBe 1404
        occurrence.isHoliday shouldBe false
        calculator.occurrences(base, 2026).single().isHoliday shouldBe true
    }

    @Test
    fun `cycles, self references, unknown targets and duplicate ids are rejected`() {
        val a = event("test.a", CalendarSystem.GREGORIAN, EventRule.RelativeToEvent(EventId("test.b"), 1))
        val b = event("test.b", CalendarSystem.GREGORIAN, EventRule.RelativeToEvent(EventId("test.a"), 1))
        val self = event("test.self", CalendarSystem.GREGORIAN, EventRule.RelativeToEvent(EventId("test.self"), 1))
        val dangling =
            event("test.dangling", CalendarSystem.GREGORIAN, EventRule.RelativeToEvent(EventId("test.none"), 1))

        shouldThrow<IllegalArgumentException> { OccurrenceCalculator(listOf(a, b)) }.message shouldContain "cycle"
        shouldThrow<IllegalArgumentException> { OccurrenceCalculator(listOf(self)) }.message shouldContain "cycle"
        shouldThrow<IllegalArgumentException> { OccurrenceCalculator(listOf(dangling)) }.message shouldContain "unknown"
        shouldThrow<IllegalArgumentException> { OccurrenceCalculator(listOf(base, base)) }
        shouldThrow<IllegalArgumentException> { OccurrenceCalculator(listOf(base)).occurrences(dangling, 2026) }
    }

    @Test
    fun `unavailable calendars and missing astronomy are handled explicitly`() {
        val nepali = event("test.nepali", CalendarSystem.NEPALI, EventRule.Fixed(1, 1))
        val equinox =
            event("test.equinox", CalendarSystem.GREGORIAN, EventRule.Astronomical(AstroKind.MARCH_EQUINOX, 0, "UTC"))
        val relativeToNepali =
            event("test.after-nepali", CalendarSystem.GREGORIAN, EventRule.RelativeToEvent(nepali.id, 1))

        // Bikram Sambat is computed by default (T-105): Baisakh 1, 2082 is 14 April 2025.
        OccurrenceCalculator(listOf(nepali)).occurrences(nepali, 2082).single().jdn shouldBe
            GregorianCalendarSystem.toJdn(CalendarDate(CalendarSystem.GREGORIAN, 2025, 4, 14))
        val withoutNepali =
            CalendarProvider { system ->
                CalendarProvider.DEFAULT.calendarFor(system).takeIf {
                    system !=
                        CalendarSystem.NEPALI
                }
            }
        val calculator = OccurrenceCalculator(listOf(nepali, relativeToNepali), withoutNepali)
        calculator.occurrences(nepali, 2082).shouldBeEmpty()
        calculator.occurrences(relativeToNepali, 2026).shouldBeEmpty()
        shouldThrow<IllegalStateException> { OccurrenceCalculator(listOf(equinox)).occurrences(equinox, 2026) }
    }

    @Test
    fun `model values validate their invariants`() {
        shouldThrow<IllegalArgumentException> { EventRule.Fixed(13, 1) }
        shouldThrow<IllegalArgumentException> { EventRule.Fixed(1, 0) }
        shouldThrow<IllegalArgumentException> { EventRule.NthWeekdayOfMonth(1, Weekday.MONDAY, 6) }
        shouldThrow<IllegalArgumentException> { EventRule.LastDayOfMonth(0) }
        shouldThrow<IllegalArgumentException> { EventRule.Single(1405, 1, 0) }
        shouldThrow<IllegalArgumentException> { EventRule.NthDayOfYear(0) }
        shouldThrow<IllegalArgumentException> { EventRule.Astronomical(AstroKind.FULL_MOON, 0, "Mars/Olympus") }
        shouldThrow<IllegalArgumentException> { EventRule.Week(EventRule.Fixed(10, 4), 1) }
        shouldThrow<IllegalArgumentException> { EventRule.Week(EventRule.Fixed(10, 4), 15) }
        shouldThrow<IllegalArgumentException> {
            EventRule.Week(EventRule.RelativeToEvent(EventId("test.x"), 1), 7)
        }
        shouldThrow<IllegalArgumentException> {
            EventRule.Week(EventRule.Astronomical(AstroKind.FULL_MOON, 0, "UTC"), 7)
        }
        shouldThrow<IllegalArgumentException> { EventId(" ") }
        shouldThrow<IllegalArgumentException> { LocalizedText(mapOf("en" to "Only English")) }
        shouldThrow<IllegalArgumentException> { LocalizedText(mapOf("fa" to "x", "en" to " ")) }
        val citation = Citation("https://example.org", "example")
        shouldThrow<IllegalArgumentException> { Validity(CalendarSystem.PERSIAN, null, null, citation) }
        shouldThrow<IllegalArgumentException> { Validity(CalendarSystem.PERSIAN, 1405, 1400, citation) }

        val validity = Validity(CalendarSystem.PERSIAN, 1400, null, citation)
        validity.contains(1399) shouldBe false
        validity.contains(1500) shouldBe true
        Validity(CalendarSystem.PERSIAN, null, 1400, citation).contains(1401) shouldBe false
        LocalizedText(mapOf("fa" to "x", "en" to "y")).forLanguage("de") shouldBe "x"
        LocalizedText(mapOf("fa" to "x", "en" to "y")).forLanguage("en") shouldBe "y"
    }

    @Test
    fun `occurrences are sorted, unique and dated in the rule's calendar`(): Unit =
        runBlocking {
            val rules =
                Arb.choice(
                    Arb.bind(Arb.int(1..12), Arb.int(1..31)) { month, day -> EventRule.Fixed(month, day) },
                    Arb.bind(
                        Arb.int(1..12),
                        Arb.enum<Weekday>(),
                        Arb.int(1..5),
                    ) { m, w, n -> EventRule.NthWeekdayOfMonth(m, w, n) },
                    Arb.bind(
                        Arb.int(1..12),
                        Arb.enum<Weekday>(),
                        Arb.int(-40..40),
                    ) { m, w, o -> EventRule.LastWeekdayOfMonth(m, w, o) },
                    Arb.int(1..12).map { EventRule.LastDayOfMonth(it) },
                    Arb.int(1..366).map { EventRule.NthDayOfYear(it) },
                    Arb.bind(
                        Arb.int(1..12),
                        Arb.int(1..28),
                        Arb.int(2..14),
                    ) { m, d, len -> EventRule.Week(EventRule.Fixed(m, d), len) },
                )
            val systems = Arb.choice(Arb.enum<CalendarSystem>())
            checkAll(PropertyTesting.iterations, rules, systems, Arb.int(1300..1500)) { rule, system, rawYear ->
                val definition = event("test.random", system, rule)
                val year = if (system == CalendarSystem.GREGORIAN) rawYear + 621 else rawYear
                val occurrences = OccurrenceCalculator(listOf(definition)).occurrences(definition, year)
                val days = occurrences.map { it.jdn }
                days shouldBe days.distinct().sorted()
                occurrences.forEach { CalendarProvider.DEFAULT.calendarFor(system)?.fromJdn(it.jdn) shouldBe it.date }
            }
        }
}
