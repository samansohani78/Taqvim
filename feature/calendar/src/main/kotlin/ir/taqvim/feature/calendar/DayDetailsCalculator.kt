/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import ir.taqvim.core.astronomy.CelestialBody
import ir.taqvim.core.astronomy.Sky
import ir.taqvim.core.astronomy.Zodiac
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.periodBetween
import ir.taqvim.core.calendar.positionInSeason
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.MinuteOfDay
import ir.taqvim.core.praytimes.PrayerTimes
import ir.taqvim.core.praytimes.PrayerTimesCalculator
import ir.taqvim.core.praytimes.PrayerTimesResult
import kotlin.math.floor
import kotlin.time.Instant
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant

/** The content of the Calendars and Times tabs of a day (T-802), meant to run off the main thread. */
object DayDetailsCalculator {
    private const val NOON_HOUR = 12
    private const val SECONDS_PER_MINUTE = 60
    private const val PHASE_SECTOR_DEGREES = 45.0
    private const val FULL_TURN = 360.0
    private const val OPPOSITE_SEASON = 2

    /** Observer used for the Moon without a chosen place: the northern-hemisphere view. */
    private val NORTHERN_OBSERVER = Coordinates(0.0, 0.0)

    /** [day] relative to [today], in its week and season, and its Sun and Moon at noon ([place]'s noon, else UTC). */
    fun overview(
        day: Jdn,
        today: Jdn,
        calendars: CalendarCalendars,
        place: CalendarPlace?,
    ): DayOverview {
        val primary = calendars.arithmetic.first()
        val weekStart = calendars.settings.weekStart
        val noon = noon(day, place?.timeZone ?: TimeZone.UTC)
        // Persian seasons coincide with the astronomical ones (Farvardin–Khordad is the northern spring).
        val season = PersianCalendarSystem.positionInSeason(PersianCalendarSystem.fromJdn(day))
        val southern = place != null && place.coordinates.latitude < 0
        val seasonIndex = if (southern) (season.season + OPPOSITE_SEASON) % SeasonName.entries.size else season.season
        return DayOverview(
            day = day,
            daysFromToday = day - today,
            period = primary.periodBetween(primary.fromJdn(today), primary.fromJdn(day)),
            dayOfWeek = day.weekday().daysAfter(weekStart) + 1,
            weekOfYear = MonthLayout.weekOfYear(day, primary, weekStart),
            season = SeasonName.entries[seasonIndex],
            dayOfSeason = season.dayOfSeason,
            seasonLength = season.seasonLength,
            sunSign = Zodiac.tropicalSign(CelestialBody.SUN, noon),
            moon = moon(noon, place?.coordinates ?: NORTHERN_OBSERVER),
        )
    }

    /** Times of [day] at [place]; the next time and the Sun's progress only when [now] falls on [day] there. */
    fun times(
        day: Jdn,
        place: CalendarPlace,
        now: Instant,
    ): DayTimes {
        val zone = place.timeZone
        val offsetMinutes = zone.offsetAt(noon(day, zone)).totalSeconds / SECONDS_PER_MINUTE
        val empty = DayTimes(day, place.name, place.prayer.method, persistentListOf(), null, null, null)
        return when (
            val result =
                PrayerTimesCalculator.calculate(
                    day,
                    place.coordinates,
                    offsetMinutes,
                    place.prayer,
                )
        ) {
            is PrayerTimesResult.Unavailable -> {
                empty.copy(unavailable = result.reason)
            }

            is PrayerTimesResult.Available -> {
                val entries = entries(result.times)
                val isToday = now.toJdn(zone) == day
                empty.copy(
                    entries = entries.toImmutableList(),
                    next = if (isToday) next(day, entries, zone, now) else null,
                    sunProgress = if (isToday) sunProgress(day, result.times, zone, now) else null,
                )
            }
        }
    }

    /** The phase name of elongation [degrees] (0 new, 90 first quarter, 180 full, 270 third quarter). */
    fun phaseName(degrees: Double): MoonPhaseName {
        val shifted = degrees + PHASE_SECTOR_DEGREES / 2
        val wrapped = shifted - FULL_TURN * floor(shifted / FULL_TURN)
        val index = (wrapped / PHASE_SECTOR_DEGREES).toInt().coerceAtMost(MoonPhaseName.entries.size - 1)
        return MoonPhaseName.entries[index]
    }

    private fun moon(
        instant: Instant,
        observer: Coordinates,
    ): MoonOverview {
        val appearance = Sky.moonAppearance(instant, observer)
        return MoonOverview(
            illuminatedFraction = appearance.illuminatedFraction.toFloat(),
            brightLimbOnRight = appearance.brightLimbOnRight,
            phase = phaseName(Sky.moonPhaseDegrees(instant)),
            sign = Zodiac.tropicalSign(CelestialBody.MOON, instant),
        )
    }

    private fun entries(times: PrayerTimes): List<PrayerTimeEntry> =
        listOf(
            PrayerTimeEntry(PrayerTimeKind.FAJR, times.fajr),
            PrayerTimeEntry(PrayerTimeKind.SUNRISE, times.sunrise),
            PrayerTimeEntry(PrayerTimeKind.DHUHR, times.dhuhr),
            PrayerTimeEntry(PrayerTimeKind.ASR, times.asr),
            PrayerTimeEntry(PrayerTimeKind.SUNSET, times.sunset),
            PrayerTimeEntry(PrayerTimeKind.MAGHRIB, times.maghrib),
            PrayerTimeEntry(PrayerTimeKind.ISHA, times.isha),
            PrayerTimeEntry(PrayerTimeKind.MIDNIGHT, times.midnight),
        )

    /** The first primary time of [day] after [now]; times past local midnight are not considered. */
    private fun next(
        day: Jdn,
        entries: List<PrayerTimeEntry>,
        zone: TimeZone,
        now: Instant,
    ): PrayerTimeKind? =
        entries
            .filter { it.kind.primary }
            .firstOrNull { entry -> entry.time?.let { instant(day, it, zone) > now } == true }
            ?.kind

    private fun sunProgress(
        day: Jdn,
        times: PrayerTimes,
        zone: TimeZone,
        now: Instant,
    ): Float? {
        val sunrise = instant(day, times.sunrise, zone)
        val sunset = instant(day, times.sunset, zone)
        return if (sunset > sunrise &&
            now in sunrise..sunset
        ) {
            ((now - sunrise) / (sunset - sunrise)).toFloat()
        } else {
            null
        }
    }

    private fun instant(
        day: Jdn,
        time: MinuteOfDay,
        zone: TimeZone,
    ): Instant = day.toLocalDate().atTime(time.hour, time.minute).toInstant(zone)

    private fun noon(
        day: Jdn,
        zone: TimeZone,
    ): Instant = day.toLocalDate().atTime(NOON_HOUR, 0).toInstant(zone)
}
