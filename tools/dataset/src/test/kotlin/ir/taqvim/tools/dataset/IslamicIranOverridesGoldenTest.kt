/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.tools.dataset

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.IranIslamicCalendar
import ir.taqvim.core.calendar.IslamicMonthOverrides
import ir.taqvim.core.calendar.PersianCalendarSystem
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import org.junit.jupiter.api.Test

/**
 * D-07: the Islamic Iran override table is the file bundled in `:core:calendar` as the optional official override
 * (ADR-0037), parses to the same months, and every start is the first day of its Hijri month in the official daily
 * calendars of 1404 and 1405 (Calendar Center, docs/sources).
 */
class IslamicIranOverridesGoldenTest {
    private val datasetDirectory = File(property("taqvim.dataset.directory"))
    private val text = File(datasetDirectory, DATASET_FILE).readText()
    private val months: List<JsonObject> =
        ((Json.parseToJsonElement(text) as? JsonObject)?.get("months") as? JsonArray)
            .orEmpty()
            .mapNotNull { it as? JsonObject }

    @Test
    fun `the override table passes the validator`() {
        OverridesValidator(overridesSchemaText()).validate(mapOf(DATASET_FILE to text)).shouldBeEmpty()
    }

    @Test
    fun `the bundled optional override is this file byte for byte`() {
        IslamicMonthOverrides.bundledIranOfficialText() shouldBe text
    }

    @Test
    fun `the override parses to the same months month for month`() {
        val table = IslamicMonthOverrides.parse(text).getOrThrow().table
        val calendar = IranIslamicCalendar(table)
        val published =
            (0 until table.monthCount).associate { offset ->
                val absolute = table.firstYear * MONTHS + table.firstMonth - 1 + offset
                val key = absolute / MONTHS to absolute % MONTHS + 1
                key to calendar.toJdn(CalendarDate(CalendarSystem.ISLAMIC, key.first, key.second, 1)).value
            } + (table.next to table.endJdn)

        months.associate { (it.int("hijriYear") to it.int("hijriMonth")) to startJdn(it) } shouldBe published
    }

    @Test
    fun `every printed start is the first day of its month in the official daily calendar`() {
        val days = OFFICIAL_YEARS.flatMap(::officialDays).associateBy { it.persian }
        val mismatches =
            months.filter { it.citation("note") == null }.mapNotNull { month ->
                val start = persianStart(month)
                val row = days[start]
                val expected = "${month.int("hijriYear")}-${month.int("hijriMonth").toString().padStart(2, '0')}-01"
                month.takeUnless { row != null && row.hijri == expected && row.page == month.citation("page") }
            }

        withClue(mismatches.joinToString("\n")) { mismatches.shouldBeEmpty() }
    }

    @Test
    fun `the derived Ramadan 1446 start counts back from the printed 20 Ramadan`() {
        val derived = months.filter { it.citation("note") != null }
        val firstDay = officialDays(OFFICIAL_YEARS.first()).first()

        derived.map { it.int("hijriYear") to it.int("hijriMonth") } shouldBe listOf(1446 to 9)
        firstDay.hijri shouldBe "1446-09-20"
        startJdn(derived.single()) + DAYS_BEFORE_20TH shouldBe jdnOf(firstDay.persian)
    }

    @Test
    fun `every month cites a stored official calendar with a page`() {
        val missing =
            months.filterNot { month ->
                val file = PDF.find(month.citation("title").orEmpty())?.groupValues?.get(1)
                file != null && File(datasetDirectory.parentFile, file).isFile && month.citation("page") != null
            }

        missing.shouldBeEmpty()
    }

    private data class OfficialDay(
        val persian: String,
        val hijri: String,
        val page: String,
    )

    private fun officialDays(year: Int): List<OfficialDay> =
        File(property("taqvim.official.days.directory"), "iran-official-$year-days.csv")
            .readLines()
            .filterNot { it.startsWith("#") || it.isBlank() }
            .drop(1)
            .map { line ->
                val columns = line.split(',')
                OfficialDay(columns[PERSIAN_COLUMN], columns[HIJRI_COLUMN], columns[PAGE_COLUMN])
            }

    private fun JsonObject.int(key: String): Int = (this[key] as? JsonPrimitive)?.int ?: 0

    private fun JsonObject.citation(key: String): String? =
        ((this["citation"] as? JsonObject)?.get(key) as? JsonPrimitive)?.content

    private fun persianStart(month: JsonObject): String {
        val start = month["persianStart"] as? JsonObject ?: JsonObject(emptyMap())
        return listOf(start.int("year"), start.int("month"), start.int("day"))
            .joinToString("-") { it.toString().padStart(2, '0') }
    }

    private fun startJdn(month: JsonObject): Long = jdnOf(persianStart(month))

    private fun jdnOf(isoPersian: String): Long {
        val (year, month, day) = isoPersian.split('-').map(String::toInt)
        return PersianCalendarSystem.toJdn(CalendarDate(CalendarSystem.PERSIAN, year, month, day)).let(Jdn::value)
    }

    private companion object {
        const val DATASET_FILE = "iran/islamic-iran-overrides.json"
        const val MONTHS = 12
        const val DAYS_BEFORE_20TH = 19
        const val PERSIAN_COLUMN = 0
        const val HIJRI_COLUMN = 2
        const val PAGE_COLUMN = 5
        val OFFICIAL_YEARS = listOf(1404, 1405)
        val PDF = Regex("""\((docs/sources/[^)]+\.pdf)\)""")

        fun property(name: String): String = requireNotNull(System.getProperty(name)) { "$name is not set" }
    }
}
