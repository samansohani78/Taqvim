/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.calendar.DateOrigin
import ir.taqvim.core.calendar.IranIslamicCalendar
import ir.taqvim.core.calendar.IslamicMonthOverrides
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.UmmAlQuraCalendar
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-1400 converter and day distance on the NLP parser and the calendars. */
class DateToolsTest {
    private val english = ToolsFixtures.settings("en")
    private val persian = ToolsFixtures.settings("fa")
    private val today = ToolsFixtures.NOW.toJdn(english.homeZone)

    private fun converted(
        input: String,
        settings: ToolsSettings = persian,
    ) = DateTools.convert(input, today, settings).shouldBeInstanceOf<ConverterResult.Converted>()

    @Test
    fun `each converted date says whether it is computed, official or from the printed calendar`() {
        val overrides =
            IslamicMonthOverrides.parse(IslamicMonthOverrides.bundledIranOfficialText().orEmpty()).getOrThrow()
        val calendars =
            listOf(
                PersianCalendarSystem,
                IranIslamicCalendar(overrides.table),
                UmmAlQuraCalendar,
                IranIslamicCalendar(),
            )
        val settings = english.copy(calendars = calendars)
        val ramadan1446 = Jdn(overrides.table.firstStartJdn)

        DateTools.describe(ramadan1446, settings).map { it.origin } shouldBe
            listOf(DateOrigin.COMPUTED, DateOrigin.OFFICIAL_OVERRIDE, DateOrigin.COMPUTED, DateOrigin.COMPUTED)
        val muharram1400 = UmmAlQuraCalendar.toJdn(CalendarDate(CalendarSystem.ISLAMIC, 1400, 1, 1))
        DateTools.describe(muharram1400, settings).map { it.origin } shouldBe
            listOf(DateOrigin.COMPUTED, DateOrigin.COMPUTED, DateOrigin.PUBLISHED_CALENDAR, DateOrigin.COMPUTED)
    }

    @Test
    fun `a written date is shown in every calendar`() {
        val result = converted("1405/6/22")
        result.isToday shouldBe false
        result.dates.map { it.system } shouldBe
            listOf(CalendarSystem.PERSIAN, CalendarSystem.ISLAMIC, CalendarSystem.GREGORIAN)
        result.dates[0].iso shouldBe "1405-06-22"
        result.dates[2].iso shouldBe "2026-09-13"
        result.dates[0].long shouldContain "شهریور"
        converted("۲۲ شهریور ۱۴۰۵").dates[2].iso shouldBe "2026-09-13"
        converted("tomorrow", english).dates[2].iso shouldBe "2026-06-22"
        converted("1405/6/22", english).dates[2].long shouldContain "September"
    }

    @Test
    fun `blank input is today and text without a date is not recognized`() {
        val result = converted("  ")
        result.isToday shouldBe true
        result.dates[0].iso shouldBe "1405-03-31"
        result.dates[2].numeric shouldContain "۲۰۲۶"
        DateTools.convert("no date here", today, english) shouldBe ConverterResult.NotRecognized
    }

    @Test
    fun `the distance counts days, weeks, calendar periods and workdays`() {
        val state = DateTools.distance("1405/1/1", "1405/6/22", today, persian)
        state.from.shouldBeInstanceOf<DistanceEnd.Recognized>().date shouldContain "فروردین"
        state.to.shouldBeInstanceOf<DistanceEnd.Recognized>().date shouldContain "شهریور"
        val result = state.result.shouldNotBeNull()
        result.isBackward shouldBe false
        listOf(result.days, result.weeks, result.weekDays) shouldBe listOf("۱۷۶", "۲۵", "۱")
        result.calendar shouldBe CalendarSystem.PERSIAN
        listOf(result.years, result.months, result.monthDays) shouldBe listOf("۰", "۵", "۲۱")
        // 2026-03-21 is a Saturday: 25 Fridays in the 176 days before 1405-06-22.
        result.workdays shouldBe WorkdaysText.Count("۱۵۱")
        result.daysText.shouldNotBeNull() shouldContain "۱۷۶"
    }

    @Test
    fun `backward, long and unconfigured distances`() {
        val backward = DateTools.distance("1405/6/22", "1405/1/1", today, english).result.shouldNotBeNull()
        backward.isBackward shouldBe true
        listOf(backward.days, backward.months, backward.monthDays) shouldBe listOf("176", "5", "21")
        DateTools
            .distance("1390/1/1", "1405/1/1", today, english)
            .result
            .shouldNotBeNull()
            .workdays shouldBe
            WorkdaysText.TooLong
        val unconfigured = ToolsFixtures.settings("en", workdays = null)
        DateTools
            .distance("1405/1/1", "1405/1/2", today, unconfigured)
            .result
            .shouldNotBeNull()
            .workdays shouldBe
            WorkdaysText.NotConfigured
        val half = DateTools.distance("", "banana", today, english)
        half.from shouldBe DistanceEnd.Empty
        half.to shouldBe DistanceEnd.NotRecognized
        half.result.shouldBeNull()
    }

    @Test
    fun `numeric dates shown by the converter convert back to the same day`(): Unit =
        runBlocking {
            val rotations = ToolsSettings.DEFAULT_CALENDARS.indices.map { ToolsSettings.DEFAULT_CALENDARS.rotate(it) }
            val offsets = Arb.long(-36_500L..36_500L)
            checkAll(PropertyTesting.iterations, offsets, Arb.int(0..2), Arb.int(0..1)) { offset, rotation, lang ->
                val day = Jdn(today.value + offset)
                val settings = ToolsFixtures.settings(listOf("fa", "en")[lang]).copy(calendars = rotations[rotation])
                val shown = DateTools.describe(day, settings).first()
                val back = DateTools.convert(shown.numeric, day, settings)
                withClue("${shown.numeric} (${shown.system}, ${settings.language.code})") {
                    back
                        .shouldBeInstanceOf<ConverterResult.Converted>()
                        .dates
                        .first()
                        .iso shouldBe shown.iso
                }
            }
        }

    private fun <T> List<T>.rotate(by: Int): List<T> = drop(by) + take(by)
}
