/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.times

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import ir.taqvim.core.calendar.CalendarArithmetic
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.UmmAlQuraCalendar
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.praytimes.PrayerTimesResult
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test

/** T-1100 "report contains 29–31 rows" for every month of several years in three calendars. */
class MonthlyReportTest {
    private val labels =
        ReportLabels(
            title = { place, from, to -> "Times of $place from $from to $to" },
            date = "Date",
            prayers = PrayerKind.entries.associateWith { it.name },
            undefined = "-",
            polarDay = "No sunset",
            polarNight = "No sunrise",
        )

    private fun checkYears(
        calendar: CalendarArithmetic,
        years: IntRange,
        rowRange: IntRange,
    ) {
        val settings = TimesFixtures.tehran().copy(calendar = calendar)
        years.forEach { year ->
            (1..calendar.monthsInYear(year)).forEach { month ->
                val report = MonthlyReportBuilder.build(year, month, settings)
                report.rows.size shouldBeInRange rowRange
                report.rows shouldHaveSize calendar.monthLength(year, month)
                report.rows.forEachIndexed { index, row ->
                    row.date shouldBe calendar.date(year, month, index + 1)
                    row.weekday shouldBe calendar.toJdn(row.date).weekday()
                    row.times.shouldBeInstanceOf<PrayerTimesResult.Available>()
                }
            }
        }
    }

    @Test
    fun `persian months have 29 to 31 rows`() {
        checkYears(PersianCalendarSystem, 1400..1410, 29..31)
    }

    @Test
    fun `gregorian and umm al-qura months have one row per day`() {
        checkYears(GregorianCalendarSystem, 2024..2026, 28..31)
        checkYears(UmmAlQuraCalendar, 1445..1447, 29..30)
    }

    @Test
    fun `the report of a day is the report of its month`() {
        val settings = TimesFixtures.tehran()
        val report = MonthlyReportBuilder.forDay(LocalDate(2026, 3, 21).toJdn(), settings)
        (report.year to report.month) shouldBe (1405 to 1)
        report.rows.first().date shouldBe PersianCalendarSystem.date(1405, 1, 1)
    }

    @Test
    fun `the html has one table row per day plus the header, in the language's direction and digits`() {
        val fa = TimesFixtures.tehran("fa")
        val report = MonthlyReportBuilder.build(1405, 12, fa)
        val html = ReportHtml.render(report, labels, fa)
        Regex("<tr>").findAll(html).count() shouldBe report.rows.size + 1
        html shouldContain "dir=\"rtl\""
        html shouldContain "lang=\"fa"
        html shouldContain "Times of Tehran from"
        Regex("""<td>\d\d:\d\d</td>""").findAll(html).count() shouldBe 0
        Regex("""<td>[۰-۹]{2}:[۰-۹]{2}</td>""").findAll(html).count() shouldBe
            report.rows.size * PrayerKind.entries.size

        val en = TimesFixtures.tehran("en").copy(placeName = "A<B & \"C\"")
        val english = ReportHtml.render(MonthlyReportBuilder.build(1405, 1, en), labels, en)
        english shouldContain "dir=\"ltr\""
        english shouldContain "A&lt;B &amp; &quot;C&quot;"
        Regex("""<td>\d\d:\d\d</td>""").findAll(english).count() shouldBe 31 * PrayerKind.entries.size
    }

    @Test
    fun `polar days span the whole row`() {
        val tromso = TimesFixtures.tromso().copy(calendar = GregorianCalendarSystem)
        val html = ReportHtml.render(MonthlyReportBuilder.build(2026, 6, tromso), labels, tromso)
        html shouldContain "<td colspan=\"${PrayerKind.entries.size}\">No sunset</td>"
        ReportHtml.escape("'") shouldBe "&#39;"
        ReportHtml.escape(">") shouldBe "&gt;"
        val winter = ReportHtml.render(MonthlyReportBuilder.build(2026, 12, tromso), labels, tromso)
        winter shouldContain "No sunrise"
    }
}
