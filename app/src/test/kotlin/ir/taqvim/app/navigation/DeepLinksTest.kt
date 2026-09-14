/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.navigation

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import ir.taqvim.core.calendar.IranIslamicCalendar
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.core.nlp.ParseContext
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test

/** T-1103: every documented `taqvim://` link opens its screen, and nothing else can break the app. */
class DeepLinksTest {
    private val nowruz1405 = LocalDate(2026, 3, 21).toJdn().value
    private val english = requireNotNull(LanguageTable.forCode("en"))

    @Test
    fun `documented links open their screens`() {
        val shawwal1447 = IranIslamicCalendar().run { toJdn(date(1447, 10, 1)) }.value
        val farvardin = requireNotNull(english.monthNames.persian).first()
        mapOf(
            "taqvim://calendar" to AppDestination.Calendar,
            "taqvim://day/1405-01-01" to AppDestination.Day(nowruz1405),
            "TAQVIM://Day/1405-1-1?calendar=PERSIAN" to AppDestination.Day(nowruz1405),
            "taqvim://day/2026-03-21?calendar=gregorian" to AppDestination.Day(nowruz1405),
            "taqvim://day/1447-10-01?calendar=islamic" to AppDestination.Day(shawwal1447),
            "taqvim://event/42" to AppDestination.EventEditor(42),
            "taqvim://event/new" to AppDestination.EventEditor(),
            "taqvim://event/new/1405-01-01" to AppDestination.EventEditor(day = nowruz1405),
            "taqvim://event/NEW/2026-03-21?calendar=gregorian" to AppDestination.EventEditor(day = nowruz1405),
            "taqvim://occasion/ir.holiday.nowruz-1?day=$nowruz1405" to AppDestination.Day(nowruz1405),
            "taqvim://convert?date=1405-01-01&from=persian" to AppDestination.Converter("1 $farvardin 1405"),
            "taqvim://convert?date=next%20friday" to AppDestination.Converter("next friday"),
            "taqvim://convert?date=1405-13-01&from=persian" to AppDestination.Converter("1405-13-01"),
            "taqvim://convert" to AppDestination.Tools,
            "taqvim://timeline" to AppDestination.Timeline(),
            "taqvim://timeline/1405-01-01" to AppDestination.Timeline(nowruz1405),
            "taqvim://timeline/2026-03-21?calendar=gregorian" to AppDestination.Timeline(nowruz1405),
            "taqvim://timeline/1405-13-01" to AppDestination.Calendar,
            "taqvim://times" to AppDestination.Times,
            "taqvim://astronomy" to AppDestination.Astronomy,
            "taqvim://map" to AppDestination.WorldMap,
            "taqvim://search?q=%D9%86%D9%88%D8%B1%D9%88%D8%B2" to AppDestination.SearchFor("نوروز"),
            "taqvim://search?q=+" to AppDestination.Search,
            "taqvim://settings" to AppDestination.Settings(),
            "taqvim://settings/backup" to AppDestination.Backup,
            "taqvim://settings/privacy" to AppDestination.Privacy,
            "taqvim://settings/about" to AppDestination.About,
            "taqvim://settings/main-calendar" to AppDestination.Settings("MAIN_CALENDAR"),
            "taqvim://settings/nonsense" to AppDestination.Settings(),
        ).forEach { (link, destination) -> withClue(link) { DeepLinks.parse(link) shouldBe destination } }
    }

    @Test
    fun `unreadable links open the calendar`() {
        listOf(
            "",
            "taqvim://",
            "https://example.com/day/1405-01-01",
            "taqvim://unknown/1",
            "taqvim://day/1405-07-31",
            "taqvim://day/1405-13-01",
            "taqvim://day/1405-01-01?calendar=julian",
            "taqvim://day/99999-01-01",
            "taqvim://event/-3",
            "taqvim://event/abc",
            "taqvim://event/new/1405-13-01",
            "taqvim://event/new/1405-01-01/extra",
            "taqvim://occasion/x?day=12",
            "taqvim://occasion/x",
            "taqvim://search?q=%ZZ",
        ).forEach { link -> withClue(link) { DeepLinks.parse(link) shouldBe AppDestination.Calendar } }
    }

    @Test
    fun `any text after the scheme is read without failing`(): Unit =
        runBlocking {
            checkAll(ITERATIONS, Arb.string(0..80)) { text ->
                DeepLinks.parse("taqvim://$text")
                DeepLinks.parse(text)
            }
        }

    @Test
    fun `every Persian date link opens exactly that day`(): Unit =
        runBlocking {
            checkAll(ITERATIONS, Arb.int(1..3_000), Arb.int(1..12), Arb.int(1..29)) { year, month, day ->
                val jdn = PersianCalendarSystem.toJdn(PersianCalendarSystem.date(year, month, day)).value
                DeepLinks.parse("taqvim://day/$year-$month-$day") shouldBe AppDestination.Day(jdn)
            }
        }

    @Test
    fun `selected text opens its first date, other text the converter and blank text the tools`() {
        val context = ParseContext.forLanguage(english, LocalDate(2026, 9, 14).toJdn())

        ProcessText.destination("Meeting on 21 March 2026 at the office", context) shouldBe
            AppDestination.Day(nowruz1405)
        ProcessText.destination("  hello there  ", context) shouldBe AppDestination.Converter("hello there")
        ProcessText.destination("   ", context) shouldBe AppDestination.Tools
        ProcessText.destination("x".repeat(900), context) shouldBe AppDestination.Converter("x".repeat(500))
    }

    private companion object {
        const val ITERATIONS = 1_000
    }
}
