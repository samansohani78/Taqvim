/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeSortedBy
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.MinuteOfDay
import ir.taqvim.core.praytimes.PrayerSettings
import ir.taqvim.core.praytimes.PrayerTimesCalculator
import ir.taqvim.core.praytimes.PrayerTimesResult
import ir.taqvim.core.testing.PropertyTesting
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import org.junit.jupiter.api.Test

/** T-1102 (U): when athans are due, checked against the A-10 calculator in Iran Standard Time. */
class AthanPlannerTest {
    private val tehranOffset = UtcOffset(hours = 3, minutes = 30)
    private val from = Instant.parse("2026-01-01T00:00:00Z").toEpochMilliseconds()
    private val until = Instant.parse("2027-01-01T00:00:00Z").toEpochMilliseconds()
    private val iterations = minOf(PropertyTesting.iterations, ITERATIONS)
    private val settings = PrayerSettings()

    /** The time of [prayer] on [day] in Tehran, computed directly with the calculator. */
    private fun expected(
        prayer: AthanPrayer,
        day: Jdn,
    ): Instant {
        val times =
            when (
                val result = PrayerTimesCalculator.calculate(day, AthanFixtures.TEHRAN, TEHRAN_MINUTES, settings)
            ) {
                is PrayerTimesResult.Available -> result.times
                is PrayerTimesResult.Unavailable -> error("Tehran has prayer times every day")
            }
        val time: MinuteOfDay? =
            when (prayer) {
                AthanPrayer.FAJR -> times.fajr
                AthanPrayer.DHUHR -> times.dhuhr
                AthanPrayer.ASR -> times.asr
                AthanPrayer.MAGHRIB -> times.maghrib
                AthanPrayer.ISHA -> times.isha
            }
        return day.toLocalDate().atTime(time.shouldNotBeNull().hour, time.minute).toInstant(tehranOffset)
    }

    @Test
    fun `athans are the enabled prayer times plus their gaps, strictly after now and in order`(): Unit =
        runBlocking {
            checkAll(
                iterations,
                Arb.long(from until until),
                Arb.list(Arb.int(-60..60), 5..5),
                Arb.int(0 until 32),
            ) { millis, gaps, mask ->
                val now = Instant.fromEpochMilliseconds(millis)
                val enabled = AthanPrayer.entries.filter { mask and (1 shl it.ordinal) != 0 }.toSet()
                val settings = AthanFixtures.tehran(AthanFixtures.alerts(enabled) { gaps[it.ordinal] })

                val athans = AthanPlanner.upcoming(now, settings)

                athans.shouldBeSortedBy { it.at }
                athans.all { it.at > now && it.at < now + 4.days && it.prayer in enabled } shouldBe true
                athans.forEach { it.at shouldBe expected(it.prayer, it.day) + gaps[it.prayer.ordinal].minutes }
                athans.map { it.prayer to it.day }.distinct().size shouldBe athans.size
                athans.forEach { AthanPlanner.at(it.at, settings).shouldNotBeNull().at shouldBe it.at }
                val tomorrow = now.toJdn(TimeZone.of("Asia/Tehran")) + 1
                athans.filter { it.day == tomorrow }.map { it.prayer }.toSet() shouldBe enabled
            }
        }

    @Test
    fun `after the last athan of the day the next one is tomorrow's Fajr`() {
        val day = Instant.parse("2026-09-13T12:00:00Z").toJdn(TimeZone.of("Asia/Tehran"))
        val settings = AthanFixtures.tehran()
        val afterIsha = expected(AthanPrayer.ISHA, day) + 1.minutes

        val next = AthanPlanner.upcoming(afterIsha, settings).first()

        next shouldBe PlannedAthan(AthanPrayer.FAJR, day + 1, expected(AthanPrayer.FAJR, day + 1))
        AthanPlanner.upcoming(expected(AthanPrayer.ISHA, day), settings).first().prayer shouldBe AthanPrayer.FAJR
    }

    @Test
    fun `a negative gap on tomorrow's Fajr can fall before midnight and is still planned`() {
        val day = Instant.parse("2026-06-21T12:00:00Z").toJdn(TimeZone.of("Asia/Tehran"))
        val settings = AthanFixtures.tehran(AthanFixtures.alerts(setOf(AthanPrayer.FAJR)) { -60 })
        val fajr = expected(AthanPrayer.FAJR, day + 1) - 60.minutes

        AthanPlanner.upcoming(fajr - 1.minutes, settings).first() shouldBe PlannedAthan(AthanPrayer.FAJR, day + 1, fajr)
    }

    @Test
    fun `polar days have no athans and nothing is planned at other instants`() {
        AthanPlanner.upcoming(Instant.parse("2026-06-21T10:00:00Z"), AthanFixtures.tromso()).shouldBeEmpty()
        val settings = AthanFixtures.tehran()
        val midnight = Instant.parse("2026-09-13T00:00:00Z")
        val dhuhr = AthanPlanner.upcoming(midnight, settings).first { it.prayer == AthanPrayer.DHUHR }
        AthanPlanner.at(dhuhr.at + 1.minutes, settings) shouldBe null
        AthanPlanner.upcoming(midnight, AthanFixtures.tehran(AthanFixtures.alerts(emptySet()))).shouldBeEmpty()
    }

    @Test
    fun `Iran time plans in UTC+03_30, which Tehran uses all year`() {
        val now = Instant.parse("2026-03-25T00:00:00Z")
        AthanPlanner.zoneOf(AthanFixtures.tehran(useIranTime = true)) shouldBe AthanPlanner.IRAN_STANDARD_TIME
        AthanPlanner.zoneOf(AthanFixtures.tehran()) shouldBe TimeZone.of("Asia/Tehran")
        AthanPlanner.upcoming(now, AthanFixtures.tehran(useIranTime = true)) shouldBe
            AthanPlanner.upcoming(now, AthanFixtures.tehran())
        AthanPlanner.upcoming(now, AthanFixtures.tehran(useIranTime = true)).map { it.prayer }.shouldContainAll(
            AthanPrayer.entries,
        )
    }

    private companion object {
        const val ITERATIONS = 150
        const val TEHRAN_MINUTES = 210
    }
}
