/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.i18n.FormatTable
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.i18n.monthName
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.MinuteOfDay
import ir.taqvim.core.praytimes.PrayerSettings
import ir.taqvim.core.praytimes.PrayerTimes
import ir.taqvim.core.praytimes.PrayerTimesCalculator
import ir.taqvim.core.praytimes.PrayerTimesResult
import kotlin.time.Instant
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** The primary prayer times the widgets show, in the order of the day. */
enum class WidgetPrayer {
    FAJR,
    SUNRISE,
    DHUHR,
    ASR,
    MAGHRIB,
    ISHA,
}

/** Localized names of [WidgetPrayer]s (string resources in the app language). */
fun interface WidgetPrayerNames {
    fun name(prayer: WidgetPrayer): String
}

/** The chosen place for prayer times: its coordinates, time zone and prayer conventions. */
data class WidgetPlace(
    val coordinates: Coordinates,
    val timeZone: TimeZone,
    val settings: PrayerSettings,
)

/**
 * Everything a widget shows for the civil day [jdn], before formatting: the language, the primary calendar and the
 * optional [secondary] one, the day's holiday flag and event lines, and the [place] for prayer times.
 */
data class WidgetDayInputs(
    val jdn: Jdn,
    val language: LanguageSpec,
    val primary: CalendarArithmetic,
    val secondary: CalendarArithmetic?,
    val isHoliday: Boolean,
    val events: List<WidgetEventLine>,
    val place: WidgetPlace?,
)

/** Builds the widgets' shared [WidgetData] (T-1201…T-1204) with every text in the app language and its digits. */
object WidgetContentBuilder {
    private const val TWO_DIGITS = 2

    /** The content of [inputs] at [now]; prayer names come from [names]. */
    fun build(
        inputs: WidgetDayInputs,
        now: Instant,
        names: WidgetPrayerNames,
    ): WidgetData {
        val language = inputs.language
        val date = inputs.primary.fromJdn(inputs.jdn)
        val weekdays = FormatTable.of(language).weekdays[date.system]
        val place = inputs.place
        val next = place?.let { WidgetPrayers.next(now, it) }
        val prayers = place?.let { prayerLines(inputs.jdn, it, next?.second, language, names) }.orEmpty()
        return WidgetData(
            date = inputs.jdn.toLocalDate(),
            dayNumber = digits(date.day.toString(), language),
            title = dayTitle(inputs.primary, inputs.jdn, language),
            weekday = weekdays?.getOrNull(inputs.jdn.weekday().ordinal).orEmpty(),
            secondaryDate = inputs.secondary?.let { dayTitle(it, inputs.jdn, language) },
            isHoliday = inputs.isHoliday,
            events = inputs.events.toImmutableList(),
            nextPrayer =
                next?.let { (prayer, at) ->
                    val local = WidgetPrayers.minuteOfDay(at, place.timeZone)
                    WidgetPrayerLine(names.name(prayer), clock(local, language), isNext = true)
                },
            prayers = prayers.toImmutableList(),
        )
    }

    /** "22 Shahrivar 1405": the day, the month name (or number where the language has none) and the year. */
    fun dayTitle(
        calendar: CalendarArithmetic,
        jdn: Jdn,
        language: LanguageSpec,
    ): String {
        val date = calendar.fromJdn(jdn)
        val month =
            FormatTable.of(language).monthName(date)
                ?: digits(date.month.toString(), language)
        return "${digits(date.day.toString(), language)} $month ${digits(date.year.toString(), language)}"
    }

    /** "HH:mm" of [minute] in the language's digits. */
    fun clock(
        minute: MinuteOfDay,
        language: LanguageSpec,
    ): String {
        val text = "${pad(minute.hour)}:${pad(minute.minute)}"
        return digits(text, language)
    }

    /** The day's times; the one at [next] (the next time, if it is today) is marked. */
    private fun prayerLines(
        jdn: Jdn,
        place: WidgetPlace,
        next: Instant?,
        language: LanguageSpec,
        names: WidgetPrayerNames,
    ): List<WidgetPrayerLine> =
        WidgetPrayers.moments(jdn, place).map { (prayer, at) ->
            val local = WidgetPrayers.minuteOfDay(at, place.timeZone)
            WidgetPrayerLine(names.name(prayer), clock(local, language), isNext = at == next)
        }

    private fun pad(value: Int): String = value.toString().padStart(TWO_DIGITS, '0')

    private fun digits(
        text: String,
        language: LanguageSpec,
    ): String = Numerals.localizeDigits(text, language.numerals)
}

/** Prayer times of the widgets' place as instants (A-10 through `:core:praytimes`). */
object WidgetPrayers {
    private const val NOON_HOUR = 12
    private const val SECONDS_PER_MINUTE = 60
    private const val MINUTES_PER_HOUR = 60

    /** The primary times of [day] at [place] in order; empty on polar days and nights. */
    fun moments(
        day: Jdn,
        place: WidgetPlace,
    ): List<Pair<WidgetPrayer, Instant>> {
        val times = times(day, place) ?: return emptyList()
        return primary(times).mapNotNull { (prayer, minute) -> minute?.let { prayer to instant(day, it, place) } }
    }

    /** Sunrise and sunset of [day] at [place], or `null` on polar days and nights (T-1209). */
    fun daylight(
        day: Jdn,
        place: WidgetPlace,
    ): Pair<Instant, Instant>? {
        val times = times(day, place) ?: return null
        return instant(day, times.sunrise, place) to instant(day, times.sunset, place)
    }

    private fun times(
        day: Jdn,
        place: WidgetPlace,
    ): PrayerTimes? {
        val noon = day.toLocalDate().atTime(NOON_HOUR, 0).toInstant(place.timeZone)
        val offsetMinutes = place.timeZone.offsetAt(noon).totalSeconds / SECONDS_PER_MINUTE
        val result = PrayerTimesCalculator.calculate(day, place.coordinates, offsetMinutes, place.settings)
        return when (result) {
            is PrayerTimesResult.Available -> result.times
            is PrayerTimesResult.Unavailable -> null
        }
    }

    private fun instant(
        day: Jdn,
        minute: MinuteOfDay,
        place: WidgetPlace,
    ): Instant = day.toLocalDate().atTime(minute.hour, minute.minute).toInstant(place.timeZone)

    /** The first primary time strictly after [now] (today's or tomorrow's), or `null` when none is defined. */
    fun next(
        now: Instant,
        place: WidgetPlace,
    ): Pair<WidgetPrayer, Instant>? {
        val today = now.toJdn(place.timeZone)
        return (0..1).asSequence().flatMap { moments(today + it, place) }.firstOrNull { it.second > now }
    }

    /** The local time of day of [at] in [zone]. */
    fun minuteOfDay(
        at: Instant,
        zone: TimeZone,
    ): MinuteOfDay {
        val time = at.toLocalDateTime(zone)
        return MinuteOfDay(time.hour * MINUTES_PER_HOUR + time.minute)
    }

    private fun primary(times: PrayerTimes): List<Pair<WidgetPrayer, MinuteOfDay?>> =
        listOf(
            WidgetPrayer.FAJR to times.fajr,
            WidgetPrayer.SUNRISE to times.sunrise,
            WidgetPrayer.DHUHR to times.dhuhr,
            WidgetPrayer.ASR to times.asr,
            WidgetPrayer.MAGHRIB to times.maghrib,
            WidgetPrayer.ISHA to times.isha,
        )
}
