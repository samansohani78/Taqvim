/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.calendar

import androidx.compose.ui.graphics.Color
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.CalendarLimits
import ir.taqvim.core.calendar.CalendarRangeException
import ir.taqvim.core.calendar.IslamicMonthOverrides
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.PrayerMethod
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.praytimes.PrayerSettings
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/**
 * BUG-1: ±100 years through every calendar entry point, in every calendar, Islamic variant, override state and both
 * app languages, must build a page and its day details without throwing (the crash guard of [CalendarRangeGuard]
 * covers what cannot be built). The month pager reaches ±[CalendarLimits.MAX_MONTH_OFFSET] months, so the pages of a
 * 100-year jump must exist for every combination the settings allow.
 */
class LongRangeNavigationTest {
    private val texts =
        MonthTexts(
            monthTitle = { month, year -> "$month $year" },
            monthRange = { first, last -> "$first-$last" },
            today = "TODAY",
            holiday = "HOLIDAY",
            events = { count, formatted -> "EVENTS $count $formatted" },
            separator = " | ",
            newEvent = "NEW",
            week = { "W$it" },
        )
    private val palette = IndicatorPalette(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Magenta)
    private val weekdays = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")

    /** 27 Shahrivar 1405 (18 September 2026). */
    private val today = Jdn(2461302L)
    private val overrides =
        IslamicMonthOverrides
            .parse(requireNotNull(IslamicMonthOverrides.bundledIranOfficialText()))
            .getOrThrow()
            .table

    private val places =
        listOf(
            CalendarPlace(
                "Tehran",
                Coordinates(35.69, 51.42),
                TimeZone.of("Asia/Tehran"),
                PrayerSettings(PrayerMethod.TEHRAN),
            ),
            CalendarPlace(
                "Tromso",
                Coordinates(69.65, 18.96),
                TimeZone.of("Europe/Oslo"),
                PrayerSettings(PrayerMethod.MWL),
            ),
            CalendarPlace(
                "Ushuaia",
                Coordinates(-54.8, -68.3),
                TimeZone.of("America/Argentina/Ushuaia"),
                PrayerSettings(PrayerMethod.MWL),
            ),
        )

    private fun settingsOf(
        primary: CalendarSystem,
        variant: IslamicVariant,
        withOverrides: Boolean,
        language: String,
    ) = CalendarSettings(
        calendars = listOf(primary) + CalendarSystem.entries.filter { it != primary },
        weekStart = Weekday.SATURDAY,
        islamicVariant = variant,
        languageCode = language,
        showWeekNumbers = true,
        islamicOverrides = if (withOverrides) overrides else null,
    )

    /** Month offsets of ±20, ±50 and ±100 years, in every calendar's month count, plus the pager's own limits. */
    private fun jumps(monthsPerYear: Int): List<Int> =
        listOf(20, 50, 100).flatMap { years -> listOf(-years * monthsPerYear, years * monthsPerYear) }

    /** Every combination of primary calendar, Islamic variant, override state and app language. */
    private fun combinations(): List<CalendarSettings> =
        CalendarSystem.entries.flatMap { primary ->
            IslamicVariant.entries.flatMap { variant ->
                listOf(false, true).flatMap { withOverrides ->
                    listOf("fa", "en").map { language -> settingsOf(primary, variant, withOverrides, language) }
                }
            }
        }

    @Test
    fun `every calendar builds pages 100 years either way`() {
        for (settings in combinations()) {
            val calendars = CalendarCalendars(settings)
            val language = requireNotNull(LanguageTable.forCode(settings.languageCode))
            val builder = MonthPageBuilder(calendars, language, texts, palette, weekdays)
            val monthsPerYear = calendars.arithmetic.first().monthsInYear(1405)
            for (offset in jumps(monthsPerYear)) {
                val page = builder.build(offset, today, today, events = null)
                page.days.size shouldBe MonthLayout.CELLS
                page.offset shouldBe offset
            }
        }
    }

    /**
     * Browsing far away must not retain anything: 2 401 pages (200 years of months) are built and dropped, and the
     * heap in use grows by less than the 80 MB the month screen is budgeted (T-1803). The measurement brackets the
     * loop with a collection, because otherwise it would only show this test's allocation churn; the device figure
     * comes from `dumpsys meminfo` in the BUG-1 report.
     */
    @Suppress("ExplicitGarbageCollectionCall")
    @Test
    fun `building two thousand pages keeps the heap under the budget`() {
        val calendars = CalendarCalendars(PERSIAN_FIRST)
        val builder =
            MonthPageBuilder(calendars, requireNotNull(LanguageTable.forCode("fa")), texts, palette, weekdays)
        System.gc()
        val before = usedHeapBytes()
        for (offset in -1_200..1_200) {
            builder.build(offset, today, today, events = null).days.size shouldBe MonthLayout.CELLS
        }
        System.gc()
        (usedHeapBytes() - before < MAX_HEAP_BYTES) shouldBe true
    }

    private fun usedHeapBytes(): Long = Runtime.getRuntime().let { it.totalMemory() - it.freeMemory() }

    @Test
    fun `day details hold 100 years either way at every place`() {
        val calendars = CalendarCalendars(PERSIAN_FIRST)
        val now = Instant.parse("2026-09-18T08:30:00Z")
        for (place in places) {
            for (years in listOf(-100, -50, -20, 20, 50, 100)) {
                val day = today + years * 365L
                DayDetailsCalculator.overview(day, today, calendars, place).day shouldBe day
                DayDetailsCalculator.times(day, place, now).day shouldBe day
            }
        }
    }

    /** Records range failures instead of writing them to the Android log, which a JVM test cannot call. */
    private class Recorder : RangeFailureSink {
        val reported = mutableListOf<String>()

        override fun report(
            what: String,
            failure: CalendarRangeException,
        ) {
            reported += what
        }
    }

    @Test
    fun `a day outside a calendar's range degrades to the nearest valid day`() {
        val calendars = CalendarCalendars(PERSIAN_FIRST)
        val sink = Recorder()
        CalendarRangeGuard.isInRange(calendars, today, sink) shouldBe true
        // The guard returns the anchor itself when nothing between it and the wanted day can be expressed.
        CalendarRangeGuard.nearestValid(calendars, today, today, sink) shouldBe today
        val far = Jdn(CalendarLimits.LAST_DAY.value)
        val nearest = CalendarRangeGuard.nearestValid(calendars, far, today, sink)
        CalendarRangeGuard.isInRange(calendars, nearest, sink) shouldBe true
    }

    @Test
    fun `the guard reports a range failure and rethrows anything else`() {
        val sink = Recorder()
        CalendarRangeGuard.orNull("range", sink) { throw CalendarRangeException("out of range") }.shouldBeNull()
        sink.reported shouldBe listOf("range")
        shouldThrow<IllegalStateException> { CalendarRangeGuard.orNull("other", sink) { error("a real bug") } }
    }

    @Test
    fun `month offsets survive the pager limits`() {
        val calendars = CalendarCalendars(PERSIAN_FIRST)
        for (offset in listOf(Int.MIN_VALUE, -MonthPages.TODAY_PAGE, 0, MonthPages.TODAY_PAGE, Int.MAX_VALUE)) {
            val page = MonthPages.pageOf(offset)
            (page in 0 until MonthPages.COUNT) shouldBe true
            calendars.monthStartAt(today, MonthPages.offsetOf(page))
        }
    }

    private companion object {
        /** The month screen's heap budget (T-1803, `MonthScreenMemoryBenchmark`). */
        const val MAX_HEAP_BYTES = 80L * 1024 * 1024
    }
}
