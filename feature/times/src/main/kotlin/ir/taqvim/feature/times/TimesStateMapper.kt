/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.times

import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.i18n.DateFormatter
import ir.taqvim.core.i18n.DateStyle
import ir.taqvim.core.i18n.DurationFormatter
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.praytimes.PrayerTimes
import ir.taqvim.core.praytimes.PrayerTimesResult
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.toLocalDateTime

/** Turns settings, the selected day and the current instant into [TimesUiState] (pure; T-1100). */
internal object TimesStateMapper {
    private const val MINUTES_PER_HOUR = 60
    private const val TWO_DIGITS = 2

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
        val empty = TimesContent.Day(settings.placeName, title, isToday, persistentListOf(), null, null, null)
        return when (val result = PrayerSchedule.calculate(day, settings)) {
            is PrayerTimesResult.Unavailable -> {
                empty.copy(unavailable = result.reason)
            }

            is PrayerTimesResult.Available -> {
                val next =
                    PrayerSchedule
                        .next(now, settings)
                        ?.takeIf { isToday && it.at.toJdn(settings.timeZone) == day }
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
                )
            }
        }
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
