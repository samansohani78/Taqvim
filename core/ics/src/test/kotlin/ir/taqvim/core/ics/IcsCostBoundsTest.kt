/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.ics

import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import ir.taqvim.core.testing.TimingTest
import kotlin.system.measureTimeMillis
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * The reader's cost stays proportional to its input (review R04). A subscribed or imported file is untrusted: folded
 * lines were joined by copying the whole logical line for each continuation, so 960 KB of folds took seconds.
 */
class IcsCostBoundsTest {
    private fun calendar(summary: String) =
        "BEGIN:VCALENDAR\nBEGIN:VEVENT\nUID:1\nDTSTART;VALUE=DATE:20260101\n$summary\nEND:VEVENT\nEND:VCALENDAR\n"

    private fun folded(continuations: Int) = calendar("SUMMARY:a\n" + " a\n".repeat(continuations).trimEnd('\n'))

    @Test
    fun `folded continuations are joined in order into one logical line`() {
        val lines = ContentLines.unfold("A:1\n 2\n\t3\nB:4\r\n 5\r\n")
        lines shouldBe listOf(1 to "A:123", 4 to "B:45")
    }

    @Test
    @Tag(TimingTest.TAG)
    fun `joining 320 000 folded continuations takes time proportional to the input`() {
        val text = folded(CONTINUATIONS)
        repeat(WARM_UP_RUNS) { ContentLines.unfold(text) }

        val best = (1..MEASURED_RUNS).minOf { measureTimeMillis { ContentLines.unfold(text) } }

        best shouldBeLessThan TimingTest.budget(BUDGET_MILLIS)
    }

    @Test
    fun `a logical line longer than the limit fails the read instead of growing without bound`() {
        val result = IcsReader.read(folded(ContentLines.MAX_LOGICAL_CHARS))
        val failure = result.shouldBeInstanceOf<IcsParseResult.Failure>()
        failure.errors.single().message shouldContain "longer than"
    }

    @Test
    fun `a document with more content lines than the limit fails the read`() {
        val lines = "X-A:1\n".repeat(ContentLines.MAX_LOGICAL_LINES)
        val failure = IcsReader.read(calendar(lines.trimEnd('\n'))).shouldBeInstanceOf<IcsParseResult.Failure>()
        failure.errors.single().message shouldContain "more than"
    }

    @Test
    fun `the reader stops as soon as it is cancelled`() {
        var checks = 0
        val stop = IllegalStateException("cancelled")
        val thrown =
            runCatching {
                IcsReader.read(folded(CONTINUATIONS)) {
                    checks++
                    if (checks == 2) throw stop
                }
            }.exceptionOrNull()
        thrown shouldBe stop
    }

    @Test
    fun `an RRULE with a huge BYDAY or BYMONTHDAY list is ignored with a warning, not expanded`() {
        val byDay = "BYDAY=" + List(HUGE_LIST) { "MO" }.joinToString(",")
        val byMonthDay = "BYMONTHDAY=" + List(HUGE_LIST) { (it % 28 + 1).toString() }.joinToString(",")
        listOf(byDay, byMonthDay).forEach { part ->
            val result = IcsReader.read(calendar("SUMMARY:x\nRRULE:FREQ=MONTHLY;$part"))
            val success = result.shouldBeInstanceOf<IcsParseResult.Success>()
            success.calendar.events
                .single()
                .recurrence
                .shouldBeNull()
            success.warnings
                .map { it.message }
                .filter { "RRULE" in it }
                .size shouldBe 1
        }
    }

    private companion object {
        const val CONTINUATIONS = 320_000
        const val WARM_UP_RUNS = 2
        const val MEASURED_RUNS = 3
        const val BUDGET_MILLIS = 300L
        const val HUGE_LIST = 100_000
    }
}
