/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.CalendarLimits
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.i18n.FormatTable
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.LocalDate

/** A unit of a countdown amount. */
enum class CountdownUnit {
    YEARS,
    MONTHS,
    WEEKS,
    DAYS,
}

/** An amount of a countdown: [count] of [unit], with [text] the count in the language's digits. */
data class CountdownPart(
    val unit: CountdownUnit,
    val count: Int,
    val text: String,
)

/**
 * A countdown as the widget shows it (T-1212), already localized: the [title] (the user's or the date), its [status],
 * the [headline] amount, the [parts] of the detail line (e.g. weeks and days left, or years, months and days since),
 * the [progress] of the ring and the civil [date] to open.
 */
data class WidgetCountdownView(
    val title: String,
    val status: CountdownStatus,
    val headline: CountdownPart,
    val parts: ImmutableList<CountdownPart>,
    val progress: Float,
    val date: LocalDate,
)

/**
 * An event offered as a countdown target (T-1212): its [title], date in its own [calendar], whether it
 * [repeatsYearly] on that date, and the [dateText] of its next occurrence.
 */
data class WidgetOccasion(
    val title: String,
    val calendar: CalendarSystem,
    val year: Int,
    val month: Int,
    val day: Int,
    val repeatsYearly: Boolean,
    val dateText: String,
)

/** What the countdown section of the configuration screen offers, localized. */
data class WidgetCountdownChoices(
    val calendars: ImmutableList<CalendarSystem>,
    val occasions: ImmutableList<WidgetOccasion>,
    val monthNames: ImmutableList<String>,
    val numerals: NumeralSystem,
    val years: IntRange,
    val dateText: String,
)

/**
 * What a countdown can be set to, from `:app`: [today], the [language], the [calendars] a date can be chosen in (the
 * first is the default) and upcoming [occasions].
 */
data class WidgetCountdownOptions(
    val today: Jdn,
    val language: LanguageSpec,
    val calendars: List<CalendarArithmetic>,
    val occasions: List<WidgetOccasion>,
) {
    init {
        require(calendars.isNotEmpty()) { "a countdown needs a calendar" }
    }

    /** The arithmetic of [system], or of the default calendar when [system] is not offered. */
    fun arithmetic(system: CalendarSystem): CalendarArithmetic =
        calendars.firstOrNull { it.system == system } ?: calendars.first()

    /** A countdown to today in the default calendar. */
    fun defaultCountdown(): WidgetCountdown {
        val date = calendars.first().fromJdn(today)
        return WidgetCountdown(date.system, date.year, date.month, date.day, startJdn = today.value)
    }

    /** [countdown]'s day in [system], or [countdown] unchanged when [system] is not offered. */
    fun inCalendar(
        countdown: WidgetCountdown,
        system: CalendarSystem,
    ): WidgetCountdown {
        val target = calendars.firstOrNull { it.system == system } ?: return countdown
        val jdn = arithmetic(countdown.calendar).let { it.toJdn(WidgetCountdownMath.origin(countdown, it)) }
        val date = target.fromJdn(jdn)
        return countdown.copy(calendar = system, year = date.year, month = date.month, day = date.day)
    }

    /** The screen's choices while editing [countdown]. */
    fun choices(countdown: WidgetCountdown): WidgetCountdownChoices {
        val calendar = arithmetic(countdown.calendar)
        val todayYear = calendar.fromJdn(today).year
        val origin = WidgetCountdownMath.origin(countdown.copy(calendar = calendar.system), calendar)
        val names =
            FormatTable.of(language).monthNames[calendar.system]
                ?: (
                    1..calendar.monthsInYear(
                        todayYear,
                    )
                ).map { Numerals.localizeDigits(it.toString(), language.numerals) }
        return WidgetCountdownChoices(
            calendars = calendars.map { it.system }.toImmutableList(),
            occasions = occasions.toImmutableList(),
            monthNames = names.toImmutableList(),
            numerals = language.numerals,
            years = CalendarLimits.years(calendar),
            dateText = WidgetContentBuilder.dayTitle(calendar, calendar.toJdn(origin), language),
        )
    }
}

/** Countdown targets offered by `:app` (the calendars and upcoming dataset events); bound in `:app`. */
fun interface WidgetCountdownSource {
    suspend fun options(): WidgetCountdownOptions
}

/** Builds the countdown widget's [WidgetCountdownView] (T-1212). */
object WidgetCountdownBuilder {
    private const val DAYS_PER_WEEK = 7

    /** [countdown] on [today] in [calendar] (its own calendar's arithmetic), localized for [language]. */
    fun view(
        countdown: WidgetCountdown,
        calendar: CalendarArithmetic,
        today: Jdn,
        language: LanguageSpec,
    ): WidgetCountdownView {
        val result = WidgetCountdownMath.evaluate(countdown, calendar, today)

        fun part(
            unit: CountdownUnit,
            count: Long,
        ): CountdownPart {
            val value = count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            return CountdownPart(unit, value, Numerals.localizeDigits(value.toString(), language.numerals))
        }
        val period = result.period
        val (headline, parts) =
            if (result.status == CountdownStatus.ELAPSED && period != null) {
                val all =
                    listOf(
                        part(CountdownUnit.YEARS, period.years.toLong()),
                        part(CountdownUnit.MONTHS, period.months.toLong()),
                        part(CountdownUnit.DAYS, period.days.toLong()),
                    )
                (all.firstOrNull { it.count > 0 } ?: all.last()) to all.filter { it.count > 0 }
            } else {
                val weeks = result.days / DAYS_PER_WEEK
                val split =
                    listOf(part(CountdownUnit.WEEKS, weeks), part(CountdownUnit.DAYS, result.days % DAYS_PER_WEEK))
                part(CountdownUnit.DAYS, result.days) to if (weeks > 0) split.filter { it.count > 0 } else emptyList()
            }
        return WidgetCountdownView(
            title = countdown.title.trim().ifEmpty { WidgetContentBuilder.dayTitle(calendar, result.target, language) },
            status = result.status,
            headline = headline,
            parts =
                if (parts.size > 1 ||
                    parts.singleOrNull() != headline
                ) {
                    parts.toImmutableList()
                } else {
                    persistentListOf()
                },
            progress = result.progress,
            date = result.target.toLocalDate(),
        )
    }
}
