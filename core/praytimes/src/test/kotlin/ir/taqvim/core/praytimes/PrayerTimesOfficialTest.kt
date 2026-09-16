/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.praytimes

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.types.shouldBeInstanceOf
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.MinuteOfDay
import ir.taqvim.core.testing.Fixtures
import ir.taqvim.core.testing.GoldenFile
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

/**
 * A-10 against the Institute of Geophysics' official 1405 prayer timetables for 31 Iranian cities
 * (golden/iran-prayer-times-1405). The published times use the TEHRAN method; they are rounded to the minute.
 */
class PrayerTimesOfficialTest {
    private val coordinatesPattern = Regex("""latitude (\d+\.\d+) N, longitude (\d+\.\d+) E""")

    private fun minutes(text: String): Int {
        val (hour, minute) = text.split(':').map(String::toInt)
        return hour * 60 + minute
    }

    /** Distance between two times of day in minutes, across midnight. */
    private fun distance(
        official: String,
        computed: MinuteOfDay?,
    ): Int {
        val value = computed?.value ?: return Int.MAX_VALUE
        val difference = Math.floorMod(minutes(official) - value, MINUTES_PER_DAY)
        return minOf(difference, MINUTES_PER_DAY - difference)
    }

    @TestFactory
    fun `every day of 31 official city timetables`(): List<DynamicTest> =
        CITIES.map { city ->
            DynamicTest.dynamicTest(city) {
                val path = "golden/iran-prayer-times-1405/$city.csv"
                val (latitude, longitude) =
                    coordinatesPattern
                        .find(
                            Fixtures.text(path),
                        ).let { requireNotNull(it) }
                        .destructured
                val place = Coordinates(latitude.toDouble(), longitude.toDouble())
                val days =
                    GoldenFile
                        .load(path)
                        .lines
                        .drop(1)
                        .map { it.split(',') }
                days shouldHaveSize 365
                val mismatches =
                    days.flatMap { row ->
                        val (year, month, day) = row[0].split('-').map(String::toInt)
                        val jdn = PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, year, month, day))
                        val result = PrayerTimesCalculator.calculate(jdn, place, IRAN_STANDARD_TIME_MINUTES)
                        result.shouldBeInstanceOf<PrayerTimesResult.Available>()
                        val times = result.times
                        listOf(
                            Triple("fajr", distance(row[1], times.fajr), 1),
                            Triple("sunrise", distance(row[2], times.sunrise), 1),
                            Triple("dhuhr", distance(row[3], times.dhuhr), 1),
                            Triple("sunset", distance(row[4], times.sunset), 1),
                            Triple("maghrib", distance(row[5], times.maghrib), 1),
                            Triple("midnight", distance(row[6], times.midnight), 1),
                        ).filter { (_, minutes, tolerance) -> minutes > tolerance }
                            .map { (prayer, minutes, _) -> "${row[0]} $prayer off by $minutes min" }
                    }
                withClue(mismatches.take(10).joinToString("\n")) { mismatches.shouldBeEmpty() }
            }
        }

    private companion object {
        const val MINUTES_PER_DAY = 1_440
        const val IRAN_STANDARD_TIME_MINUTES = 210
        val CITIES =
            listOf(
                "Ahvaz",
                "Arak",
                "Ardebil",
                "BandarAbbas",
                "Birjand",
                "Bojnord",
                "Boshehr",
                "Gorgan",
                "Hamedan",
                "Ilam",
                "Isfahan",
                "Karaj",
                "Kerman",
                "Kermanshah",
                "Khorramabad",
                "Mashhad",
                "Orumyeh",
                "Qazvin",
                "Qom",
                "Rasht",
                "Sanandaj",
                "Sari",
                "Semnan",
                "Shahrkord",
                "Shiraz",
                "Tabriz",
                "Tehran",
                "Yasooj",
                "Yazd",
                "Zahedan",
                "Zanjan",
            )
    }
}
