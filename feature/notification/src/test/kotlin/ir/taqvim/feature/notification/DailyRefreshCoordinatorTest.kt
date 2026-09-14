/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.testing.PropertyTesting
import ir.taqvim.feature.notification.TodayFixtures.MAGHRIB
import ir.taqvim.feature.notification.TodayFixtures.MIDNIGHT
import ir.taqvim.feature.notification.TodayFixtures.NOW
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test

/** T-1213/T-1214 (U): one wake-up at the earliest time a surface goes stale; failures retry, nothing spins. */
class DailyRefreshCoordinatorTest {
    private class RecordingScheduler : DailyRefreshScheduler {
        var scheduled: Instant? = null
        var cancels = 0

        override fun schedule(at: Instant) {
            scheduled = at
        }

        override fun cancel() {
            scheduled = null
            cancels++
        }
    }

    private val clock =
        object : Clock {
            override fun now(): Instant = NOW
        }

    @Test
    fun `every refresh runs and the earliest wake-up is scheduled`(): Unit =
        runTest {
            val scheduler = RecordingScheduler()
            val runs = mutableListOf<Instant>()
            val refreshes =
                listOf(
                    DailyRefresh {
                        runs += it
                        MIDNIGHT
                    },
                    DailyRefresh {
                        runs += it
                        null
                    },
                    DailyRefresh {
                        runs += it
                        MAGHRIB
                    },
                )

            DailyRefreshCoordinator(refreshes, scheduler, clock).run() shouldBe MAGHRIB

            runs shouldBe listOf(NOW, NOW, NOW)
            scheduler.scheduled shouldBe MAGHRIB
        }

    @Test
    fun `without a wake-up the alarm is cancelled`(): Unit =
        runTest {
            val scheduler = RecordingScheduler().apply { schedule(MIDNIGHT) }

            DailyRefreshCoordinator(listOf(DailyRefresh { null }), scheduler).run(NOW) shouldBe null
            DailyRefreshCoordinator(emptyList(), scheduler).run(NOW) shouldBe null

            scheduler.scheduled shouldBe null
            scheduler.cancels shouldBe 2
        }

    @Test
    fun `a failing refresh is retried later and does not stop the others`(): Unit =
        runTest {
            val scheduler = RecordingScheduler()
            var others = 0
            val refreshes =
                listOf(
                    DailyRefresh { error("storage unavailable") },
                    DailyRefresh {
                        others++
                        MIDNIGHT
                    },
                )

            DailyRefreshCoordinator(refreshes, scheduler).run(NOW) shouldBe NOW + DailyRefreshCoordinator.RETRY

            others shouldBe 1
        }

    @Test
    fun `cancellation is not taken for a failure`(): Unit =
        runTest {
            val stopped = DailyRefresh { throw CancellationException("stopped") }
            val coordinator = DailyRefreshCoordinator(listOf(stopped), RecordingScheduler())

            shouldThrow<CancellationException> { coordinator.run(NOW) }
        }

    @Test
    fun `a wake-up in the past or right now waits the minimum gap`(): Unit =
        runTest {
            val scheduler = RecordingScheduler()

            DailyRefreshCoordinator(listOf(DailyRefresh { NOW - 5.seconds }), scheduler).run(NOW)

            scheduler.scheduled shouldBe NOW + DailyRefreshCoordinator.MIN_GAP
        }

    @Test
    fun `the next day starts at local midnight`() {
        DailyRefreshCoordinator.startOfNextDay(NOW, TimeZone.of("Asia/Tehran")) shouldBe MIDNIGHT
        DailyRefreshCoordinator.startOfNextDay(MIDNIGHT, TimeZone.of("Asia/Tehran")) shouldBe MIDNIGHT + 24.hours
    }

    @Test
    fun `property - the next day starts after now, within a day and on the following date`(): Unit =
        runTest {
            // Half-hour offsets and DST in both hemispheres; none of these zones skips a whole calendar day.
            val zones = listOf("Asia/Tehran", "Asia/Kabul", "Europe/Berlin", "America/Santiago", "UTC")
            checkAll(PropertyTesting.iterations, Arb.long(0L..4_102_444_800_000L), Arb.element(zones)) { millis, id ->
                val zone = TimeZone.of(id)
                val now = Instant.fromEpochMilliseconds(millis)

                val next = DailyRefreshCoordinator.startOfNextDay(now, zone)

                (next > now && next - now <= 26.hours) shouldBe true
                next.toLocalDateTime(zone).date shouldBe now.toLocalDateTime(zone).date.plus(1, DateTimeUnit.DAY)
            }
        }

    @Test
    fun `property - the wake-up is the earliest answer but never within the minimum gap`(): Unit =
        runTest {
            checkAll(PropertyTesting.iterations, Arb.long(-86_400L..86_400L), Arb.long(-86_400L..86_400L)) { a, b ->
                val scheduler = RecordingScheduler()
                val answers = listOf(NOW + a.seconds, NOW + b.seconds)

                DailyRefreshCoordinator(answers.map { answer -> DailyRefresh { answer } }, scheduler).run(NOW)

                scheduler.scheduled shouldBe maxOf(answers.min(), NOW + DailyRefreshCoordinator.MIN_GAP)
            }
        }
}
