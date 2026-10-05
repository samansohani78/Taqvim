/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import org.junit.jupiter.api.Test

/** DT-015: Abdollahy's rule for the Iranian twelve-animal year, and how it differs from the Chinese cycle. */
class PersianAnimalYearTest {
    @Test
    fun `the years in force match the animals Iran actually used`() {
        // The check that made this rule trustworthy: the animals of the Solar Hijri years currently in use.
        PersianAnimalYear.of(1403) shouldBe ChineseZodiacAnimal.DRAGON
        PersianAnimalYear.of(1404) shouldBe ChineseZodiacAnimal.SNAKE
        PersianAnimalYear.of(1405) shouldBe ChineseZodiacAnimal.HORSE
        PersianAnimalYear.of(1406) shouldBe ChineseZodiacAnimal.GOAT
    }

    @Test
    fun `the rule is Abdollahy's, applied literally`() {
        // "add 6 … divide by 12; the remainder … 1 = mouse" — remainder 1 must be the rat, the first of the cycle.
        val remainderOne = (1 until 12 * 4).first { (it + 6) % 12 == 1 }
        PersianAnimalYear.of(remainderOne) shouldBe ChineseZodiacAnimal.RAT
        // And the cycle is twelve years long in both directions, including before the era's year 1.
        PersianAnimalYear.of(1405 + 12) shouldBe PersianAnimalYear.of(1405)
        PersianAnimalYear.of(1405 - 12) shouldBe PersianAnimalYear.of(1405)
        PersianAnimalYear.of(-7) shouldBe PersianAnimalYear.of(5)
    }

    @Test
    fun `the difference from the Chinese cycle is when the year turns, not which animal`() {
        // Worth stating precisely, because it is easy to get backwards. The Persian cycle was borrowed from the
        // Turco-Mongol one, so the animal *sequence* is the same and a Solar Hijri year carries the same animal as
        // the Gregorian year it mostly covers. What differs is the turn: Nowruz in late March against the Chinese
        // new year in late January or February.
        PersianAnimalYear.of(1405) shouldBe AnimalYear.ofChineseYearStartingIn(2026)
        PersianAnimalYear.of(1400) shouldBe AnimalYear.ofChineseYearStartingIn(2021)

        // So the two disagree for any date between the Chinese new year and Nowruz. On 1 March 2026 the Chinese
        // year is already the horse, while Solar Hijri 1404 runs until Nowruz and is still the snake.
        val firstOfMarch2026 = CalendarDate(CalendarSystem.GREGORIAN, 2026, 3, 1)
        AnimalYear.forDate(firstOfMarch2026) shouldBe ChineseZodiacAnimal.HORSE
        PersianAnimalYear.of(1404) shouldBe ChineseZodiacAnimal.SNAKE
    }

    @Test
    fun `the period the naming was official is recorded, not assumed`() {
        PersianAnimalYear.IN_FORCE_YEARS shouldBe 1329..1344
    }
}
