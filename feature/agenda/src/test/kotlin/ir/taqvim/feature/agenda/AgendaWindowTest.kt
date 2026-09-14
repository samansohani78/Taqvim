/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.agenda

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-901 paging: the loaded months grow at either end, stay contiguous and bounded, and stop at the limits. */
class AgendaWindowTest {
    @Test
    fun `the first window surrounds today's month`() {
        val window = AgendaWindow.INITIAL
        (0 in window).shouldBeTrue()
        (-1 in window).shouldBeTrue()
        (3 in window).shouldBeFalse()
        window.later() shouldBe AgendaWindow(-1, 5)
        window.earlier() shouldBe AgendaWindow(-4, 2)
    }

    @Test
    fun `any sequence of loads keeps the window contiguous, bounded and anchored at the end it grew`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.list(Arb.boolean(), 0..600)) { steps ->
                var window = AgendaWindow.INITIAL
                steps.forEach { later ->
                    val next = if (later) window.later() else window.earlier()
                    (next.last - next.first < AgendaWindow.MAX_SPAN).shouldBeTrue()
                    (next.first >= -AgendaWindow.LIMIT && next.last <= AgendaWindow.LIMIT).shouldBeTrue()
                    if (later) {
                        (next.last >= window.last && window.last in next).shouldBeTrue()
                    } else {
                        (next.first <= window.first && window.first in next).shouldBeTrue()
                    }
                    window = next
                }
            }
        }

    @Test
    fun `loading stops at a century either way`() {
        var window = AgendaWindow.INITIAL
        repeat(500) { window = window.later() }
        window.last shouldBe AgendaWindow.LIMIT
        window.canLoadLater.shouldBeFalse()
        window.later() shouldBe window
        repeat(1_000) { window = window.earlier() }
        window.first shouldBe -AgendaWindow.LIMIT
        window.canLoadEarlier.shouldBeFalse()
        window.earlier() shouldBe window
        window.canLoadLater.shouldBeTrue()
    }

    @Test
    fun `invalid windows are rejected`() {
        shouldThrow<IllegalArgumentException> { AgendaWindow(2, 1) }
        shouldThrow<IllegalArgumentException> { AgendaWindow(0, AgendaWindow.MAX_SPAN) }
        shouldThrow<IllegalArgumentException> { AgendaWindow(-AgendaWindow.LIMIT - 1, -AgendaWindow.LIMIT) }
        shouldThrow<IllegalArgumentException> { AgendaWindow(AgendaWindow.LIMIT, AgendaWindow.LIMIT + 1) }
    }
}
