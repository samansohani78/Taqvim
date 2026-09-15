/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.CalendarLimits
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.praytimes.PrayerSettings
import ir.taqvim.core.testing.PropertyTesting
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/** T-1205…T-1209: month pages, the week, the schedule and the Sun's path, in the language and week of the user. */
class WidgetCalendarBuilderTest {
    private val fa: LanguageSpec = requireNotNull(LanguageTable.forCode("fa"))
    private val en: LanguageSpec = requireNotNull(LanguageTable.forCode("en"))
    private val today = LocalDate(2026, 9, 13).toJdn()
    private val noFacts: (Jdn) -> WidgetDayFacts = { WidgetDayFacts.NONE }
    private val tehran = WidgetPlace(Coordinates(35.69, 51.42), TimeZone.of("Asia/Tehran"), PrayerSettings())
    private val dentist = WidgetEventLine("Dentist", isHoliday = false, eventId = 43)

    private fun inputs(
        language: LanguageSpec = en,
        secondary: CalendarArithmetic? = GregorianCalendarSystem,
        weekStart: Weekday = Weekday.SATURDAY,
    ) = WidgetCalendarInputs(today, language, PersianCalendarSystem, secondary, weekStart)

    @Test
    fun `the month page starts on the week start and marks today, the month and each day's facts`() {
        val holiday = LocalDate(2026, 9, 15).toJdn()
        val month =
            WidgetCalendarBuilder.month(inputs(), 0) { jdn ->
                if (jdn == holiday) WidgetDayFacts(true, false, listOf(dentist)) else WidgetDayFacts.NONE
            }

        month.title shouldBe "Shahrivar 1405"
        month.secondaryTitle shouldBe "August – September 2026"
        month.weekdayLabels shouldBe listOf("S", "S", "M", "T", "W", "T", "F")
        month.days shouldHaveSize WidgetCalendarBuilder.GRID_DAYS
        month.days.first().date shouldBe LocalDate(2026, 8, 22)
        month.days.first().inMonth shouldBe false
        month.days.count { it.inMonth } shouldBe 31
        month.firstDay shouldBe LocalDate(2026, 8, 23)
        month.offset shouldBe 0
        val shownToday = month.days.single { it.isToday }
        shownToday.date shouldBe LocalDate(2026, 9, 13)
        shownToday.dayLabel shouldBe "22"
        shownToday.secondaryLabel shouldBe "13"
        shownToday.description shouldBe "22 Shahrivar 1405"
        val shownHoliday = month.days.single { it.isHoliday }
        shownHoliday.date shouldBe LocalDate(2026, 9, 15)
        shownHoliday.eventCount shouldBe 1
    }

    @Test
    fun `month titles span the secondary calendar's months and years`() {
        val dey = WidgetCalendarBuilder.month(inputs(), -8, noFacts)
        dey.title shouldBe "Dey 1404"
        dey.secondaryTitle shouldBe "December 2025 – January 2026"
        dey.days.none { it.isToday } shouldBe true
        WidgetCalendarBuilder.month(inputs(secondary = PersianCalendarSystem), 0, noFacts).secondaryTitle shouldBe
            "Shahrivar 1405"
        WidgetCalendarBuilder.month(inputs(secondary = null), 0, noFacts).run {
            secondaryTitle.shouldBeNull()
            days.all { it.secondaryLabel == null } shouldBe true
        }
    }

    @Test
    fun `month navigation reaches every month an Int offset holds`() {
        val far = WidgetCalendarBuilder.MAX_MONTH_OFFSET
        far shouldBe CalendarLimits.MAX_MONTH_OFFSET
        WidgetCalendarBuilder.monthStart(inputs(), Int.MAX_VALUE) shouldBe
            WidgetCalendarBuilder.monthStart(inputs(), far)
        WidgetCalendarBuilder.month(inputs(), Int.MIN_VALUE, noFacts).offset shouldBe -far
        val shown = PersianCalendarSystem.fromJdn(today)
        PersianCalendarSystem.fromJdn(WidgetCalendarBuilder.monthStart(inputs(), 12_000)) shouldBe
            PersianCalendarSystem.date(shown.year + 1_000, shown.month, 1)
        PersianCalendarSystem.fromJdn(WidgetCalendarBuilder.monthStart(inputs(), far)).year shouldBe
            shown.year + Math.floorDiv(shown.month - 1 + far, 12)
        WidgetMonthStep.next(3, 1) shouldBe 4
        WidgetMonthStep.next(3, -4) shouldBe -1
        WidgetMonthStep.next(3, 0) shouldBe 0
        WidgetMonthStep.next(3, null) shouldBe 0
        WidgetMonthStep.next(WidgetCalendarBuilder.MAX_MONTH_OFFSET, 1) shouldBe WidgetCalendarBuilder.MAX_MONTH_OFFSET
    }

    @Test
    fun `every month page holds its whole month in six weeks from the week start`(): Unit =
        runBlocking {
            val shownToday = PersianCalendarSystem.fromJdn(today)
            val todayIndex = shownToday.year * 12 + shownToday.month - 1
            checkAll(PropertyTesting.iterations, Arb.int(-1200..1200), Arb.enum<Weekday>()) { offset, weekStart ->
                val first = WidgetCalendarBuilder.monthStart(inputs(weekStart = weekStart), offset)
                val range = WidgetCalendarBuilder.monthRange(inputs(weekStart = weekStart), offset)
                val date = PersianCalendarSystem.fromJdn(first)

                date.day shouldBe 1
                (date.year * 12 + date.month - 1) shouldBe todayIndex + offset
                range.start.weekday() shouldBe weekStart
                range.dayCount shouldBe WidgetCalendarBuilder.GRID_DAYS.toLong()
                val last = first + (PersianCalendarSystem.monthLength(date.year, date.month) - 1)
                (range.start <= first && last <= range.endInclusive) shouldBe true
            }
        }

    @Test
    fun `the week runs from the week start and the schedule keeps today and days with events`() {
        val week = WidgetCalendarBuilder.week(inputs(), noFacts)
        week.map { it.date } shouldBe (12..18).map { LocalDate(2026, 9, it) }
        week.single { it.isToday }.weekdayLabel shouldBe "S"
        week.all { it.inMonth } shouldBe true
        WidgetCalendarBuilder.weekRange(inputs(weekStart = Weekday.MONDAY)).start shouldBe LocalDate(2026, 9, 7).toJdn()

        val eventDays = setOf(today + 2, today + WidgetCalendarBuilder.SCHEDULE_DAYS)
        val schedule =
            WidgetCalendarBuilder.schedule(inputs()) { jdn ->
                if (jdn in eventDays) WidgetDayFacts(false, false, listOf(dentist)) else WidgetDayFacts.NONE
            }
        schedule.map { it.date } shouldBe listOf(LocalDate(2026, 9, 13), LocalDate(2026, 9, 15))
        schedule.first().isToday shouldBe true
        schedule.first().events.shouldBeEmpty()
        schedule.first().title shouldBe "Sunday 22 Shahrivar 1405"
        schedule.last().title shouldBe "Tuesday 24 Shahrivar 1405"
        schedule.last().events shouldBe listOf(dentist)
    }

    @Test
    fun `persian pages use persian names and digits`() {
        val month = WidgetCalendarBuilder.month(inputs(fa), 0, noFacts)
        month.title shouldBe "شهریور ۱۴۰۵"
        month.secondaryTitle shouldBe "اوت – سپتامبر ۲۰۲۶"
        month.weekdayLabels shouldBe listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
        month.days.single { it.isToday }.run {
            dayLabel shouldBe "۲۲"
            secondaryLabel shouldBe "۱۳"
        }
    }

    @Test
    fun `the sun's progress runs from sunrise to sunset and is missing at night`() {
        val noon = WidgetCalendarBuilder.sun(Instant.parse("2026-09-13T10:00:00Z"), tehran, en).shouldNotBeNull()
        noon.sunrise shouldMatch Regex("""05:\d\d""")
        noon.sunset shouldMatch Regex("""18:\d\d""")
        val progress = noon.progress.shouldNotBeNull()
        (progress > 0.4f && progress < 0.7f) shouldBe true

        WidgetCalendarBuilder
            .sun(
                Instant.parse("2026-09-13T20:00:00Z"),
                tehran,
                en,
            ).shouldNotBeNull()
            .progress
            .shouldBeNull()
        WidgetCalendarBuilder
            .sun(
                Instant.parse("2026-09-13T10:00:00Z"),
                tehran,
                fa,
            ).shouldNotBeNull()
            .sunrise shouldMatch
            Regex("""۰۵:[۰-۹]{2}""")
        val longyearbyen = WidgetPlace(Coordinates(78.22, 15.65), TimeZone.of("Arctic/Longyearbyen"), PrayerSettings())
        WidgetCalendarBuilder.sun(Instant.parse("2026-12-21T11:00:00Z"), longyearbyen, en).shouldBeNull()
    }
}
