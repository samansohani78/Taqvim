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
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.MinuteOfDay
import ir.taqvim.core.praytimes.PrayerSettings
import ir.taqvim.core.testing.PropertyTesting
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

/** T-1201…T-1204: the widgets' shared content, localized, with the day's prayer times and the next one. */
class WidgetContentBuilderTest {
    private val fa: LanguageSpec = requireNotNull(LanguageTable.forCode("fa"))
    private val en: LanguageSpec = requireNotNull(LanguageTable.forCode("en"))
    private val tehran = WidgetPlace(Coordinates(35.69, 51.42), TimeZone.of("Asia/Tehran"), PrayerSettings())
    private val day = LocalDate(2026, 9, 13).toJdn()
    private val names = WidgetPrayerNames { it.name }

    private fun inputs(
        language: LanguageSpec,
        place: WidgetPlace? = tehran,
    ) = WidgetDayInputs(
        jdn = day,
        language = language,
        primary = PersianCalendarSystem,
        secondary = GregorianCalendarSystem,
        isHoliday = true,
        events = listOf(WidgetEventLine("Mom", isHoliday = false, eventId = 7)),
        place = place,
    )

    @Test
    fun `dates are written in the language with its digits`() {
        val persian = WidgetContentBuilder.build(inputs(fa), Instant.parse("2026-09-13T06:00:00Z"), names)
        persian.dayNumber shouldBe "۲۲"
        persian.title shouldBe "۲۲ شهریور ۱۴۰۵"
        persian.weekday shouldBe "یکشنبه"
        persian.secondaryDate shouldBe "۱۳ سپتامبر ۲۰۲۶"
        persian.date shouldBe LocalDate(2026, 9, 13)
        persian.isHoliday shouldBe true
        persian.events.single().eventId shouldBe 7

        val english = WidgetContentBuilder.build(inputs(en), Instant.parse("2026-09-13T06:00:00Z"), names)
        english.dayNumber shouldBe "22"
        english.title shouldBe "22 Shahrivar 1405"
        english.weekday shouldBe "Sunday"
        english.secondaryDate shouldBe "13 September 2026"
    }

    @Test
    fun `the day's prayer times are listed in order and the next one is marked`() {
        // 13:30 in Tehran: Dhuhr has passed, Asr is next.
        val data = WidgetContentBuilder.build(inputs(fa), Instant.parse("2026-09-13T10:00:00Z"), names)

        data.prayers.map { it.name } shouldBe WidgetPrayer.entries.map { it.name }
        data.prayers.filter { it.isNext }.map { it.name } shouldBe listOf("ASR")
        data.nextPrayer.shouldNotBeNull().name shouldBe "ASR"
        data.prayers.map { it.time } shouldBe data.prayers.map { it.time }.sorted()
        data.prayers.all { line -> line.time.all { it == ':' || it in '۰'..'۹' } } shouldBe true
    }

    @Test
    fun `after the last time of the day the next prayer is tomorrow's Fajr`() {
        val data = WidgetContentBuilder.build(inputs(en), Instant.parse("2026-09-13T19:30:00Z"), names)

        data.prayers shouldHaveSize WidgetPrayer.entries.size
        data.prayers.none { it.isNext } shouldBe true
        data.nextPrayer.shouldNotBeNull().name shouldBe "FAJR"
    }

    @Test
    fun `without a place there are no prayer times`() {
        val data = WidgetContentBuilder.build(inputs(en, place = null), Instant.parse("2026-09-13T10:00:00Z"), names)

        data.prayers.shouldBeEmpty()
        data.nextPrayer.shouldBeNull()
    }

    @Test
    fun `the next prayer is always strictly later and at most one line is marked`(): Unit =
        runBlocking {
            val start = Instant.parse("2026-01-01T00:00:00Z").epochSeconds
            val year = start..Instant.parse("2027-01-01T00:00:00Z").epochSeconds
            checkAll(PropertyTesting.iterations, Arb.long(year)) { seconds ->
                val now = Instant.fromEpochSeconds(seconds)
                val next = WidgetPrayers.next(now, tehran).shouldNotBeNull()
                (next.second > now) shouldBe true
                val data = WidgetContentBuilder.build(inputs(en).copy(jdn = now.toJdn(tehran.timeZone)), now, names)
                (data.prayers.count { it.isNext } <= 1) shouldBe true
            }
        }

    @Test
    fun `clock times are zero padded in the language's digits`() {
        WidgetContentBuilder.clock(MinuteOfDay(5 * 60 + 7), fa) shouldBe "۰۵:۰۷"
        WidgetContentBuilder.clock(MinuteOfDay(23 * 60 + 59), en) shouldBe "23:59"
    }
}
