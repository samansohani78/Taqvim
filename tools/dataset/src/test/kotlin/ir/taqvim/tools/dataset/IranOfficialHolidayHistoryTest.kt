/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.tools.dataset

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import ir.taqvim.core.testing.SnapshotVerifier
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/**
 * D-01/D-02: the Iran holiday records against every official calendar of 1381–1405 SH that can be read
 * (`golden/persian/official/<year>.csv`, `golden/iran/iran-official-holidays-<year>.csv`).
 *
 * Read with the lunar dates the calendars print, the records must give exactly the official holidays of every year: a
 * holiday a law added or removed is handled by the record's `validity`, never by a per-year exception. Read with the
 * computed Iranian lunar calendar the app uses by default, the holidays may land on other days, but only where that
 * calendar starts a month on another day than the official one, or where an announcement moved a month after the
 * calendar was printed. The per-year comparison is the snapshot `docs/data-todo/iran-holiday-history.md`; run
 * `./gradlew :tools:dataset:test -Ptaqvim.updateSnapshots=true` to rewrite it after adding a calendar.
 */
class IranOfficialHolidayHistoryTest {
    private val events: List<JsonObject> =
        ((Json.parseToJsonElement(datasetFile().readText()) as? JsonObject)?.get("events") as? JsonArray)
            .orEmpty()
            .mapNotNull { it as? JsonObject }
    private val rules = IranHolidayRules(events)
    private val years: Map<Int, List<OfficialDay>> =
        File(property("taqvim.official.days.directory"))
            .listFiles { file -> YEAR_FILE.matches(file.name) }
            .orEmpty()
            .associate { it.nameWithoutExtension.toInt() to OfficialDay.readYear(it) }
            .filterKeys { goldenHolidays(it) != null }
            .toSortedMap()
    private val printedHijri: Map<Long, Ymd> = years.values.flatten().associate { it.jdn to it.printedHijri }
    private val computed = ComputedCalendarComparison(rules, announcedMonths())

    private fun printed(jdn: Long): Ymd = printedHijri[jdn] ?: computed.hijri(jdn)

    @Test
    fun `every readable official calendar is compared`() {
        years.keys.toList() shouldBe EXPECTED_YEARS
    }

    @TestFactory
    fun `the records give the official holidays of every year read with the printed lunar dates`(): List<DynamicTest> =
        years.map { (year, days) ->
            DynamicTest.dynamicTest("$year") {
                val produced = rules.evaluate(days, ::printed).holidays.keys
                val official = days.filter { it.holiday }.map { it.persian.toString() }.toSet()
                withClue("missing ${official - produced}, extra ${produced - official}") { produced shouldBe official }
                official shouldBe goldenHolidays(year)
            }
        }

    @Test
    fun `every difference with the computed calendar is a lunar-calendar difference or an announced shift`() {
        years.values
            .flatMap { days -> computed.differences(days, rules.evaluate(days, ::printed).holidays) }
            .filter { it.kind == HolidayDifference.RULE_ERROR }
            .shouldBeEmpty()
    }

    @Test
    fun `the holiday history report is up to date`() {
        val report = File(property(REPORT_PROPERTY))
        SnapshotVerifier().verify(report, render(), GENERATOR).getOrThrow() shouldBe Unit
    }

    private fun render(): String =
        buildString {
            appendLine(REPORT_HEADER)
            appendLine()
            appendLine(
                "| Year | Official holidays | Printed lunar dates: missing / extra | Left out by `validity` | " +
                    "Computed calendar: lunar-calendar / announced shift |",
            )
            appendLine("|---|---|---|---|---|")
            val differences = years.mapValues { (_, days) -> computed.differences(days, printedReading(days)) }
            years.forEach { (year, days) -> appendLine(summaryRow(year, days, differences.getValue(year))) }
            appendLine()
            appendLine(LAW_CHANGES)
            appendLine()
            appendAnnouncedMonths()
            appendLine()
            appendLine("## Differences with the computed calendar")
            appendLine()
            appendLine(
                "| Persian date | Record | Official holiday | Printed lunar date | Computed lunar date | Class |",
            )
            appendLine("|---|---|---|---|---|---|")
            differences.values.flatten().forEach { difference ->
                val official = if (difference.official) "yes" else "no"
                appendLine(
                    "| ${difference.persian} | `${difference.record}` | $official | ${difference.printedHijri} | " +
                        "${difference.computedHijri} | ${difference.kind.label} |",
                )
            }
        }

    private fun StringBuilder.appendAnnouncedMonths() {
        appendLine("## Months an announcement moved")
        appendLine()
        appendLine(ANNOUNCED_INTRO)
        appendLine()
        appendLine("| Lunar month | Printed first day | Announced first day | Computed first day |")
        appendLine("|---|---|---|---|")
        announcedRows().forEach { (month, announced) ->
            val (year, number) = month.split('-').map(String::toInt)
            val printedStart = years.values.flatten().firstOrNull { it.printedHijri == Ymd(year, number, 1) }
            val computedStart = computed.persianStart(year, number)
            appendLine("| $month | ${printedStart?.persian ?: "—"} | $announced | $computedStart |")
        }
    }

    private fun printedReading(days: List<OfficialDay>): Map<String, List<String>> =
        rules.evaluate(days, ::printed).holidays

    private fun summaryRow(
        year: Int,
        days: List<OfficialDay>,
        differences: List<ComputedDifference>,
    ): String {
        val reading = rules.evaluate(days, ::printed)
        val official = days.filter { it.holiday }.map { it.persian.toString() }.toSet()
        val excluded =
            reading.excluded.entries.joinToString(", ") { (date, ids) ->
                ids.joinToString(", ") { "`${it.removePrefix(ID_PREFIX)}` ${date.substring(MONTH_DAY)}" }
            }
        val lunar = differences.count { it.kind == HolidayDifference.LUNAR_CALENDAR }
        val announced = differences.count { it.kind == HolidayDifference.ANNOUNCED_SHIFT }
        return "| $year | ${official.size} | ${(official - reading.holidays.keys).size} / " +
            "${(reading.holidays.keys - official).size} | ${excluded.ifEmpty { "—" }} | $lunar / $announced |"
    }

    private fun goldenHolidays(year: Int): Set<String>? =
        javaClass
            .getResource("/golden/iran/iran-official-holidays-$year.csv")
            ?.readText()
            ?.lines()
            ?.filterNot { it.startsWith("#") || it.isBlank() }
            ?.drop(1)
            ?.map { it.substringBefore(',') }
            ?.toSet()

    private fun announcedMonths(): Set<String> = announcedRows().keys

    /** Lunar month (yyyy-mm) → announced first day (Persian), from the `announced` rows of the month-start history. */
    private fun announcedRows(): Map<String, String> =
        File(property(MONTH_STARTS_PROPERTY))
            .readLines()
            .filterNot { it.startsWith("#") || it.isBlank() }
            .map { it.split(',') }
            .filter { it.getOrNull(BASIS_COLUMN) == "announced" }
            .associate { it[0] to it[PERSIAN_START_COLUMN] }

    private fun datasetFile(): File = File(property("taqvim.dataset.directory"), "iran/iran-official-holidays.json")

    private companion object {
        const val REPORT_PROPERTY = "taqvim.iran.holiday.history.report"
        const val MONTH_STARTS_PROPERTY = "taqvim.iran.official.month.starts"
        const val GENERATOR = "IranOfficialHolidayHistoryTest (:tools:dataset)"
        const val ID_PREFIX = "ir.holiday."
        const val MONTH_DAY = 5
        const val BASIS_COLUMN = 4
        const val PERSIAN_START_COLUMN = 2
        val YEAR_FILE = Regex("""\d{4}\.csv""")

        // The editions of 1395, 1396, 1401 and 1402 cannot be read (their digits do not extract).
        val EXPECTED_YEARS = (1381..1394) + (1397..1400) + (1403..1405)

        val REPORT_HEADER =
            """
            # Iran official holidays 1381–1405 against the dataset rules

            Every official calendar of Iran that the importer can read (docs/sources/iran/MANIFEST.md) compared with the
            records of `dataset/iran/iran-official-holidays.json`. Read with the lunar dates each calendar prints, the
            records give exactly its holidays in every year. Read with the computed Iranian lunar calendar (the app's
            default, ADR-0027), a holiday moves with its month wherever that calendar starts the month on another day
            than the official one; in 1381 and 1383–1385 the printed calendar was the Calendar Center's prediction,
            and an announcement later moved one month (the notice on page 1 of each), so those holidays moved too.
            Written by `IranOfficialHolidayHistoryTest`; run `./gradlew :tools:dataset:test
            -Ptaqvim.updateSnapshots=true` to refresh this page.
            """.trimIndent()

        val ANNOUNCED_INTRO =
            """
            The calendars of 1381 and 1383–1385 were printed before the month began; the notice on their first page
            says the official month started a day off the printed one. The holiday lists above keep the printed dates.
            The computed calendar is compared here with both.
            """.trimIndent()

        val LAW_CHANGES =
            """
            Two holidays were added by law during these years; each record's `validity` starts at the first year the
            official calendar marks the day (تعطیل), with that page as its citation:

            - `imam-hasan-askari-martyrdom` (8 Rabi al-Awwal): a holiday from 1440 AH (Calendar-1397.pdf page 13). The
              calendars of 1381–1396 print the day without (تعطیل); 1395 and 1396 were checked in their occasion text.
            - `eid-al-fitr-holiday` (2 Shawwal): a holiday from 1433 AH (Calendar-1391.pdf page 8); up to 1390 only
              1 Shawwal is.
            """.trimIndent()

        fun property(name: String): String = requireNotNull(System.getProperty(name)) { "$name is not set" }
    }
}
