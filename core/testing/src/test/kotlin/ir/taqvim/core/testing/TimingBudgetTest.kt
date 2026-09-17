/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.testing

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/** ADR-0018 addendum: timing budgets are scaled only by a valid factor of at least 1. */
class TimingBudgetTest {
    private val previous: String? = System.getProperty(TimingTest.SCALE_PROPERTY)

    @AfterEach
    fun restore() {
        if (previous == null) {
            System.clearProperty(TimingTest.SCALE_PROPERTY)
        } else {
            System.setProperty(TimingTest.SCALE_PROPERTY, previous)
        }
    }

    private fun scaledWith(value: String?): Triple<Long, Int, Double> {
        if (value ==
            null
        ) {
            System.clearProperty(TimingTest.SCALE_PROPERTY)
        } else {
            System.setProperty(TimingTest.SCALE_PROPERTY, value)
        }
        return Triple(TimingTest.budget(50L), TimingTest.budget(16), TimingTest.budget(50.0))
    }

    @Test
    fun `budgets keep the plan's value unless a larger factor is set`() {
        scaledWith(null) shouldBe Triple(50L, 16, 50.0)
        scaledWith("1") shouldBe Triple(50L, 16, 50.0)
        scaledWith("2") shouldBe Triple(100L, 32, 100.0)
        scaledWith("1.5") shouldBe Triple(75L, 24, 75.0)
    }

    @Test
    fun `invalid or smaller factors are ignored`() {
        scaledWith("0.5") shouldBe Triple(50L, 16, 50.0)
        scaledWith("fast") shouldBe Triple(50L, 16, 50.0)
        scaledWith("") shouldBe Triple(50L, 16, 50.0)
    }
}
