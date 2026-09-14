/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.addMonths
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.i18n.FormatTable
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.core.model.Weekday
import kotlin.time.Instant
import kotlinx.collections.immutable.toImmutableList

/** What the calendar widgets are built from: today, the language, both calendars and the week start. */
data class WidgetCalendarInputs(
    val today: Jdn,
    val language: LanguageSpec,
    val primary: CalendarArithmetic,
    val secondary: CalendarArithmetic?,
    val weekStart: Weekday,
)

/**
 * Builds the content of the month, week, schedule and sun widgets (T-1205…T-1209) with every text in the app language
 * and its digits. The ranges tell `:app` which days to load from the events repository.
 */
object WidgetCalendarBuilder {
    const val GRID_DAYS: Int = 42
    const val WEEK_DAYS: Int = 7
    const val SCHEDULE_DAYS: Int = 14

    /** Month navigation stops a century either way. */
    const val MAX_MONTH_OFFSET: Int = 1200

    /** The first day of the month [offset] months from today's month in the primary calendar. */
    fun monthStart(
        inputs: WidgetCalendarInputs,
        offset: Int,
    ): Jdn {
        val calendar = inputs.primary
        val today = calendar.fromJdn(inputs.today)
        val first = calendar.date(today.year, today.month, 1)
        return calendar.toJdn(calendar.addMonths(first, clampOffset(offset)))
    }

    /** The six weeks of the month page [offset] months away, from the week start on or before its first day. */
    fun monthRange(
        inputs: WidgetCalendarInputs,
        offset: Int,
    ): JdnRange {
        val start = startOfWeek(monthStart(inputs, offset), inputs.weekStart)
        return start..(start + (GRID_DAYS - 1))
    }

    /** The week containing today, from the week start. */
    fun weekRange(inputs: WidgetCalendarInputs): JdnRange {
        val start = startOfWeek(inputs.today, inputs.weekStart)
        return start..(start + (WEEK_DAYS - 1))
    }

    /** Today and the following days of the schedule. */
    fun scheduleRange(inputs: WidgetCalendarInputs): JdnRange = inputs.today..(inputs.today + (SCHEDULE_DAYS - 1))

    /** The [weekStart] day on or before [day]. */
    fun startOfWeek(
        day: Jdn,
        weekStart: Weekday,
    ): Jdn = day - Math.floorMod(day.weekday().ordinal - weekStart.ordinal, WEEK_DAYS)

    fun clampOffset(offset: Int): Int = offset.coerceIn(-MAX_MONTH_OFFSET, MAX_MONTH_OFFSET)

    /** The month page [offset] months from today's month; [facts] gives each day's holidays and events. */
    fun month(
        inputs: WidgetCalendarInputs,
        offset: Int,
        facts: (Jdn) -> WidgetDayFacts,
    ): WidgetMonth {
        val first = monthStart(inputs, offset)
        val shown = inputs.primary.fromJdn(first)
        val last = first + (inputs.primary.monthLength(shown.year, shown.month) - 1)
        val days =
            monthRange(inputs, offset).map { jdn ->
                val date = inputs.primary.fromJdn(jdn)
                day(inputs, jdn, facts(jdn), inMonth = date.year == shown.year && date.month == shown.month)
            }
        return WidgetMonth(
            title = monthTitle(shown, inputs.language),
            secondaryTitle = inputs.secondary?.let { spanTitle(it.fromJdn(first), it.fromJdn(last), inputs.language) },
            weekdayLabels = weekdayLabels(inputs).toImmutableList(),
            days = days.toImmutableList(),
            offset = clampOffset(offset),
            firstDay = first.toLocalDate(),
        )
    }

    /** The seven days of today's week. */
    fun week(
        inputs: WidgetCalendarInputs,
        facts: (Jdn) -> WidgetDayFacts,
    ): List<WidgetCalendarDay> = weekRange(inputs).map { day(inputs, it, facts(it), inMonth = true) }

    /** The schedule: today and every following day of [SCHEDULE_DAYS] that has events. */
    fun schedule(
        inputs: WidgetCalendarInputs,
        facts: (Jdn) -> WidgetDayFacts,
    ): List<WidgetScheduleDay> =
        scheduleRange(inputs).mapNotNull { jdn ->
            val dayFacts = facts(jdn)
            if (dayFacts.events.isEmpty() && jdn != inputs.today) return@mapNotNull null
            WidgetScheduleDay(
                date = jdn.toLocalDate(),
                title = "${weekdayName(inputs, jdn)} ${dayTitle(inputs, jdn)}",
                isToday = jdn == inputs.today,
                isHoliday = dayFacts.isHoliday,
                events = dayFacts.events.toImmutableList(),
            )
        }

    /** Today's sunrise, sunset and daylight progress at [place], or `null` on polar days and nights. */
    fun sun(
        now: Instant,
        place: WidgetPlace,
        language: LanguageSpec,
    ): WidgetSun? {
        val (sunrise, sunset) = WidgetPrayers.daylight(now.toJdn(place.timeZone), place) ?: return null
        val progress = if (now in sunrise..sunset) ((now - sunrise) / (sunset - sunrise)).toFloat() else null

        fun clock(at: Instant) = WidgetContentBuilder.clock(WidgetPrayers.minuteOfDay(at, place.timeZone), language)
        return WidgetSun(clock(sunrise), clock(sunset), progress)
    }

    private fun day(
        inputs: WidgetCalendarInputs,
        jdn: Jdn,
        facts: WidgetDayFacts,
        inMonth: Boolean,
    ): WidgetCalendarDay =
        WidgetCalendarDay(
            date = jdn.toLocalDate(),
            dayLabel = digits(inputs.primary.fromJdn(jdn).day, inputs.language),
            weekdayLabel = narrow(weekdayName(inputs, jdn)),
            secondaryLabel = inputs.secondary?.let { digits(it.fromJdn(jdn).day, inputs.language) },
            isToday = jdn == inputs.today,
            isHoliday = facts.isHoliday,
            isWeekend = facts.isWeekend,
            inMonth = inMonth,
            eventCount = facts.events.size,
            description = dayTitle(inputs, jdn),
        )

    private fun weekdayLabels(inputs: WidgetCalendarInputs): List<String> {
        val names = FormatTable.of(inputs.language).weekdays[inputs.primary.system].orEmpty()
        return (0 until WEEK_DAYS).map { narrow(names.getOrNull((inputs.weekStart.ordinal + it) % WEEK_DAYS)) }
    }

    private fun dayTitle(
        inputs: WidgetCalendarInputs,
        jdn: Jdn,
    ): String = WidgetContentBuilder.dayTitle(inputs.primary, jdn, inputs.language)

    private fun weekdayName(
        inputs: WidgetCalendarInputs,
        jdn: Jdn,
    ): String =
        FormatTable
            .of(inputs.language)
            .weekdays[inputs.primary.system]
            ?.getOrNull(jdn.weekday().ordinal)
            .orEmpty()

    /** "Shahrivar 1405". */
    private fun monthTitle(
        date: CalendarDate,
        language: LanguageSpec,
    ): String = "${monthName(date, language)} ${digits(date.year, language)}"

    /** "Shahrivar 1405", "Mordad – Shahrivar 1405" or "Esfand 1404 – Farvardin 1405". */
    private fun spanTitle(
        from: CalendarDate,
        to: CalendarDate,
        language: LanguageSpec,
    ): String =
        when {
            from.year == to.year && from.month == to.month -> monthTitle(from, language)
            from.year == to.year -> "${monthName(from, language)} – ${monthTitle(to, language)}"
            else -> "${monthTitle(from, language)} – ${monthTitle(to, language)}"
        }

    private fun monthName(
        date: CalendarDate,
        language: LanguageSpec,
    ): String =
        FormatTable.of(language).monthNames[date.system]?.getOrNull(date.month - 1) ?: digits(date.month, language)

    /** The first letter of [name], as calendars print weekday column heads. */
    private fun narrow(name: String?): String =
        if (name.isNullOrEmpty()) "" else name.substring(0, name.offsetByCodePoints(0, 1))

    private fun digits(
        value: Int,
        language: LanguageSpec,
    ): String = Numerals.localizeDigits(value.toString(), language.numerals)
}
