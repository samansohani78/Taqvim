/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.events

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.NepaliLunarDays
import ir.taqvim.core.calendar.TithiObservance
import ir.taqvim.core.model.CalendarSystem
import java.time.LocalDate
import org.junit.jupiter.api.Test

/** ADR-0038: the `LunarTithi` rule and Nepali-only titles. */
class LunarTithiRuleTest {
    private val dashain = event("test.dashain", CalendarSystem.NEPALI, EventRule.LunarTithi(6, 7, endTithi = 12))
    private val calculator = OccurrenceCalculator(listOf(dashain))

    @Test
    fun `a lunar range yields every day of it in the Bikram Sambat year`() {
        val occurrences = calculator.occurrences(dashain, 2082)

        occurrences.map { it.jdn.toJavaDate() } shouldBe
            (0L..5L).map { LocalDate.of(2025, 9, 29).plusDays(it) }
        occurrences.map { it.date.month }.toSet() shouldBe setOf(6)
        occurrences.map { it.year }.toSet() shouldBe setOf(2082)
    }

    @Test
    fun `the rule follows the lunar calendar computation`() {
        val shivaratri = EventRule.LunarTithi(10, 29, TithiObservance.MIDNIGHT)
        val definition = event("test.shivaratri", CalendarSystem.NEPALI, shivaratri)

        OccurrenceCalculator(listOf(definition)).occurrences(definition, 2083).map { it.jdn } shouldBe
            NepaliLunarDays.days(2083, 10, 29, TithiObservance.MIDNIGHT)
        shivaratri.endTithi shouldBe null
        shivaratri.endOffsetDays shouldBe 0
    }

    @Test
    fun `lunar rules outside the Nepali calendar have no days`() {
        val gregorian = event("test.lunar-gregorian", CalendarSystem.GREGORIAN, EventRule.LunarTithi(1, 15))

        OccurrenceCalculator(listOf(gregorian)).occurrences(gregorian, 2026).shouldBeEmpty()
    }

    @Test
    fun `invalid lunar rules are rejected`() {
        shouldThrow<IllegalArgumentException> { EventRule.LunarTithi(0, 1) }
        shouldThrow<IllegalArgumentException> { EventRule.LunarTithi(1, 0) }
        shouldThrow<IllegalArgumentException> { EventRule.LunarTithi(1, 31) }
        shouldThrow<IllegalArgumentException> { EventRule.LunarTithi(1, 1, endTithi = 0) }
        shouldThrow<IllegalArgumentException> { EventRule.LunarTithi(1, 1, endTithi = 31) }
        shouldThrow<IllegalArgumentException> { EventRule.LunarTithi(1, 1, endOffsetDays = -1) }
    }

    @Test
    fun `a Nepali title stands in for Persian`() {
        val nepali = LocalizedText(mapOf(LocalizedText.NEPALI to "दशैं"))

        nepali.forLanguage("fa") shouldBe "दशैं"
        nepali.forLanguage("en") shouldBe "दशैं"
        LocalizedText(mapOf("fa" to "x", "ne" to "y")).forLanguage("en") shouldBe "x"
        shouldThrow<IllegalArgumentException> { LocalizedText(mapOf("ne" to " ")) }
    }
}
