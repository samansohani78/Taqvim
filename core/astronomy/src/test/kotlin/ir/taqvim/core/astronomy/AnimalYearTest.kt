/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.astronomy

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AnimalYearTest {
    @Test
    fun `the cycle is anchored on the 2020 Year of the Rat`() {
        AnimalYear.ofChineseYearStartingIn(2020) shouldBe ChineseZodiacAnimal.RAT
        AnimalYear.ofChineseYearStartingIn(2021) shouldBe ChineseZodiacAnimal.OX
        AnimalYear.ofChineseYearStartingIn(2019) shouldBe ChineseZodiacAnimal.PIG
        AnimalYear.ofChineseYearStartingIn(2026) shouldBe ChineseZodiacAnimal.HORSE
        AnimalYear.ofChineseYearStartingIn(2020 - 12 * 50) shouldBe ChineseZodiacAnimal.RAT
    }

    @Test
    fun `mapping table for 24 consecutive years`() {
        (2020 until 2044).map(AnimalYear::ofChineseYearStartingIn) shouldBe
            ChineseZodiacAnimal.entries + ChineseZodiacAnimal.entries
        (1996 until 2008).map(AnimalYear::ofChineseYearStartingIn) shouldBe ChineseZodiacAnimal.entries
    }
}
