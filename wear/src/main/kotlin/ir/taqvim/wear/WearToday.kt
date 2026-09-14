/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.wear

import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.events.EventDefinition
import ir.taqvim.core.events.EventLookup
import ir.taqvim.core.events.IslamicCalendarSelection
import ir.taqvim.core.i18n.DateFormatter
import ir.taqvim.core.i18n.DateStyle
import ir.taqvim.core.i18n.FormatTable
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.MinuteOfDay
import ir.taqvim.core.praytimes.PrayerTimes
import ir.taqvim.core.praytimes.PrayerTimesCalculator
import ir.taqvim.core.praytimes.PrayerTimesResult
import java.util.Locale
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** The prayers a watch shows as "next prayer". */
enum class WearPrayer {
    FAJR,
    DHUHR,
    ASR,
    MAGHRIB,
    ISHA,
}

/** The next prayer after now: which one, when, and its clock time in the user's digits. */
data class NextPrayer(
    val prayer: WearPrayer,
    val at: Instant,
    val clock: String,
)

/** The next official occasion from today: its title, how many days away (0 = today) and whether it is a holiday. */
data class NextEvent(
    val title: String,
    val daysAway: Int,
    val isHoliday: Boolean,
)

/** Everything the watch shows about today (T-1600). */
data class WearToday(
    val jdn: Jdn,
    val primaryDate: String,
    val dayLabel: String,
    val monthName: String,
    val secondaryDates: List<String>,
    val dayOfMonth: Int,
    val monthLength: Int,
    val holidays: List<String>,
    val events: List<String>,
    val nextPrayer: NextPrayer?,
    val nextEvent: NextEvent?,
) {
    /** Share of the primary month that has passed, counting today (`dayOfMonth / monthLength`). */
    val monthProgress: Float
        get() = dayOfMonth.toFloat() / monthLength
}

/** Computes [WearToday] from the watch setup and the current instant, with official events of [definitions]. */
class WearDayCalculator(
    private val definitions: List<EventDefinition>,
) {
    private val lookups = LookupCache(definitions)

    /** The official events lookup whose Islamic dates follow [variant]. */
    fun lookup(variant: IslamicVariant): EventLookup = lookups.forVariant(variant)

    fun today(
        setup: WearSetup,
        now: Instant,
    ): WearToday {
        val jdn = now.toJdn(setup.zone)
        val date = setup.primary.fromJdn(jdn)
        val lookup = lookup(setup.islamicVariant)
        val occurrences = lookup.eventsOn(jdn, setup.enabledSources)
        return WearToday(
            jdn = jdn,
            primaryDate = DateFormatter.format(date, jdn.weekday(), setup.language, DateStyle.LONG, setup.numerals),
            dayLabel = digits(date.day.toString(), setup),
            monthName = monthName(date, setup),
            secondaryDates =
                setup.calendars.drop(1).map { calendar ->
                    DateFormatter.format(
                        calendar.fromJdn(jdn),
                        jdn.weekday(),
                        setup.language,
                        DateStyle.NUMERIC,
                        setup.numerals,
                    )
                },
            dayOfMonth = date.day,
            monthLength = setup.primary.monthLength(date.year, date.month),
            holidays = occurrences.filter { it.isHoliday }.map { it.definition.title.forLanguage(setup.language.code) },
            events =
                occurrences.filterNot { it.isHoliday }.map {
                    it.definition.title.forLanguage(
                        setup.language.code,
                    )
                },
            nextPrayer = nextPrayer(setup, jdn, now),
            nextEvent = nextEvent(lookup, setup, jdn),
        )
    }

    private fun nextPrayer(
        setup: WearSetup,
        today: Jdn,
        now: Instant,
    ): NextPrayer? {
        val place = setup.place ?: return null
        return (0..1)
            .asSequence()
            .flatMap { offset ->
                val day = today + offset
                prayersOn(day, setup, place).map { (prayer, minute) -> prayer to instantOf(day, minute, setup.zone) }
            }.firstOrNull { (_, at) -> at > now }
            ?.let { (prayer, at) ->
                val time = at.toLocalDateTime(setup.zone)
                NextPrayer(prayer, at, digits(String.format(Locale.ROOT, CLOCK, time.hour, time.minute), setup))
            }
    }

    private fun prayersOn(
        day: Jdn,
        setup: WearSetup,
        place: WearPlace,
    ): List<Pair<WearPrayer, MinuteOfDay>> {
        val noon = instantOf(day, MinuteOfDay(NOON_MINUTE), setup.zone)
        val offsetMinutes = setup.zone.offsetAt(noon).totalSeconds / SECONDS_PER_MINUTE
        val result = PrayerTimesCalculator.calculate(day, place.coordinates, offsetMinutes, setup.prayer)
        val times = (result as? PrayerTimesResult.Available)?.times ?: return emptyList()
        return times.ordered()
    }

    private fun nextEvent(
        lookup: EventLookup,
        setup: WearSetup,
        today: Jdn,
    ): NextEvent? =
        (0..EVENT_HORIZON_DAYS).firstNotNullOfOrNull { offset ->
            lookup.eventsOn(today + offset, setup.enabledSources).firstOrNull()?.let { occurrence ->
                NextEvent(occurrence.definition.title.forLanguage(setup.language.code), offset, occurrence.isHoliday)
            }
        }

    private companion object {
        const val CLOCK = "%02d:%02d"
        const val NOON_MINUTE = 12 * 60
        const val SECONDS_PER_MINUTE = 60
        const val EVENT_HORIZON_DAYS = 60
    }
}

/** [text] with ASCII digits written in the user's numeral system. */
internal fun digits(
    text: String,
    setup: WearSetup,
): String = Numerals.localizeDigits(text, setup.numerals)

/** The month name of [date] in the setup's language, or its number when the language has no names for it. */
internal fun monthName(
    date: CalendarDate,
    setup: WearSetup,
): String =
    FormatTable.of(setup.language).monthNames[date.system]?.getOrNull(date.month - 1)
        ?: digits(date.month.toString(), setup)

private fun PrayerTimes.ordered(): List<Pair<WearPrayer, MinuteOfDay>> =
    listOfNotNull(
        fajr?.let { WearPrayer.FAJR to it },
        WearPrayer.DHUHR to dhuhr,
        WearPrayer.ASR to asr,
        maghrib?.let { WearPrayer.MAGHRIB to it },
        isha?.let { WearPrayer.ISHA to it },
    )

private fun instantOf(
    day: Jdn,
    minute: MinuteOfDay,
    zone: TimeZone,
): Instant = LocalDateTime(day.toLocalDate(), LocalTime(minute.hour, minute.value % MINUTES_PER_HOUR)).toInstant(zone)

private const val MINUTES_PER_HOUR = 60

/** One events lookup per Islamic variant, rebuilt only when the variant changes. */
private class LookupCache(
    private val definitions: List<EventDefinition>,
) {
    @Volatile private var current: Pair<IslamicVariant, EventLookup>? = null

    fun forVariant(variant: IslamicVariant): EventLookup {
        current?.takeIf { it.first == variant }?.let { return it.second }
        val lookup = EventLookup(definitions, IslamicCalendarSelection(variant))
        current = variant to lookup
        return lookup
    }
}
