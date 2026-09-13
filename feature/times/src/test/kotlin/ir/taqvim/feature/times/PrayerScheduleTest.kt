/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.times

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.floats.shouldBeBetween
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.MinuteOfDay
import ir.taqvim.core.praytimes.PrayerTimesResult
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import org.junit.jupiter.api.Test

class PrayerScheduleTest {
    private val tehran = TimesFixtures.tehran()
    private val solstice = LocalDate(2026, 6, 21).toJdn()

    private fun timesOf(day: Jdn) =
        when (val result = PrayerSchedule.calculate(day, tehran)) {
            is PrayerTimesResult.Available -> result.times
            is PrayerTimesResult.Unavailable -> error("Tehran has times every day")
        }

    private fun MinuteOfDay.on(day: Jdn) = day.toLocalDateTimeAt(this).toInstant(tehran.timeZone)

    private fun Jdn.toLocalDateTimeAt(time: MinuteOfDay) =
        LocalDate.fromEpochDays(value - 2_440_588L).atTime(time.hour, time.minute)

    @Test
    fun `the offset follows the zone rules of the day`() {
        PrayerSchedule.utcOffsetMinutes(solstice, tehran) shouldBe 210
        val oslo = TimesFixtures.tromso()
        PrayerSchedule.utcOffsetMinutes(solstice, oslo) shouldBe 120
        PrayerSchedule.utcOffsetMinutes(LocalDate(2026, 1, 15).toJdn(), oslo) shouldBe 60
    }

    @Test
    fun `entries list every time in display order`() {
        val entries = PrayerSchedule.entries(timesOf(solstice))
        entries.map { it.kind } shouldContainExactly PrayerKind.entries
        entries.filter { it.kind.primary }.map { it.kind } shouldContainExactly
            listOf(
                PrayerKind.FAJR,
                PrayerKind.SUNRISE,
                PrayerKind.DHUHR,
                PrayerKind.ASR,
                PrayerKind.MAGHRIB,
                PrayerKind.ISHA,
            )
    }

    @Test
    fun `before noon the next time is dhuhr, counted from sunrise`() {
        val times = timesOf(solstice)
        val now = TimesFixtures.at("2026-06-21T12:00", tehran)
        val next = PrayerSchedule.next(now, tehran).shouldNotBeNull()
        next.kind shouldBe PrayerKind.DHUHR
        next.at shouldBe times.dhuhr.on(solstice)
        next.remaining shouldBe next.at - now
        val expected = (now - times.sunrise.on(solstice)) / (next.at - times.sunrise.on(solstice))
        next.progress shouldBe expected.toFloat()
    }

    @Test
    fun `after isha the next time is tomorrow's fajr, and before fajr it is today's`() {
        val tomorrow = solstice + 1
        val late = PrayerSchedule.next(TimesFixtures.at("2026-06-21T23:59", tehran), tehran).shouldNotBeNull()
        late.kind shouldBe PrayerKind.FAJR
        late.at shouldBe requireNotNull(timesOf(tomorrow).fajr).on(tomorrow)

        val early = PrayerSchedule.next(TimesFixtures.at("2026-06-22T00:30", tehran), tehran).shouldNotBeNull()
        early.at shouldBe late.at
        early.progress.shouldBeBetween(0f, 1f, 0f)
        (early.progress > late.progress) shouldBe true
    }

    @Test
    fun `the next time is always ahead and its progress within the interval`() {
        val start = TimesFixtures.at("2026-01-01T00:00", tehran)
        repeat(24 * 40) { step ->
            val now = start + (step * 37).minutes + (step % 5).hours
            val next = PrayerSchedule.next(now, tehran).shouldNotBeNull()
            (next.at > now) shouldBe true
            next.progress.shouldBeBetween(0f, 1f, 0f)
            (next.remaining < 24.hours) shouldBe true
        }
    }

    @Test
    fun `there is no next time through a polar day`() {
        val tromso = TimesFixtures.tromso()
        PrayerSchedule.calculate(solstice, tromso) shouldBe
            PrayerTimesResult.Unavailable(PrayerTimesResult.Reason.POLAR_DAY)
        PrayerSchedule.next(TimesFixtures.at("2026-06-21T12:00", tromso), tromso).shouldBeNull()
    }

    @Test
    fun `times are written with two-digit fields in the requested digits`() {
        MinuteOfDay(5 * 60 + 7).localized(NumeralSystem.LATIN) shouldBe "05:07"
        MinuteOfDay(23 * 60 + 59).localized(NumeralSystem.PERSIAN) shouldBe "۲۳:۵۹"
    }
}
