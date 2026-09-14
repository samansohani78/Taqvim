/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.timeline

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-900: minutes ↔ pixels, zoom limits, 15-minute steps and the drag box of a new event. */
class TimelineGeometryTest {
    private val minutes = Arb.int(-100..1600)

    @Test
    fun `zoom stays between half and double`() {
        TimelineGeometry.clampZoom(Float.NaN) shouldBe 1f
        TimelineGeometry.clampZoom(0.1f) shouldBe 0.5f
        TimelineGeometry.clampZoom(3f) shouldBe 2f
        TimelineGeometry.clampZoom(1.3f) shouldBe 1.3f
    }

    @Test
    fun `minutes snap to 15-minute steps within the day`() {
        mapOf(-5 to 0, 0 to 0, 14 to 0, 15 to 15, 1439 to 1425, 1500 to 1440).forEach { (minute, step) ->
            withClue("down $minute") { TimelineGeometry.snapDown(minute) shouldBe step }
        }
        mapOf(7 to 0, 8 to 15, 22 to 15, 23 to 30, 1437 to 1440, 2000 to 1440).forEach { (minute, step) ->
            withClue("nearest $minute") { TimelineGeometry.snapNearest(minute) shouldBe step }
        }
    }

    @Test
    fun `pixels map back to the minute they show`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.int(0..1439), Arb.int(12..200)) { minute, hour ->
                val hourHeight = hour.toFloat()
                val halfMinute = hourHeight / TimelineGeometry.MINUTES_PER_HOUR / 2

                val offset = TimelineGeometry.offsetOf(minute, hourHeight) + halfMinute

                TimelineGeometry.minuteAt(offset, hourHeight) shouldBe minute
            }
            TimelineGeometry.minuteAt(-10f, 48f) shouldBe 0
            TimelineGeometry.minuteAt(100_000f, 48f) shouldBe TimelineGeometry.MINUTES_PER_DAY
        }

    @Test
    fun `a drag draws the box between its two ends in either direction`() {
        listOf(
            Triple(540, 630, 540..630),
            Triple(630, 540, 540..630),
            Triple(545, 545, 540..555),
            Triple(537, 623, 525..630),
            Triple(0, 0, 0..15),
            Triple(1439, 1440, 1425..1440),
            Triple(-30, 2000, 0..1440),
        ).forEach { (from, to, span) ->
            val draft = TimelineDraft.spanning(TODAY, from, to)
            withClue("$from → $to") { draft.startMinute..draft.endMinute shouldBe span }
        }
    }

    @Test
    fun `drawn boxes are whole steps within the day and do not depend on the drag direction`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, minutes, minutes) { from, to ->
                val draft = TimelineDraft.spanning(TODAY, from, to)

                draft shouldBe TimelineDraft.spanning(TODAY, to, from)
                draft.startMinute % TimelineGeometry.SNAP_MINUTES shouldBe 0
                draft.endMinute % TimelineGeometry.SNAP_MINUTES shouldBe 0
                draft.lengthMinutes shouldBeGreaterThanOrEqual TimelineGeometry.SNAP_MINUTES
                draft.startMinute shouldBeLessThanOrEqual minOf(from, to).coerceIn(0, 1425)
                draft.endMinute shouldBeLessThanOrEqual TimelineGeometry.MINUTES_PER_DAY
            }
        }

    @Test
    fun `moving keeps the length and resizing keeps the start, both within the day`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, minutes, minutes, Arb.int(-200..200)) { from, to, steps ->
                val draft = TimelineDraft.spanning(TODAY, from, to)
                val moved = draft.moved(steps)
                val resized = draft.resized(steps)

                moved.lengthMinutes shouldBe draft.lengthMinutes
                moved.startMinute shouldBeGreaterThanOrEqual 0
                moved.endMinute shouldBeLessThanOrEqual TimelineGeometry.MINUTES_PER_DAY
                val freeStart = draft.startMinute + steps * TimelineGeometry.SNAP_MINUTES
                if (freeStart in 0..TimelineGeometry.MINUTES_PER_DAY - draft.lengthMinutes) {
                    moved.startMinute shouldBe freeStart
                }
                resized.startMinute shouldBe draft.startMinute
                resized.lengthMinutes shouldBeGreaterThanOrEqual TimelineGeometry.SNAP_MINUTES
                resized.endMinute shouldBeLessThanOrEqual TimelineGeometry.MINUTES_PER_DAY
            }
        }

    @Test
    fun `boxes that are not whole steps within the day are rejected`() {
        shouldThrow<IllegalArgumentException> { TimelineDraft(TODAY, 5, 30) }
        shouldThrow<IllegalArgumentException> { TimelineDraft(TODAY, 30, 30) }
        shouldThrow<IllegalArgumentException> { TimelineDraft(TODAY, 1425, 1455) }
        shouldThrow<IllegalArgumentException> { TimelineDraft(TODAY, -15, 15) }
        shouldThrow<IllegalArgumentException> { TimelineNow(TODAY, 1440) }
        shouldThrow<IllegalArgumentException> { timed("late", 1400, 1500) }
        shouldThrow<IllegalArgumentException> { timed("empty", 600, 600) }
        allDay("fine", "All day").isAllDay shouldBe true
    }
}
