/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import kotlin.time.Instant

/** Today in Tehran on Sunday 22 Shahrivar 1405 (13 September 2026), for the T-1213/T-1215 surfaces. */
internal object TodayFixtures {
    /** 12:00 in Tehran. */
    val NOW: Instant = Instant.parse("2026-09-13T08:30:00Z")

    /** Midnight after [NOW] in Tehran (UTC+03:30). */
    val MIDNIGHT: Instant = Instant.parse("2026-09-13T20:30:00Z")

    /** Maghrib, the next prayer after [NOW]. */
    val MAGHRIB: Instant = Instant.parse("2026-09-13T15:40:00Z")

    val PRAYERS: List<SummaryPrayer> =
        listOf(
            SummaryPrayer("Fajr", "04:52"),
            SummaryPrayer("Dhuhr", "13:07"),
            SummaryPrayer("Maghrib", "19:10", isNext = true),
        )

    fun summary(
        withPlace: Boolean = true,
        otherDates: List<String> = listOf("13 September 2026", "1 Rabi al-Awwal 1448"),
        holidays: List<String> = listOf("Holiday"),
        dayNumber: String = "22",
    ): TodaySummary =
        TodaySummary(
            dayOfMonth = 22,
            dayNumber = dayNumber,
            title = "22 Shahrivar 1405",
            weekday = "Sunday",
            otherDates = otherDates,
            holidays = holidays,
            prayers = if (withPlace) PRAYERS else emptyList(),
            nextDayAt = MIDNIGHT,
            nextPrayerAt = if (withPlace) MAGHRIB else null,
        )
}
