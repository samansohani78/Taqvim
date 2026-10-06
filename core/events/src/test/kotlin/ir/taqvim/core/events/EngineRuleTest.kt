/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.events

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.ChristianMovableFeasts
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.JewishObservance
import ir.taqvim.core.calendar.JewishObservances
import ir.taqvim.core.calendar.MovableFeast
import ir.taqvim.core.model.CalendarSystem
import org.junit.jupiter.api.Test

/** T-108/T-109: the two rules that hand the question to a calendar engine instead of restating a date. */
class EngineRuleTest {
    @Test
    fun `a Jewish observance is the day its engine gives for that Hebrew year`() {
        val pesach =
            event("test.pesach", CalendarSystem.HEBREW, EventRule.JewishObservanceDate(JewishObservance.PESACH))

        val days = OccurrenceCalculator(listOf(pesach)).occurrences(pesach, HEBREW_LEAP_YEAR).map { it.jdn }

        days shouldContainExactly listOf(JewishObservances.day(JewishObservance.PESACH, HEBREW_LEAP_YEAR).jdn)
        // A leap year inserts Adar I, so Nisan is month 8 rather than 7: a Fixed rule would land in the wrong month.
        days.map { GregorianCalendarSystem.fromJdn(it) }.size shouldBe 1
    }

    @Test
    fun `a Western feast follows the computus of the calendar in force`() {
        val easter =
            event(
                "test.easter",
                CalendarSystem.GREGORIAN,
                EventRule.ChristianFeastDate(MovableFeast.EASTER, ChristianRite.WESTERN),
            )

        OccurrenceCalculator(listOf(easter)).occurrences(easter, 2026).map { it.jdn } shouldContainExactly
            listOfNotNull(ChristianMovableFeasts.civilForYear(2026L)[MovableFeast.EASTER])
    }

    @Test
    fun `the Orthodox rite is the Julian computus, and has no Advent to give`() {
        val orthodoxEaster =
            event(
                "test.orthodox-easter",
                CalendarSystem.GREGORIAN,
                EventRule.ChristianFeastDate(MovableFeast.EASTER, ChristianRite.ORTHODOX),
            )
        val advent =
            event(
                "test.orthodox-advent",
                CalendarSystem.GREGORIAN,
                EventRule.ChristianFeastDate(MovableFeast.FIRST_SUNDAY_OF_ADVENT, ChristianRite.ORTHODOX),
            )

        val easterDays = OccurrenceCalculator(listOf(orthodoxEaster)).occurrences(orthodoxEaster, 2026).map { it.jdn }
        easterDays shouldContainExactly listOfNotNull(ChristianMovableFeasts.orthodoxForYear(2026)[MovableFeast.EASTER])
        // The two rites disagree in 2026, which is the whole reason the rule carries one.
        easterDays shouldBe listOfNotNull(ChristianMovableFeasts.orthodoxForYear(2026)[MovableFeast.EASTER])
        (easterDays != listOfNotNull(ChristianMovableFeasts.civilForYear(2026L)[MovableFeast.EASTER])) shouldBe true

        // The Orthodox churches keep a Nativity Fast, not a four-Sunday Advent, so the engine offers no date and the
        // rule yields nothing rather than inventing one.
        OccurrenceCalculator(listOf(advent)).occurrences(advent, 2026).shouldBeEmpty()
    }

    @Test
    fun `a scope rides along with the definition and changes no day`() {
        val label = LocalizedText(mapOf(LocalizedText.PERSIAN to "تراي"))
        val scope = EventScope(EventScopeLevel.REGION, label, listOf("np.region.terai"))
        val scoped = event("test.scoped", CalendarSystem.GREGORIAN, EventRule.Fixed(3, 21)).copy(scope = scope)
        val plain = scoped.copy(id = EventId("test.plain"), scope = null)

        val calculator = OccurrenceCalculator(listOf(scoped, plain))

        calculator.occurrences(scoped, 2026).map { it.jdn } shouldBe calculator.occurrences(plain, 2026).map { it.jdn }
        scoped.scope?.level shouldBe EventScopeLevel.REGION
        scoped.scope?.areas shouldContainExactly listOf("np.region.terai")
        EventScopeLevel.entries.size shouldBe 5
    }

    private companion object {
        /** 5787 is a Hebrew leap year, where Adar I shifts Nisan and Sivan by a month. */
        const val HEBREW_LEAP_YEAR = 5787
    }
}
