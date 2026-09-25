/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.praytimes

import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.UmmAlQuraCalendar
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.PrayerMethod
import org.junit.jupiter.api.Test

/**
 * T-601: Umm al-Qura lengthens Isha in Ramadan, pinned to the authority's own timetables.
 *
 * The app applied 90 minutes all year, so its Makkah Isha ran about half an hour early throughout Ramadan — the month
 * that method is most used. The rule was documented only by secondary aggregators and by GPL prayer-time libraries,
 * which this project may not read, so it was left unfixed until a primary source could be found.
 *
 * It was found in the calendar authority's own published output, the way the Singapore angles were. ummulqura.org.sa
 * serves a JavaScript application today, but the Internet Archive holds the site from the years when it rendered its
 * timetable server-side, for 13 Saudi cities at a time. Nine captures across two Hijri years, 117 city-days in all,
 * give Isha minus Maghrib as exactly:
 *
 * | capture (archived) | Hijri month | interval |
 * |---|---|---|
 * | 2010-08-01 | Sha'ban 1431 | 90 min (13 cities) |
 * | 2010-08-13, 08-19, 08-26 | Ramadan 1431 | **120 min** (39 city-days) |
 * | 2010-09-09, 09-18, 09-25 | Shawwal 1431 | 90 min (39 city-days) |
 * | 2011-08-21 | Ramadan 1432 | **120 min** (13 cities) |
 * | 2011-08-31 | Shawwal 1432 | 90 min (13 cities) |
 *
 * No capture in either year departs from those two values, and the switch happens at the month boundary in both
 * directions, which is what distinguishes a rule from a seasonal drift. The two rows asserted below are Makkah's own,
 * the first row of each archived table:
 * - 9 Ramadan 1431 — Maghrib 06:49, Isha 08:49
 *   (https://web.archive.org/web/20100819071558id_/http://www.ummulqura.org.sa/)
 * - Shawwal 1431 — Maghrib 06:15, Isha 07:45
 *   (https://web.archive.org/web/20100925161345id_/http://www.ummulqura.org.sa/)
 *
 * The times themselves are not compared: the archived table is KACST's own computation with its own conventions, and
 * this test is about the *interval* the method adds after Maghrib, which is the parameter the app owns.
 */
class UmmAlQuraRamadanIshaTest {
    private val makkah = Coordinates(21.4225, 39.8262)
    private val settings = PrayerSettings(method = PrayerMethod.MAKKAH)

    @Test
    fun `the published interval is 90 minutes, and 120 in Ramadan`() {
        val rule = PrayerMethod.MAKKAH.parameters().isha
        rule shouldBe IshaRule.MinutesAfterMaghrib(90, ramadanMinutes = 120)

        val minutes = rule as IshaRule.MinutesAfterMaghrib
        (1..12).forEach { month ->
            minutes.minutesIn(month) shouldBe if (month == RAMADAN) 120 else 90
        }
    }

    @Test
    fun `Makkah's Isha follows Maghrib by two hours through Ramadan and by ninety minutes outside it`() {
        // Every day of Ramadan 1431 and of the months around it, so the boundary is crossed in both
        // directions rather than sampled in the middle of a month.
        val shaban = daysOf(1431, 8)
        val ramadan = daysOf(1431, 9)
        val shawwal = daysOf(1431, 10)

        ramadan.forEach { day -> gapMinutes(day) shouldBe 120 }
        (shaban + shawwal).forEach { day -> gapMinutes(day) shouldBe 90 }
    }

    @Test
    fun `no other method gained a Ramadan interval`() {
        // The lengthened interval is Umm al-Qura's own; a method whose Isha is an angle is unaffected, and no other
        // interval method should acquire one by copying this definition.
        val withRamadanInterval =
            PrayerMethod.entries.filter {
                val isha = it.parameters().isha
                isha is IshaRule.MinutesAfterMaghrib && isha.ramadanMinutes != isha.minutes
            }
        withRamadanInterval shouldBe listOf(PrayerMethod.MAKKAH)
    }

    /** Whole minutes from Maghrib to Isha at Makkah on [day], as the app computes them. */
    private fun gapMinutes(day: Jdn): Int {
        val times = PrayerTimesCalculator.calculate(day, makkah, UTC_OFFSET_MINUTES, settings)
        val available = times as PrayerTimesResult.Available
        return checkNotNull(available.times.isha).value - checkNotNull(available.times.maghrib).value
    }

    private fun daysOf(
        year: Int,
        month: Int,
    ): List<Jdn> =
        (1..UmmAlQuraCalendar.monthLength(year, month)).map { day ->
            UmmAlQuraCalendar.toJdn(CalendarDate(CalendarSystem.ISLAMIC, year, month, day))
        }

    private companion object {
        const val RAMADAN = 9

        /** Saudi Arabia keeps UTC+3 all year. */
        const val UTC_OFFSET_MINUTES = 3 * 60
    }
}
