/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/** DT-015: Abdollahy's rule for the Iranian twelve-animal year, and the twelve names Dehkhoda gives it. */
class PersianAnimalYearTest {
    @Test
    fun `the years in force match the animals Iran actually used`() {
        // The check that made this rule trustworthy: the animals of the Solar Hijri years currently in use.
        PersianAnimalYear.of(1403) shouldBe PersianZodiacAnimal.WHALE
        PersianAnimalYear.of(1404) shouldBe PersianZodiacAnimal.SNAKE
        PersianAnimalYear.of(1405) shouldBe PersianZodiacAnimal.HORSE
        PersianAnimalYear.of(1406) shouldBe PersianZodiacAnimal.SHEEP
    }

    @Test
    fun `the rule is Abdollahy's, applied literally`() {
        // "add 6 … divide by 12; the remainder … 1 = mouse" — remainder 1 must be the mouse, the first of the cycle.
        val remainderOne = (1 until 12 * 4).first { (it + 6) % 12 == 1 }
        PersianAnimalYear.of(remainderOne) shouldBe PersianZodiacAnimal.MOUSE
        // And the cycle is twelve years long in both directions, including before the era's year 1.
        PersianAnimalYear.of(1405 + 12) shouldBe PersianAnimalYear.of(1405)
        PersianAnimalYear.of(1405 - 12) shouldBe PersianAnimalYear.of(1405)
        PersianAnimalYear.of(-7) shouldBe PersianAnimalYear.of(5)
    }

    @Test
    fun `the cycle is Dehkhoda's twelve, not the Chinese twelve`() {
        // Dehkhoda's ordered list (entry سال, from the Dīvān Lughāt al-Turk) and the Niṣāb al-Ṣibyān verse agree:
        // leopard not tiger, whale not dragon, sheep not goat, hen not rooster. Getting these wrong would show a
        // Persian user the wrong animal for their own year, which is the whole point of the cycle.
        PersianZodiacAnimal.entries shouldBe
            listOf(
                PersianZodiacAnimal.MOUSE,
                PersianZodiacAnimal.OX,
                PersianZodiacAnimal.LEOPARD,
                PersianZodiacAnimal.RABBIT,
                PersianZodiacAnimal.WHALE,
                PersianZodiacAnimal.SNAKE,
                PersianZodiacAnimal.HORSE,
                PersianZodiacAnimal.SHEEP,
                PersianZodiacAnimal.MONKEY,
                PersianZodiacAnimal.HEN,
                PersianZodiacAnimal.DOG,
                PersianZodiacAnimal.PIG,
            )
    }

    @Test
    fun `the period the naming was official is recorded, not assumed`() {
        PersianAnimalYear.IN_FORCE_YEARS shouldBe 1329..1344
    }
}
