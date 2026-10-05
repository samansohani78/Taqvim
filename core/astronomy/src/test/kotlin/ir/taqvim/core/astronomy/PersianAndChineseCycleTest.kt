/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.PersianAnimalYear
import ir.taqvim.core.calendar.PersianZodiacAnimal
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import org.junit.jupiter.api.Test

/** DT-015/T-406: how the Iranian twelve-animal year relates to the Chinese one it was borrowed from. */
class PersianAndChineseCycleTest {
    @Test
    fun `the difference is when the year turns, not which position`() {
        // Worth stating precisely, because it is easy to get backwards. The Persian cycle was borrowed from the
        // Turco-Mongol one, so the *positions* line up and a Solar Hijri year holds the same position as the
        // Gregorian year it mostly covers — even though the names differ (leopard for tiger, whale for dragon).
        // What differs is the turn: Nowruz in late March against the Chinese new year in late January or February.
        PersianAnimalYear.of(1405).ordinal shouldBe AnimalYear.ofChineseYearStartingIn(2026).ordinal
        PersianAnimalYear.of(1400).ordinal shouldBe AnimalYear.ofChineseYearStartingIn(2021).ordinal

        // So the two disagree for any date between the Chinese new year and Nowruz. On 1 March 2026 the Chinese
        // year is already the horse, while Solar Hijri 1404 runs until Nowruz and is still the snake.
        val firstOfMarch2026 = CalendarDate(CalendarSystem.GREGORIAN, 2026, 3, 1)
        AnimalYear.forDate(firstOfMarch2026) shouldBe ChineseZodiacAnimal.HORSE
        PersianAnimalYear.of(1404) shouldBe PersianZodiacAnimal.SNAKE
    }

    @Test
    fun `both cycles have the same twelve positions`() {
        PersianZodiacAnimal.entries.size shouldBe ChineseZodiacAnimal.entries.size
    }
}
