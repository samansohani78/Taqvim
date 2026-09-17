/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.completeness

import io.kotest.matchers.collections.shouldBeEmpty
import ir.taqvim.core.astronomy.AnimalYear
import ir.taqvim.core.astronomy.CelestialBody
import ir.taqvim.core.astronomy.ChineseNewYear
import ir.taqvim.core.astronomy.Eclipses
import ir.taqvim.core.astronomy.PlanetaryHours
import ir.taqvim.core.astronomy.PlanetaryHoursResult
import ir.taqvim.core.astronomy.Sky
import ir.taqvim.core.astronomy.Tithi
import ir.taqvim.core.astronomy.Zodiac
import ir.taqvim.core.astronomy.ZodiacSystem
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.praytimes.HighLatitudeRule
import ir.taqvim.core.praytimes.PrayerSettings
import ir.taqvim.core.praytimes.PrayerTimes
import ir.taqvim.core.praytimes.PrayerTimesCalculator
import ir.taqvim.core.praytimes.PrayerTimesResult
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.offsetAt
import org.junit.jupiter.api.Test

/**
 * Owner directive 2026-09-17: prayer times and astronomy are complete for every year of 1380–1480 SH — no missing
 * values and no exceptions. Per-day computations run on every day (about 37 000; the class takes about 35 s), yearly
 * ones on every year. Calendars and events are covered by `CalendarCompletenessTest` in `:data:events`.
 */
class SkyAndTimesCompletenessTest {
    private val firstYear = 1380
    private val lastYear = 1480
    private val newYears = (firstYear..lastYear + 1).map(::persianNewYear)
    private val everyDay: List<Jdn> = (newYears.first().value until newYears.last().value).map(::Jdn)

    @Test
    fun `prayer times exist for every day in Tehran, Kabul and the Arctic`() {
        val failures =
            PLACES.flatMap { place -> everyDay.mapNotNull { day -> prayerProblem(place, day) } }
        failures.shouldBeEmpty()
    }

    @Test
    fun `sun, moon, zodiac, tithi and planetary hours exist for every day`() {
        val failures = everyDay.flatMap { day -> skyProblems(TEHRAN, day) + arcticProblems(day) }
        failures.shouldBeEmpty()
    }

    @Test
    fun `seasons, apsides, phases, eclipses and the Chinese year exist for every Gregorian year`() {
        val years = gregorianYear(newYears.first())..gregorianYear(newYears.last())
        val failures = years.flatMap(::yearProblems)
        failures.shouldBeEmpty()
    }

    @Test
    fun `the Moon visits Scorpio and every tithi occurs in every Persian year`() {
        val failures =
            newYears.zipWithNext().flatMap { (start, next) ->
                val from = start.startIn(TEHRAN.zone)
                val until = next.startIn(TEHRAN.zone)
                scorpioAndTithiProblems(start, from, until)
            }
        failures.shouldBeEmpty()
    }

    private fun prayerProblem(
        place: Place,
        day: Jdn,
    ): String? {
        val offset = place.zone.offsetAt(day.startIn(place.zone)).totalSeconds / SECONDS_PER_MINUTE
        val label = "${place.name} ${day.value}"
        return when (val result = PrayerTimesCalculator.calculate(day, place.coordinates, offset, place.settings)) {
            is PrayerTimesResult.Available -> {
                if (place.polar) arcticOrderProblem(label, result.times) else orderProblem(label, result.times)
            }

            is PrayerTimesResult.Unavailable -> {
                if (place.polar) null else "$label: no times (${result.reason})"
            }
        }
    }

    private fun orderProblem(
        label: String,
        times: PrayerTimes,
    ): String? {
        val order =
            listOf(times.fajr, times.sunrise, times.dhuhr, times.asr, times.sunset, times.maghrib, times.isha)
                .map { it?.value }
        val complete = order.all { it != null } && times.midnight != null
        val ascending = order.filterNotNull().zipWithNext().all { (a, b) -> a <= b }
        return if (complete && ascending) null else "$label: incomplete or unordered times $times"
    }

    private fun arcticOrderProblem(
        label: String,
        times: PrayerTimes,
    ): String? {
        // Near the midnight Sun the sunset falls after midnight and wraps to the next civil day, so only the times
        // that always exist are checked: MinuteOfDay keeps them within the day.
        val present =
            listOf(times.sunrise, times.dhuhr, times.asr, times.sunset).all {
                it.value in
                    0 until MINUTES_PER_DAY
            }
        return if (present) null else "$label: times outside the day $times"
    }

    private fun skyProblems(
        place: Place,
        day: Jdn,
    ): List<String> {
        val midnight = day.startIn(place.zone)
        val label = "${place.name} ${day.value}"
        val sun = Sky.riseSetTransit(CelestialBody.SUN, place.coordinates, midnight)
        val moon = Sky.riseSetTransit(CelestialBody.MOON, place.coordinates, midnight)
        val phase = Sky.moonPhaseDegrees(midnight)
        val fraction = Sky.moonAppearance(midnight, place.coordinates).illuminatedFraction
        val tithi = Tithi.at(midnight).number
        val hours = PlanetaryHours.forDay(place.coordinates, midnight, day.weekday())
        return listOfNotNull(
            "$label: sun rise/transit/set $sun".takeIf { sun.rise == null || sun.set == null || sun.transit == null },
            "$label: no moon rise, transit or set".takeIf {
                moon.rise == null && moon.transit == null &&
                    moon.set == null
            },
            "$label: moon phase $phase".takeIf { !phase.isFinite() || phase < 0.0 || phase >= FULL_TURN },
            "$label: illuminated fraction $fraction".takeIf { !fraction.isFinite() || fraction !in 0.0..1.0 },
            "$label: tithi $tithi".takeIf { tithi !in 1..TITHIS },
            "$label: moon constellation".takeIf { Zodiac.moonConstellation(midnight).isBlank() },
            "$label: planetary hours $hours".takeIf {
                (hours as? PlanetaryHoursResult.Available)?.hours?.size != HOURS_PER_DAY
            },
        ).also {
            // Tropical signs are enums; computing them for both bodies must not throw.
            Zodiac.tropicalSign(CelestialBody.SUN, midnight)
            Zodiac.tropicalSign(CelestialBody.MOON, midnight)
        }
    }

    /** In the Arctic the Sun may stay up or down, but then planetary hours must say so rather than fail. */
    private fun arcticProblems(day: Jdn): List<String> {
        val midnight = day.startIn(ARCTIC.zone)
        val sun = Sky.riseSetTransit(CelestialBody.SUN, ARCTIC.coordinates, midnight)
        val hours = PlanetaryHours.forDay(ARCTIC.coordinates, midnight, day.weekday())
        val consistent = hours is PlanetaryHoursResult.Unavailable || sun.rise != null
        return listOfNotNull("Arctic ${day.value}: $sun but $hours".takeIf { !consistent })
    }

    private fun yearProblems(year: Int): List<String> {
        val from = startOfGregorianYear(year)
        val until = startOfGregorianYear(year + 1)
        val seasons = Sky.seasons(year)
        val instants =
            listOf(seasons.marchEquinox, seasons.juneSolstice, seasons.septemberEquinox, seasons.decemberSolstice)
        val apsides = Sky.earthApsides(from, until)
        val quarters = Sky.moonQuarters(from, until)
        val solar = Eclipses.solarEclipses(from, until)
        val lunar = Eclipses.lunarEclipses(from, until)
        val newYear = GregorianCalendarSystem.fromJdn(ChineseNewYear.day(year))
        AnimalYear.forDate(newYear)
        return listOfNotNull(
            "$year: seasons $seasons".takeIf { instants != instants.sorted() || instants.any { it !in from..<until } },
            "$year: ${apsides.size} apsides".takeIf { apsides.size !in 1..APSIDES_MAX },
            "$year: ${quarters.size} moon quarters".takeIf { quarters.size !in QUARTERS_MIN..QUARTERS_MAX },
            "$year: ${solar.size} solar eclipses".takeIf { solar.size !in 2..ECLIPSES_MAX },
            "$year: ${lunar.size} lunar eclipses".takeIf { lunar.size !in 2..ECLIPSES_MAX },
            "$year: Chinese New Year $newYear".takeIf { newYear.year != year || newYear.month !in 1..2 },
        )
    }

    private fun scorpioAndTithiProblems(
        persianNewYear: Jdn,
        from: Instant,
        until: Instant,
    ): List<String> {
        val year = PersianCalendarSystem.fromJdn(persianNewYear).year
        val constellation = Zodiac.moonInScorpio(from, until, ZodiacSystem.IAU_CONSTELLATION)
        val tropical = Zodiac.moonInScorpio(from, until, ZodiacSystem.TROPICAL)
        val tithis = Tithi.changes(from, until).map { Tithi.at(it).number }.toSet()
        return listOfNotNull(
            "$year: ${constellation.size} Moon-in-Scorpius visits".takeIf { constellation.size < MIN_SCORPIO_VISITS },
            "$year: ${tropical.size} Moon-in-Scorpio visits".takeIf { tropical.size < MIN_SCORPIO_VISITS },
            "$year: tithis ${(1..TITHIS) - tithis} never begin".takeIf { tithis != (1..TITHIS).toSet() },
        )
    }

    private data class Place(
        val name: String,
        val coordinates: Coordinates,
        val zone: TimeZone,
        val settings: PrayerSettings = PrayerSettings(),
        val polar: Boolean = false,
    )

    private companion object {
        const val SECONDS_PER_MINUTE = 60
        const val MINUTES_PER_DAY = 1_440
        const val FULL_TURN = 360.0
        const val TITHIS = 30
        const val HOURS_PER_DAY = 24
        const val APSIDES_MAX = 3
        const val QUARTERS_MIN = 48
        const val QUARTERS_MAX = 52
        const val ECLIPSES_MAX = 5
        const val MIN_SCORPIO_VISITS = 12

        val TEHRAN = Place("Tehran", Coordinates(35.6892, 51.3890, 1_190.0), TimeZone.of("Asia/Tehran"))
        val KABUL = Place("Kabul", Coordinates(34.5553, 69.2075, 1_790.0), TimeZone.of("Asia/Kabul"))
        val ARCTIC =
            Place(
                "Tromsø",
                Coordinates(69.6492, 18.9553),
                TimeZone.of("Europe/Oslo"),
                PrayerSettings(highLatitude = HighLatitudeRule.NEAREST_LATITUDE),
                polar = true,
            )
        val PLACES = listOf(TEHRAN, KABUL, ARCTIC)

        fun persianNewYear(year: Int): Jdn =
            PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, year, 1, 1))

        fun gregorianYear(day: Jdn): Int = GregorianCalendarSystem.fromJdn(day).year

        fun startOfGregorianYear(year: Int): Instant =
            GregorianCalendarSystem.toJdn(CalendarDate(CalendarSystem.GREGORIAN, year, 1, 1)).startIn(TimeZone.UTC)

        fun Jdn.startIn(zone: TimeZone): Instant = toLocalDate().atStartOfDayIn(zone)
    }
}
