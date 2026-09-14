/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.timeline

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.IslamicVariant
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.praytimes.PrayerSettings
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/** T-900: shown days, the day calendar, columns with placed events and prayer lines. */
class TimelineContentBuilderTest {
    @Test
    fun `a week starts on the week start and a day shows itself`() {
        mapOf(
            Weekday.SATURDAY to gregorian(2026, 4, 4),
            Weekday.MONDAY to gregorian(2026, 4, 6),
            Weekday.SUNDAY to gregorian(2026, 4, 5),
            Weekday.FRIDAY to TODAY,
        ).forEach { (weekStart, first) ->
            val week = TimelineContentBuilder.range(TimelineMode.WEEK, TODAY, weekStart)
            week.start shouldBe first
            week.endInclusive shouldBe first + 6
        }
        TimelineContentBuilder.range(TimelineMode.DAY, TODAY, Weekday.SATURDAY).toList() shouldContainExactly
            listOf(TODAY)
    }

    @Test
    fun `every week holds its day and starts on the week start`(): Unit =
        runBlocking {
            checkAll(PropertyTesting.iterations, Arb.long(2_400_000L..2_500_000L), Arb.enum<Weekday>()) { day, start ->
                val week = TimelineContentBuilder.range(TimelineMode.WEEK, Jdn(day), start)

                week.dayCount shouldBe 7L
                week.start.weekday() shouldBe start
                week.toList() shouldContain Jdn(day)
            }
        }

    @Test
    fun `the first available calendar titles the days`() {
        fun primary(vararg calendars: CalendarSystem) =
            TimelineContentBuilder.primaryCalendar(calendars.toList(), IslamicVariant.UMM_AL_QURA).system

        primary(CalendarSystem.NEPALI, CalendarSystem.ISLAMIC) shouldBe CalendarSystem.ISLAMIC
        primary(CalendarSystem.NEPALI) shouldBe CalendarSystem.GREGORIAN
        primary() shouldBe CalendarSystem.GREGORIAN
        primary(CalendarSystem.PERSIAN) shouldBe CalendarSystem.PERSIAN
    }

    @Test
    fun `columns separate all-day events and place overlapping timed events side by side`() {
        val holiday = allDay("h", "Holiday", isHoliday = true)
        val events = listOf(holiday, timed("a", 540, 600), timed("b", 570, 660))
        val day = TimelineDay(TODAY, isHoliday = true, isWeekend = true, events = events)
        val line = persistentListOf(PrayerLine(PrayerLineKind.DHUHR, 725))

        val columns =
            TimelineContentBuilder.columns(TODAY - 1..TODAY + 1, listOf(day)) {
                if (it == TODAY) line else persistentListOf()
            }

        columns.map { it.jdn } shouldContainExactly listOf(TODAY - 1, TODAY, TODAY + 1)
        columns.first().run {
            isHoliday shouldBe false
            allDay.shouldBeEmpty()
            timed.shouldBeEmpty()
            prayerLines.shouldBeEmpty()
        }
        columns[1].run {
            isHoliday shouldBe true
            isWeekend shouldBe true
            allDay shouldContainExactly listOf(holiday)
            timed.map { Triple(it.event.id, it.column, it.columns) } shouldContainExactly
                listOf(Triple("a", 0, 2), Triple("b", 1, 2))
            prayerLines shouldBe line
        }
    }

    @Test
    fun `prayer lines follow the day in order at the chosen place`() {
        val lines = TimelineContentBuilder.prayerLines(TODAY, TEHRAN)

        lines.map { it.kind } shouldContainExactly PrayerLineKind.entries
        lines.zipWithNext().forEach { (earlier, later) -> (earlier.minute < later.minute) shouldBe true }
        // Solar noon at 51.39° E on a clock of UTC+3:30 (meridian 52.5° E) falls a few minutes after 12:00.
        lines.single { it.kind == PrayerLineKind.DHUHR }.minute shouldBeInRange 12 * 60..12 * 60 + 15
        TimelineContentBuilder.prayerLines(TODAY, null).shouldBeEmpty()
    }

    @Test
    fun `no prayer lines on a polar day`() {
        // Tromsø at midsummer; rounded coordinates used only as a sample input.
        val tromso = TimelinePlace(Coordinates(69.65, 18.96), TimeZone.of("Europe/Oslo"), PrayerSettings())

        TimelineContentBuilder.prayerLines(gregorian(2026, 6, 21), tromso).shouldBeEmpty()
    }
}
