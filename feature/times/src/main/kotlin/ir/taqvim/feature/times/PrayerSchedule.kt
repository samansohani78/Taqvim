/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.times

import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.MinuteOfDay
import ir.taqvim.core.praytimes.PrayerTimes
import ir.taqvim.core.praytimes.PrayerTimesCalculator
import ir.taqvim.core.praytimes.PrayerTimesResult
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.datetime.atTime
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant

/** The times of a day, in display order; [primary] ones are shown when the list is collapsed. */
enum class PrayerKind(
    val primary: Boolean,
) {
    FAJR(true),
    SUNRISE(true),
    DHUHR(true),
    ASR(true),
    SUNSET(false),
    MAGHRIB(true),
    ISHA(true),
    MIDNIGHT(false),
}

/** One time of a day; [time] is `null` where the chosen rules leave it undefined. */
data class PrayerEntry(
    val kind: PrayerKind,
    val time: MinuteOfDay?,
)

/** The next primary time after "now": when it is, how long until then and the elapsed part of the interval. */
data class NextPrayer(
    val kind: PrayerKind,
    val at: Instant,
    val remaining: Duration,
    /** Elapsed fraction (0‥1) of the interval from the previous primary time to [at]. */
    val progress: Float,
)

/** Prayer times of the chosen place for civil days in its time zone (T-1100 on A-10). */
object PrayerSchedule {
    private const val NOON_HOUR = 12
    private const val SECONDS_PER_MINUTE = 60

    /** Offset of the place's clocks from UTC at local noon of [day], in minutes. */
    fun utcOffsetMinutes(
        day: Jdn,
        settings: TimesSettings,
    ): Int {
        val noon = day.toLocalDate().atTime(NOON_HOUR, 0).toInstant(settings.timeZone)
        return settings.timeZone.offsetAt(noon).totalSeconds / SECONDS_PER_MINUTE
    }

    /** Times of [day] at the place. */
    fun calculate(
        day: Jdn,
        settings: TimesSettings,
    ): PrayerTimesResult =
        PrayerTimesCalculator.calculate(day, settings.place, utcOffsetMinutes(day, settings), settings.prayer)

    /** Every time of [times] in display order. */
    fun entries(times: PrayerTimes): List<PrayerEntry> =
        listOf(
            PrayerEntry(PrayerKind.FAJR, times.fajr),
            PrayerEntry(PrayerKind.SUNRISE, times.sunrise),
            PrayerEntry(PrayerKind.DHUHR, times.dhuhr),
            PrayerEntry(PrayerKind.ASR, times.asr),
            PrayerEntry(PrayerKind.SUNSET, times.sunset),
            PrayerEntry(PrayerKind.MAGHRIB, times.maghrib),
            PrayerEntry(PrayerKind.ISHA, times.isha),
            PrayerEntry(PrayerKind.MIDNIGHT, times.midnight),
        )

    /** The next primary time after [now], or `null` when none is defined around it (polar days and nights). */
    fun next(
        now: Instant,
        settings: TimesSettings,
    ): NextPrayer? {
        val today = now.toJdn(settings.timeZone)
        val moments = (-1..1).flatMap { offset -> moments(today + offset, settings) }
        val index = moments.indexOfFirst { it.second > now }
        if (index < 1) return null
        val (kind, at) = moments[index]
        val previous = moments[index - 1].second
        return NextPrayer(kind, at, at - now, ((now - previous) / (at - previous)).toFloat())
    }

    /** Primary times of [day] as instants, strictly increasing (times after midnight belong to the next date). */
    private fun moments(
        day: Jdn,
        settings: TimesSettings,
    ): List<Pair<PrayerKind, Instant>> {
        val times =
            when (val result = calculate(day, settings)) {
                is PrayerTimesResult.Available -> result.times
                is PrayerTimesResult.Unavailable -> return emptyList()
            }
        val date = day.toLocalDate()
        val moments = mutableListOf<Pair<PrayerKind, Instant>>()
        entries(times).filter { it.kind.primary }.forEach { entry ->
            val time = entry.time ?: return@forEach
            val instant = date.atTime(time.hour, time.minute).toInstant(settings.timeZone)
            val last = moments.lastOrNull()?.second
            moments += entry.kind to if (last != null && instant <= last) instant + 1.days else instant
        }
        return moments
    }
}

/** `HH:mm` with the digits of [numerals]. */
internal fun MinuteOfDay.localized(numerals: NumeralSystem): String {
    val text = hour.toString().padStart(TWO_DIGITS_PAD, '0') + ":" + minute.toString().padStart(TWO_DIGITS_PAD, '0')
    return Numerals.localizeDigits(text, numerals)
}

private const val TWO_DIGITS_PAD = 2
