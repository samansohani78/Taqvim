/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.tools.dataset

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/**
 * D-02: the Iran official holiday records reproduce, date for date, the holidays of the official 1404 and 1405
 * calendars (Calendar Center, docs/sources). A minimal evaluator covers the rule types these records use.
 */
class IranOfficialHolidaysTest {
    private val datasetText = File(property("taqvim.dataset.directory"), DATASET_FILE).readText()
    private val events: List<JsonObject> =
        ((Json.parseToJsonElement(datasetText) as? JsonObject)?.get("events") as? JsonArray)
            .orEmpty()
            .mapNotNull { it as? JsonObject }

    @Test
    fun `the Iran holiday dataset passes the validator`() {
        DatasetValidator(schemaText()).validate(mapOf(DATASET_FILE to datasetText)).shouldBeEmpty()
    }

    @TestFactory
    fun `holiday rules reproduce the official holiday dates`(): List<DynamicTest> =
        YEARS.map { year ->
            DynamicTest.dynamicTest("$year") {
                val days = officialDays(year)
                val produced = days.indices.filter { index -> events.any { matches(it, days, index) } }
                val producedDates = produced.map { days[it].persian }.toSet()

                producedDates shouldHaveSize HOLIDAYS_PER_YEAR
                producedDates shouldBe days.filter { it.holiday }.map { it.persian }.toSet()
                producedDates shouldBe goldenHolidays(year)
            }
        }

    @Test
    fun `every record cites both official calendars with a page`() {
        // Both calendars must be cited with the page they print the day on. A record may carry further citations —
        // the law that created or restored a holiday, for instance — and a law has no page, so only the calendar
        // citations are required to have one.
        events
            .filter { event ->
                val citations = (event["citations"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
                val calendars = citations.filter { it.text("title").orEmpty().contains(CALENDAR_TITLE) }
                calendars.size != YEARS.size || calendars.any { it.text("page").isNullOrBlank() }
            }.shouldBeEmpty()
    }

    private fun matches(
        event: JsonObject,
        days: List<OfficialDay>,
        index: Int,
    ): Boolean {
        val rule = event["rule"] as? JsonObject ?: return false
        val month = rule.number("month")
        val day = days[index]
        val date = if (event.text("calendar") == "PERSIAN") day.persianParts else day.hijriParts
        val nextMonth = days.getOrNull(index + 1)?.hijriParts?.month
        return when (rule.text("type")) {
            "Fixed" -> date.month == month && date.day == rule.number("day")
            "LastDayOfMonth" -> date.month == month && nextMonth != null && nextMonth != month
            else -> error("rule type ${rule.text("type")} is not used by the Iran holiday records")
        }
    }

    private fun officialDays(year: Int): List<OfficialDay> =
        File(property("taqvim.official.days.directory"), "$year.csv")
            .readLines()
            .filterNot { it.startsWith("#") || it.isBlank() }
            .drop(1)
            .map { line ->
                val columns = line.split(',')
                OfficialDay(columns[PERSIAN_COLUMN], columns[HIJRI_COLUMN], columns[HOLIDAY_COLUMN] == "true")
            }

    private fun goldenHolidays(year: Int): Set<String> =
        requireNotNull(
            javaClass.getResource("/golden/iran/iran-official-holidays-$year.csv"),
        ) { "missing golden $year" }
            .readText()
            .lines()
            .filterNot { it.startsWith("#") || it.isBlank() }
            .drop(1)
            .map { it.substringBefore(',') }
            .toSet()

    private data class DateParts(
        val month: Int,
        val day: Int,
    )

    private data class OfficialDay(
        val persian: String,
        val hijri: String,
        val holiday: Boolean,
    ) {
        val persianParts: DateParts get() = parts(persian)
        val hijriParts: DateParts get() = parts(hijri)

        private fun parts(iso: String): DateParts {
            val fields = iso.split('-').map(String::toInt)
            return DateParts(fields[1], fields[2])
        }
    }

    private companion object {
        const val DATASET_FILE = "iran/iran-official-holidays.json"
        const val HOLIDAYS_PER_YEAR = 26
        const val PERSIAN_COLUMN = 0
        const val HIJRI_COLUMN = 2
        const val HOLIDAY_COLUMN = 4
        val YEARS = listOf(1404, 1405)

        /** Marks the citations that are official calendars, as opposed to a law or another source. */
        const val CALENDAR_TITLE = "Official calendar of Iran"

        fun property(name: String): String = requireNotNull(System.getProperty(name)) { "$name is not set" }

        fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

        fun JsonObject.number(key: String): Int? = (this[key] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull
    }
}
