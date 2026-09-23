/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.times

import ir.taqvim.core.astronomy.CelestialBody
import ir.taqvim.core.astronomy.Sky
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.i18n.DateFormatter
import ir.taqvim.core.i18n.DateStyle
import ir.taqvim.core.i18n.DurationFormatter
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.MinuteOfDay
import ir.taqvim.core.praytimes.PrayerTimes
import ir.taqvim.core.praytimes.PrayerTimesResult
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

/** Turns settings, the selected day and the current instant into [TimesUiState] (pure; T-1100). */
internal object TimesStateMapper {
    private const val MINUTES_PER_HOUR = 60
    private const val TWO_DIGITS = 2
    private const val FULL_TURN = 360.0
    private const val PHASE_SECTOR_DEGREES = FULL_TURN / 8
    private const val HALF_DAY_HOURS = 12
    private const val PERCENT = 100

    fun map(
        settings: TimesSettings?,
        dayOffset: Long,
        now: Instant,
        expanded: Boolean,
    ): TimesUiState {
        if (settings == null) return TimesUiState(TimesContent.NoLocation, expanded)
        val today = now.toJdn(settings.timeZone)
        return TimesUiState(day(settings, today + dayOffset, today, now, expanded), expanded)
    }

    private fun day(
        settings: TimesSettings,
        day: Jdn,
        today: Jdn,
        now: Instant,
        expanded: Boolean,
    ): TimesContent.Day {
        val date = settings.calendar.fromJdn(day)
        val title = DateFormatter.format(date, day.weekday(), settings.language, DateStyle.LONG)
        val isToday = day == today
        val empty =
            TimesContent.Day(settings.placeName, title, isToday, persistentListOf(), null, null, null, null)
        return when (val result = PrayerSchedule.calculate(day, settings)) {
            is PrayerTimesResult.Unavailable -> {
                empty.copy(unavailable = result.reason)
            }

            is PrayerTimesResult.Available -> {
                // Only the shown day may carry the countdown, but the time it counts to may fall on the next date:
                // after the day's last prayer the next one is tomorrow's Fajr, and requiring it to share the shown
                // day's date left the screen with no countdown and an empty arc from Isha until dawn.
                val next = PrayerSchedule.next(now, settings)?.takeIf { isToday }
                val numerals = settings.language.numerals
                val rows =
                    PrayerSchedule
                        .entries(result.times)
                        .filter { expanded || it.kind.primary }
                        .map { TimeRow(it.kind, it.time?.localized(numerals), it.kind == next?.kind) }
                empty.copy(
                    rows = rows.toImmutableList(),
                    next = next?.let { NextPrayerText(it.kind, remaining(it.remaining, settings), it.progress) },
                    sunPath = sunPath(result.times, if (isToday) now else null, settings),
                    moon = moon(day, settings),
                )
            }
        }
    }

    /**
     * The Moon of [day]: its phase and lit fraction at local noon, as the Calendars tab reads them, and the first
     * rise and set within that civil day. Either can be absent — the Moon rises about fifty minutes later each day,
     * so on one day of most months it does not rise (or does not set) between one midnight and the next.
     */
    private fun moon(
        day: Jdn,
        settings: TimesSettings,
    ): MoonSummary {
        val startOfDay = day.toLocalDate().atStartOfDayIn(settings.timeZone)
        val noon = startOfDay + HALF_DAY_HOURS.hours
        val appearance = Sky.moonAppearance(noon, settings.place)
        val riseSet = Sky.riseSetTransit(CelestialBody.MOON, settings.place, startOfDay)
        val numerals = settings.language.numerals
        return MoonSummary(
            phase = phaseName(Sky.moonPhaseDegrees(noon)),
            illuminatedFraction = appearance.illuminatedFraction.toFloat(),
            illuminatedPercent =
                Numerals.format((appearance.illuminatedFraction * PERCENT).roundToInt().toLong(), numerals),
            brightLimbOnRight = appearance.brightLimbOnRight,
            rise = riseSet.rise?.let { sameDay(it, day, settings.timeZone) }?.localized(numerals),
            set = riseSet.set?.let { sameDay(it, day, settings.timeZone) }?.localized(numerals),
        )
    }

    /** [instant] as a time of the shown [day], or `null` when the search ran past it into the next date. */
    private fun sameDay(
        instant: Instant,
        day: Jdn,
        zone: TimeZone,
    ): MinuteOfDay? =
        instant
            .toLocalDateTime(zone)
            .takeIf { instant.toJdn(zone) == day }
            ?.let { MinuteOfDay(it.hour * MINUTES_PER_HOUR + it.minute) }

    /** The phase name of elongation [degrees] (0 new, 90 first quarter, 180 full, 270 third quarter). */
    internal fun phaseName(degrees: Double): MoonPhaseName {
        val shifted = degrees + PHASE_SECTOR_DEGREES / 2
        val wrapped = shifted - FULL_TURN * floor(shifted / FULL_TURN)
        val index = (wrapped / PHASE_SECTOR_DEGREES).toInt().coerceAtMost(MoonPhaseName.entries.size - 1)
        return MoonPhaseName.entries[index]
    }

    private fun sunPath(
        times: PrayerTimes,
        now: Instant?,
        settings: TimesSettings,
    ): SunPath {
        val numerals = settings.language.numerals
        val progress =
            now?.let {
                val local = it.toLocalDateTime(settings.timeZone)
                val minute = local.hour * MINUTES_PER_HOUR + local.minute
                val span = times.sunset.value - times.sunrise.value
                if (minute in times.sunrise.value..times.sunset.value && span > 0) {
                    (minute - times.sunrise.value).toFloat() / span
                } else {
                    null
                }
            }
        return SunPath(progress, times.sunrise.localized(numerals), times.sunset.localized(numerals))
    }

    /** [duration] in words, or `H:MM` where the language has no duration patterns (docs/DATA_TODO.md DT-009). */
    private fun remaining(
        duration: Duration,
        settings: TimesSettings,
    ): String =
        DurationFormatter.format(duration, settings.language)
            ?: duration.inWholeMinutes.let { minutes ->
                val text =
                    "${minutes / MINUTES_PER_HOUR}:" +
                        (minutes % MINUTES_PER_HOUR).toString().padStart(TWO_DIGITS, '0')
                Numerals.localizeDigits(text, settings.language.numerals)
            }
}
