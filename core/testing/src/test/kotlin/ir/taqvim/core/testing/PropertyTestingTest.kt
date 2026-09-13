/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.testing

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PropertyTestingTest {
    @Test
    fun `parses positive iteration counts and falls back otherwise`() {
        PropertyTesting.parse("10000") shouldBe 10_000
        PropertyTesting.parse(" 250 ") shouldBe 250
        PropertyTesting.parse(null) shouldBe 1_000
        PropertyTesting.parse("") shouldBe 1_000
        PropertyTesting.parse("many") shouldBe 1_000
        PropertyTesting.parse("0") shouldBe 1_000
        PropertyTesting.parse("-5") shouldBe 1_000
    }

    @Test
    fun `reads the system property`() {
        val previous = System.getProperty(PropertyTesting.ITERATIONS_PROPERTY)
        System.setProperty(PropertyTesting.ITERATIONS_PROPERTY, "42")
        val configured = PropertyTesting.iterations
        if (previous == null) {
            System.clearProperty(PropertyTesting.ITERATIONS_PROPERTY)
        } else {
            System.setProperty(PropertyTesting.ITERATIONS_PROPERTY, previous)
        }

        configured shouldBe 42
    }
}
