/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.tools.dataset

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/**
 * D-03: Afghanistan official holidays from Bakhtar News Agency announcements. Each record is one announced day
 * (a `Single` rule in the calendar the announcement uses); the golden file lists, per Solar Hijri year, exactly the
 * dates the cited announcements state.
 */
class AfghanistanOfficialHolidaysTest {
    private val datasetText = File(property("taqvim.dataset.directory"), DATASET_FILE).readText()
    private val events: List<JsonObject> =
        ((Json.parseToJsonElement(datasetText) as? JsonObject)?.get("events") as? JsonArray)
            .orEmpty()
            .mapNotNull { it as? JsonObject }

    @Test
    fun `the Afghanistan holiday dataset passes the validator`() {
        DatasetValidator(schemaText()).validate(mapOf(DATASET_FILE to datasetText)).shouldBeEmpty()
    }

    @TestFactory
    fun `records reproduce the announced holiday dates per year`(): List<DynamicTest> {
        val golden = goldenRows()
        return golden.map { it.solarYear }.distinct().map { year ->
            DynamicTest.dynamicTest(year) {
                val expected = golden.filter { it.solarYear == year }.associate { it.id to it.date }
                events.filter { it.text("id") in expected.keys }.associate { it.text("id") to date(it) } shouldBe
                    expected
            }
        }
    }

    @Test
    fun `the dataset holds exactly the golden records`() {
        events.map { it.text("id") }.toSet() shouldBe goldenRows().map { it.id }.toSet()
    }

    @Test
    fun `every record is an announced Afghan holiday cited to Bakhtar with a page`() {
        events
            .filter { event ->
                val citations = (event["citations"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
                event.text("source") != "AFGHANISTAN_OFFICIAL" ||
                    event["isHoliday"] != JsonPrimitive(true) ||
                    (event["rule"] as? JsonObject)?.text("type") != "Single" ||
                    (event["title"] as? JsonObject)?.text("fa").isNullOrBlank() ||
                    citations.isEmpty() ||
                    citations.any { citation ->
                        citation.text("url")?.startsWith(BAKHTAR) != true || citation.text("page").isNullOrBlank()
                    }
            }.map { it.text("id") }
            .shouldBeEmpty()
    }

    private fun date(event: JsonObject): String {
        val rule = event["rule"] as? JsonObject ?: JsonObject(emptyMap())
        val parts = listOf("year", "month", "day").map { (rule[it] as? JsonPrimitive)?.content.orEmpty() }
        return (listOf(event.text("calendar").orEmpty()) + parts).joinToString(",")
    }

    private fun goldenRows(): List<GoldenRow> =
        requireNotNull(javaClass.getResource(GOLDEN)) { "missing $GOLDEN" }
            .readText()
            .lines()
            .filterNot { it.startsWith("#") || it.isBlank() }
            .drop(1)
            .map { line ->
                val columns = line.split(',')
                GoldenRow(columns[0], columns[1], columns.drop(2).joinToString(","))
            }

    private data class GoldenRow(
        val solarYear: String,
        val id: String,
        val date: String,
    )

    private companion object {
        const val DATASET_FILE = "afghanistan/afghanistan-official-holidays.json"
        const val GOLDEN = "/golden/afghanistan/afghanistan-official-holidays.csv"
        const val BAKHTAR = "https://www.bakhtarnews.af/"

        fun property(name: String): String = requireNotNull(System.getProperty(name)) { "$name is not set" }

        fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
    }
}
