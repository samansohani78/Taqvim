/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.originOf
import ir.taqvim.core.calendar.periodBetween
import ir.taqvim.core.i18n.DateFormatter
import ir.taqvim.core.i18n.DateStyle
import ir.taqvim.core.i18n.DurationFormatter
import ir.taqvim.core.i18n.DurationUnit
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.nlp.DateParser
import ir.taqvim.core.nlp.ParseContext
import java.math.BigDecimal
import kotlin.math.abs
import kotlinx.collections.immutable.toImmutableList

/** The date converter and the day-distance tool (T-1400) on the NLP parser (T-500) and the calendars (T-101…T-106). */
internal object DateTools {
    /** Longest distance whose workdays are counted, about ten years; longer ranges would be slow to scan. */
    const val MAX_WORKDAY_SPAN_DAYS: Long = 3_660
    private const val DAYS_PER_WEEK = 7

    /** What [input] means, or today when it is blank. */
    fun convert(
        input: String,
        today: Jdn,
        settings: ToolsSettings,
    ): ConverterResult {
        if (input.isBlank()) return ConverterResult.Converted(isToday = true, dates = describe(today, settings))
        val reading = DateParser.parseBest(input, context(settings, today))
        return reading?.let { ConverterResult.Converted(isToday = false, dates = describe(it.jdn, settings)) }
            ?: ConverterResult.NotRecognized
    }

    /** The distance between the dates written in [from] and [to]. */
    fun distance(
        from: String,
        to: String,
        today: Jdn,
        settings: ToolsSettings,
    ): DistanceState {
        val context = context(settings, today)
        val first = from.takeUnless { it.isBlank() }?.let { DateParser.parseBest(it, context) }
        val second = to.takeUnless { it.isBlank() }?.let { DateParser.parseBest(it, context) }
        val result =
            if (first != null && second != null) measure(first.jdn, second.jdn, settings) else null
        return DistanceState(
            from = end(from, first?.jdn, settings),
            to = end(to, second?.jdn, settings),
            result = result,
        )
    }

    /** [jdn] in every calendar of [settings]. */
    fun describe(
        jdn: Jdn,
        settings: ToolsSettings,
    ) = settings.calendars.map { describe(jdn, it, settings) }.toImmutableList()

    private fun describe(
        jdn: Jdn,
        calendar: CalendarArithmetic,
        settings: ToolsSettings,
    ): ConvertedDate {
        val date = calendar.fromJdn(jdn)

        fun text(style: DateStyle) = DateFormatter.format(date, jdn.weekday(), settings.language, style)
        return ConvertedDate(
            calendar.system,
            text(DateStyle.LONG),
            text(DateStyle.NUMERIC),
            text(DateStyle.ISO),
            calendar.originOf(jdn),
        )
    }

    private fun context(
        settings: ToolsSettings,
        today: Jdn,
    ): ParseContext {
        val primary = settings.calendars.first()
        return ParseContext.forLanguage(settings.language, today, primary.system, settings.anchors).copy(
            calendars = ParseContext.DEFAULT_CALENDARS + settings.calendars.associateBy { it.system },
        )
    }

    private fun end(
        input: String,
        jdn: Jdn?,
        settings: ToolsSettings,
    ): DistanceEnd =
        when {
            input.isBlank() -> DistanceEnd.Empty
            jdn == null -> DistanceEnd.NotRecognized
            else -> DistanceEnd.Recognized(describe(jdn, settings.calendars.first(), settings).long)
        }

    private fun measure(
        from: Jdn,
        to: Jdn,
        settings: ToolsSettings,
    ): DistanceResult {
        val numerals = settings.language.numerals
        val days = to - from
        val span = abs(days)
        val calendar = settings.calendars.first()
        val (earlier, later) = if (days >= 0) from to to else to to from
        val period = calendar.periodBetween(calendar.fromJdn(earlier), calendar.fromJdn(later))

        fun number(value: Long) = Numerals.format(value, numerals)
        return DistanceResult(
            isBackward = days < 0,
            days = number(span),
            daysText = DurationFormatter.format(mapOf(DurationUnit.DAY to span), settings.language),
            weeks = number(span / DAYS_PER_WEEK),
            weekDays = number(span % DAYS_PER_WEEK),
            calendar = calendar.system,
            years = number(period.years.toLong()),
            months = number(period.months.toLong()),
            monthDays = number(period.days.toLong()),
            workdays = workdays(earlier, later, settings),
        )
    }

    private fun workdays(
        earlier: Jdn,
        later: Jdn,
        settings: ToolsSettings,
    ): WorkdaysText {
        val calculator = settings.workdays
        return when {
            calculator == null -> {
                WorkdaysText.NotConfigured
            }

            later - earlier > MAX_WORKDAY_SPAN_DAYS -> {
                WorkdaysText.TooLong
            }

            else -> {
                val value = BigDecimal.valueOf(calculator.workdaysBetween(earlier, later)).stripTrailingZeros()
                WorkdaysText.Count(Numerals.format(value, settings.language.numerals))
            }
        }
    }
}
