/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.timeline

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import ir.taqvim.core.testing.PropertyTesting
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-900: interval-graph coloring of timed events — a table of layouts incl. nested events, and its properties. */
class IntervalColoringTest {
    private val intervals: Arb<List<TimedInterval>> =
        Arb
            .list(Arb.bind(Arb.int(0..1400), Arb.int(1..240)) { start, length -> start to minOf(start + length, 1440) })
            .map { spans -> spans.mapIndexed { index, (start, end) -> TimedInterval(index.toString(), start, end) } }

    private fun parse(spec: String): List<TimedInterval> =
        spec.split(' ').filter { it.isNotBlank() }.map { token ->
            val (key, span) = token.split(':')
            val (start, end) = span.split('-')
            TimedInterval(key, start.toInt(), end.toInt())
        }

    private fun render(slots: List<IntervalSlot>): String =
        slots.sortedBy { it.interval.key }.joinToString(" ") { "${it.interval.key}:${it.column}/${it.columns}" }

    private fun overlap(
        first: TimedInterval,
        second: TimedInterval,
    ): Boolean = first.start < second.end && second.start < first.end

    @Test
    fun `layouts of disjoint, touching, overlapping, nested and chained events`() {
        CASES.size shouldBeGreaterThanOrEqual 30
        CASES.forEach { (input, expected) ->
            withClue(input) { render(IntervalColoring.assign(parse(input))) shouldBe expected }
        }
    }

    @Test
    fun `slots come in the greedy order by start, longer first, then key`() {
        val input = parse("c:60-90 b:0-30 a:0-90 d:60-120 e:60-120")

        IntervalColoring.assign(input).map { it.interval.key } shouldContainExactly listOf("a", "b", "d", "e", "c")
        shouldThrow<IllegalArgumentException> { TimedInterval("empty", 10, 10) }
        shouldThrow<IllegalArgumentException> { TimedInterval("reversed", 20, 10) }
    }

    @Test
    fun `overlapping events never share a column and share their width`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, intervals) { input ->
                val slots = IntervalColoring.assign(input)

                slots.map { it.interval } shouldContainExactly
                    input.sortedWith(
                        compareBy<TimedInterval> { it.start }.thenByDescending { it.end }.thenBy { it.key },
                    )
                slots.forEach { slot -> slot.column shouldBeLessThan slot.columns }
                slots.forEach { first ->
                    slots.filter { it !== first && overlap(it.interval, first.interval) }.forEach { second ->
                        second.column shouldNotBe first.column
                        second.columns shouldBe first.columns
                    }
                }
            }
        }

    @Test
    fun `every cluster is exactly as wide as its deepest overlap`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, intervals) { input ->
                val columns = IntervalColoring.assign(input).associate { it.interval.key to it.columns }

                clusters(input).forEach { cluster ->
                    val depth =
                        cluster.maxOf { point -> cluster.count { it.start <= point.start && point.start < it.end } }
                    cluster.forEach { columns[it.key] shouldBe depth }
                }
            }
        }

    @Test
    fun `the order of the input does not change the layout`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, intervals, Arb.long()) { input, seed ->
                val layout = render(IntervalColoring.assign(input))

                render(IntervalColoring.assign(input.shuffled(Random(seed)))) shouldBe layout
            }
        }

    /** Groups of intervals connected by overlaps, found independently of the implementation. */
    private fun clusters(input: List<TimedInterval>): List<List<TimedInterval>> {
        val groups = mutableListOf<MutableList<TimedInterval>>()
        var end = Int.MIN_VALUE
        input.sortedBy { it.start }.forEach { interval ->
            if (interval.start >= end) groups += mutableListOf<TimedInterval>()
            groups.last() += interval
            end = maxOf(end, interval.end)
        }
        return groups
    }

    private companion object {
        /** Input intervals (`key:start-end`) and the expected `key:column/columns`, sorted by key. */
        val CASES: List<Pair<String, String>> =
            listOf(
                "" to "",
                "a:0-60" to "a:0/1",
                "a:0-60 b:60-120" to "a:0/1 b:0/1",
                "a:0-60 b:90-120" to "a:0/1 b:0/1",
                "a:0-60 b:30-90" to "a:0/2 b:1/2",
                "a:0-60 b:0-60" to "a:0/2 b:1/2",
                "a:0-120 b:30-60" to "a:0/2 b:1/2",
                "a:0-180 b:30-150 c:60-90" to "a:0/3 b:1/3 c:2/3",
                "a:0-180 b:30-60 c:90-120" to "a:0/2 b:1/2 c:1/2",
                "a:0-60 b:30-90 c:60-120" to "a:0/2 b:1/2 c:0/2",
                "a:0-60 b:50-110 c:100-160 d:150-210" to "a:0/2 b:1/2 c:0/2 d:1/2",
                "a:0-100 b:10-110 c:20-120" to "a:0/3 b:1/3 c:2/3",
                "a:0-30 b:0-90" to "a:1/2 b:0/2",
                "a:0-30 b:0-60 c:0-90" to "a:2/3 b:1/3 c:0/3",
                "a:0-60 b:30-90 c:120-180" to "a:0/2 b:1/2 c:0/1",
                "a:0-60 b:30-90 c:120-180 d:150-210" to "a:0/2 b:1/2 c:0/2 d:1/2",
                "a:0-60 b:0-120 c:60-90" to "a:1/2 b:0/2 c:1/2",
                "a:0-240 b:0-60 c:30-90 d:100-200" to "a:0/3 b:1/3 c:2/3 d:1/3",
                "a:0-1440 b:540-600 c:600-660" to "a:0/2 b:1/2 c:1/2",
                "a:0-1 b:1-2" to "a:0/1 b:0/1",
                "a:1380-1440 b:1400-1440" to "a:0/2 b:1/2",
                "a:120-180 b:0-60" to "a:0/1 b:0/1",
                "a:0-100 b:20-40 c:50-70 d:80-120" to "a:0/2 b:1/2 c:1/2 d:1/2",
                "a:0-60 b:0-60 c:0-60 d:0-60" to "a:0/4 b:1/4 c:2/4 d:3/4",
                "b:0-60 a:0-60" to "a:0/2 b:1/2",
                "a:0-300 b:10-290 c:20-280 d:30-270" to "a:0/4 b:1/4 c:2/4 d:3/4",
                "a:0-100 b:50-150 c:100-120" to "a:0/2 b:1/2 c:0/2",
                "a:0-60 b:30-90 c:90-100" to "a:0/2 b:1/2 c:0/1",
                "a:0-60 b:10-50 c:55-65" to "a:0/2 b:1/2 c:1/2",
                "a:0-60 b:0-60 c:0-60 d:60-120" to "a:0/3 b:1/3 c:2/3 d:0/1",
                "a:0-60 b:59-61 c:60-120" to "a:0/2 b:1/2 c:0/2",
                "a:540-600 b:540-570 c:570-630 d:600-660 e:615-645" to "a:0/3 b:1/3 c:1/3 d:0/3 e:2/3",
                "a:480-1020 b:540-600 c:545-555 d:600-720 e:700-800" to "a:0/3 b:1/3 c:2/3 d:1/3 e:2/3",
            )
    }
}
