/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeInRange
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.NepaliLunarDays
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.events.EventDefinition
import ir.taqvim.core.events.EventRule
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.events.IslamicCalendarSelection
import ir.taqvim.core.events.OccurrenceCalculator
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.JdnRange
import ir.taqvim.data.events.generated.OfficialEvents
import ir.taqvim.data.preferences.UserPreferences
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.junit.jupiter.api.Test

/**
 * Owner directive 2026-09-17 ("computed, not typed"): the calendar is complete for every day of 1380–1480 SH — every
 * calendar system and Islamic variant converts every day and back, every dataset rule occurs in every year it can, and
 * the day assembler resolves every day under the default settings of every one of the 24 launch languages and with every
 * source on. Prayer times and astronomy are covered by the app's `SkyAndTimesCompletenessTest`.
 *
 * Required and optional day fields (REVIEW R14): the Islamic date is required every day and must convert back to the
 * day; the source-aware Hijri date is required exactly when the Iranian official variant is chosen and absent
 * otherwise, and when present it is the day's Islamic date. Lunar-tithi expectations below use `NepaliLunarDays`, the
 * routine the app itself uses, so they check the rule engine's use of it, not the tithi astronomy (DT-014).
 */
class CalendarCompletenessTest {
    private val days: JdnRange =
        persianNewYear(FIRST_YEAR)..(persianNewYear(LAST_YEAR + 1) - 1)
    private val tehran = TimeZone.of("Asia/Tehran")

    @Test
    fun `the range covers the 101 Persian years 1380 to 1480`() {
        days.dayCount shouldBeInRange 36_880L..36_900L
        PersianCalendarSystem.fromJdn(days.start) shouldBe CalendarDate(CalendarSystem.PERSIAN, FIRST_YEAR, 1, 1)
        PersianCalendarSystem.fromJdn(days.endInclusive).year shouldBe LAST_YEAR
    }

    @Test
    fun `every calendar and Islamic variant converts every day and back`() {
        val failures =
            allCalendars().flatMap { calendar ->
                var previous: CalendarDate? = null
                days.mapNotNull { day ->
                    val date = calendar.fromJdn(day)
                    val problem = conversionProblem(calendar, day, date, previous)
                    previous = date
                    problem
                }
            }
        failures.shouldBeEmpty()
    }

    @Test
    fun `every dataset rule occurs in every year it can for every Islamic variant`() {
        val failures =
            IslamicVariant.entries.flatMap { variant ->
                val selection = IslamicCalendarSelection(variant)
                OfficialEvents.ALL.flatMap { definition -> ruleProblems(definition, selection) }
            }
        failures.shouldBeEmpty()
    }

    @Test
    fun `every day assembles under each language's defaults and with every source on`() {
        val failures = settingsUnderTest().flatMap { (label, settings) -> assemblyProblems(label, settings) }
        failures.shouldBeEmpty()
    }

    @Test
    fun `the assembly covers all 24 launch languages`() {
        LanguageTable.languages
            .map { it.code }
            .toSet()
            .size shouldBe LAUNCH_LANGUAGES
    }

    @Test
    fun `every Persian year has Iranian official holidays`() {
        val settings = UserPreferences.defaultsFor("fa").toEventsSettings(homeTimeZone = tehran)
        val view = OfficialView(OfficialCatalog(), settings)
        (FIRST_YEAR..LAST_YEAR).forEach { year ->
            val holidays = (persianNewYear(year)..(persianNewYear(year + 1) - 1)).count(view::isHoliday)
            holidays shouldBeGreaterThan MIN_HOLIDAYS_PER_YEAR
        }
    }

    private fun allCalendars(): List<CalendarArithmetic> =
        CalendarSystem.entries
            .filter { it != CalendarSystem.ISLAMIC }
            .map { IslamicCalendarSelection.arithmeticFor(it, IslamicVariant.TABULAR_16) } +
            IslamicVariant.entries.map(IslamicCalendarSelection::calendarFor)

    private fun conversionProblem(
        calendar: CalendarArithmetic,
        day: Jdn,
        date: CalendarDate,
        previous: CalendarDate?,
    ): String? {
        val label = "${calendar.system} $calendar $date (JDN ${day.value})"
        return when {
            !calendar.isValid(date.year, date.month, date.day) -> "$label is not a valid date"
            calendar.toJdn(date) != day -> "$label converts back to ${calendar.toJdn(date).value}"
            previous != null && !follows(calendar, previous, date) -> "$label does not follow $previous"
            else -> null
        }
    }

    /** Whether [date] is the day after [previous]: the next day of the month, or day 1 of the next month or year. */
    private fun follows(
        calendar: CalendarArithmetic,
        previous: CalendarDate,
        date: CalendarDate,
    ): Boolean {
        val lastOfMonth = previous.day == calendar.monthLength(previous.year, previous.month)
        val lastOfYear = lastOfMonth && previous.month == calendar.monthsInYear(previous.year)
        val expected =
            when {
                lastOfYear -> CalendarDate(previous.system, previous.year + 1, 1, 1)
                lastOfMonth -> previous.copy(month = previous.month + 1, day = 1)
                else -> previous.copy(day = previous.day + 1)
            }
        return date == expected
    }

    private fun ruleProblems(
        definition: EventDefinition,
        selection: IslamicCalendarSelection,
    ): List<String> {
        val provider = selection.providerFor(definition.source)
        val calendar = provider.calendarFor(definition.calendar) ?: return listOf("${definition.id} has no calendar")
        val calculator = OccurrenceCalculator(OfficialEvents.ALL, provider, SkyAstronomicalEventSource)
        val years = (calendar.fromJdn(days.start).year + 1) until calendar.fromJdn(days.endInclusive).year
        return years.mapNotNull { year ->
            val occurrences = calculator.occurrences(definition, year)
            val expected = expectedDates(definition.rule, calendar, year)
            val actual = occurrences.map { it.date }
            if (actual ==
                expected
            ) {
                null
            } else {
                "${definition.id} in $year (${calendar.system}): $actual, expected $expected"
            }
        }
    }

    /** The days [rule]'s astronomical instants fall on, in its own time zone, keeping its month filter. */
    private fun expectedAstronomicalDates(
        rule: EventRule.Astronomical,
        calendar: CalendarArithmetic,
        year: Int,
    ): List<CalendarDate> {
        val zone = TimeZone.of(rule.timeZone)
        val from = calendar.toJdn(CalendarDate(calendar.system, year, 1, 1)).toLocalDate().atStartOfDayIn(zone)
        val until = calendar.toJdn(CalendarDate(calendar.system, year + 1, 1, 1)).toLocalDate().atStartOfDayIn(zone)
        val instantDays =
            SkyAstronomicalEventSource
                .instants(rule.kind, from, until)
                .filter { it >= from && it < until }
                .sorted()
                .map { calendar.fromJdn(it.toJdn(zone) + rule.offsetDays) }
        val month = rule.month
        return if (month == null) instantDays else instantDays.filter { it.month == month }.take(1)
    }

    /** Every day of the span [rule] starts on, or none when its start rule gives no single day (ADR-0046). */
    private fun expectedWeekDates(
        rule: EventRule.Week,
        calendar: CalendarArithmetic,
        year: Int,
    ): List<CalendarDate> {
        val start = expectedDates(rule.start, calendar, year).singleOrNull() ?: return emptyList()
        val startJdn = calendar.toJdn(start)
        return (0 until rule.lengthDays).map { calendar.fromJdn(startJdn + it) }
    }

    /** The dates [rule] must give in [year], computed independently of the rule engine. */
    private fun expectedDates(
        rule: EventRule,
        calendar: CalendarArithmetic,
        year: Int,
    ): List<CalendarDate> =
        when (rule) {
            is EventRule.Fixed -> {
                listOfNotNull(
                    CalendarDate(calendar.system, year, rule.month, rule.day)
                        .takeIf { calendar.isValid(year, rule.month, rule.day) },
                )
            }

            is EventRule.LastDayOfMonth -> {
                listOf(CalendarDate(calendar.system, year, rule.month, calendar.monthLength(year, rule.month)))
            }

            is EventRule.NthWeekdayOfMonth -> {
                (1..calendar.monthLength(year, rule.month))
                    .map { CalendarDate(calendar.system, year, rule.month, it) }
                    .filter { calendar.toJdn(it).weekday() == rule.weekday }
                    .drop(rule.n - 1)
                    .take(1)
            }

            is EventRule.LastWeekdayOfMonth -> {
                (1..calendar.monthLength(year, rule.month))
                    .map { CalendarDate(calendar.system, year, rule.month, it) }
                    .filter { calendar.toJdn(it).weekday() == rule.weekday }
                    .takeLast(1)
                    .map { calendar.fromJdn(calendar.toJdn(it) + rule.offsetDays) }
            }

            is EventRule.LunarTithi -> {
                NepaliLunarDays
                    .days(year, rule.month, rule.tithi, rule.observance, rule.endTithi, rule.endOffsetDays)
                    .map(calendar::fromJdn)
            }

            is EventRule.Astronomical -> {
                expectedAstronomicalDates(rule, calendar, year)
            }

            is EventRule.Week -> {
                expectedWeekDates(rule, calendar, year)
            }

            else -> {
                error("rule type ${rule::class.simpleName} is not used by the dataset; extend this test")
            }
        }

    private fun settingsUnderTest(): List<Pair<String, EventsSettings>> {
        val defaults =
            LanguageTable.languages.map { spec ->
                "defaults of ${spec.code}" to UserPreferences.defaultsFor(spec).toEventsSettings(homeTimeZone = tehran)
            }
        val everySource =
            IslamicVariant.entries.map { variant ->
                "all sources, $variant" to
                    UserPreferences
                        .defaultsFor("fa")
                        .copy(islamicVariant = variant)
                        .toEventsSettings(enabledSources = EventSource.entries.toSet(), homeTimeZone = tehran)
            }
        return defaults + everySource
    }

    private fun assemblyProblems(
        label: String,
        settings: EventsSettings,
    ): List<String> {
        val view = OfficialView(OfficialCatalog(), settings)
        val assembler = DayEventsAssembler(Clock.System, tehran)
        val snapshot = Snapshot(view, emptyList(), emptyList(), emptyList())
        return (FIRST_YEAR..LAST_YEAR).flatMap { year ->
            val range = persianNewYear(year)..(persianNewYear(year + 1) - 1)
            val assembled = assembler.assemble(range, snapshot)
            val sizeProblem =
                "$label $year: ${assembled.size} days for ${range.dayCount}".takeIf {
                    assembled.size.toLong() != range.dayCount
                }
            listOfNotNull(sizeProblem) + assembled.mapNotNull { dayProblem(label, view, it) }
        }
    }

    private fun dayProblem(
        label: String,
        view: OfficialView,
        day: DayEvents,
    ): String? {
        val islamic = day.islamicDate
        val hijri = day.hijri
        val officialOk = day.official.all { it.jdn == day.jdn }
        val holidayOk = day.official.none { it.isHoliday } || day.isHoliday
        return when {
            view.islamicCalendar.toJdn(islamic) != day.jdn -> {
                "$label ${day.jdn}: Islamic date $islamic"
            }

            hijriFieldProblem(view, day) != null -> {
                "$label ${day.jdn}: ${hijriFieldProblem(view, day)}"
            }

            hijri != null && view.islamicCalendar.isValid(hijri.date.year, hijri.date.month, hijri.date.day).not() -> {
                "$label ${day.jdn}: resolved Hijri date ${hijri.date} is not valid"
            }

            !officialOk -> {
                "$label ${day.jdn}: an occurrence is dated on another day"
            }

            !holidayOk -> {
                "$label ${day.jdn}: a holiday occurrence on a day not marked as a holiday"
            }

            else -> {
                null
            }
        }
    }

    /** The source-aware Hijri date is required exactly for the Iranian official variant and is the day's Islamic date. */
    private fun hijriFieldProblem(
        view: OfficialView,
        day: DayEvents,
    ): String? {
        val required = view.settings.preferences.islamicVariant == IslamicVariant.IRAN_OFFICIAL
        val hijri = day.hijri
        return when {
            required && hijri == null -> "the Iranian official variant resolved no Hijri date"
            !required && hijri != null -> "a source-aware Hijri date outside the Iranian official variant"
            hijri != null && hijri.date != day.islamicDate -> "Hijri date ${hijri.date} is not ${day.islamicDate}"
            else -> null
        }
    }

    private companion object {
        const val FIRST_YEAR = 1380
        const val LAST_YEAR = 1480

        /** Nowruz alone gives four days off, and the law adds many more. */
        const val MIN_HOLIDAYS_PER_YEAR = 15
        const val LAUNCH_LANGUAGES = 24

        fun persianNewYear(year: Int): Jdn =
            PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, year, 1, 1))
    }
}
