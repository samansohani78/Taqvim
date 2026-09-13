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
import org.junit.jupiter.api.Test

/**
 * D-05: the UN international days compiled from the United Nations list (titles in the UN languages) with Persian
 * titles from UN Information Centre Tehran and the United Nations in Iran. Rules are spot-checked against the cited
 * pages; days without a primary Persian title are listed in docs/DATA_TODO.md, not in the dataset.
 */
class UnInternationalDaysTest {
    private val datasetText = File(property("taqvim.dataset.directory"), DATASET_FILE).readText()
    private val events: List<JsonObject> =
        ((Json.parseToJsonElement(datasetText) as? JsonObject)?.get("events") as? JsonArray)
            .orEmpty()
            .mapNotNull { it as? JsonObject }

    @Test
    fun `the UN international days dataset passes the validator`() {
        DatasetValidator(schemaText()).validate(mapOf(DATASET_FILE to datasetText)).shouldBeEmpty()
    }

    @Test
    fun `golden count and rule types`() {
        events.size shouldBe EVENT_COUNT
        events.groupingBy { it.rule().text("type") }.eachCount() shouldBe
            mapOf("Fixed" to FIXED_COUNT, "NthWeekdayOfMonth" to WEEKDAY_COUNT)
    }

    @Test
    fun `rules match the cited pages`() {
        val byId = events.associateBy { it.text("id") }
        goldenRules().forEach { (id, expected) -> byId[id]?.rule()?.let(::ruleColumns) shouldBe expected }
    }

    @Test
    fun `every record is an international observance with cited Persian and English titles`() {
        events
            .filter { event ->
                val titles = event["title"] as? JsonObject
                val urls = event.citations().mapNotNull { it.text("url") }
                event.text("source") != "INTERNATIONAL" ||
                    event.text("category") != "INTERNATIONAL" ||
                    event["isHoliday"] != JsonPrimitive(false) ||
                    titles?.containsKey("en") != true ||
                    UN_LIST_URL !in urls ||
                    urls.none { url -> PERSIAN_SOURCES.any { url.startsWith(it) } }
            }.map { it.text("id") }
            .shouldBeEmpty()
    }

    private fun goldenRules(): Map<String, List<String>> =
        requireNotNull(javaClass.getResource(GOLDEN)) { "missing $GOLDEN" }
            .readText()
            .lines()
            .filterNot { it.startsWith("#") || it.isBlank() }
            .drop(1)
            .associate { line ->
                val columns = line.split(',')
                columns.first() to columns.drop(1)
            }

    private fun ruleColumns(rule: JsonObject): List<String> =
        listOf("type", "month", "day", "weekday", "n").map { key -> (rule[key] as? JsonPrimitive)?.content.orEmpty() }

    private fun JsonObject.rule(): JsonObject = this["rule"] as? JsonObject ?: JsonObject(emptyMap())

    private fun JsonObject.citations(): List<JsonObject> =
        (this["citations"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }

    private companion object {
        const val DATASET_FILE = "international/un-international-days.json"
        const val GOLDEN = "/golden/international/un-international-days-rules.csv"
        const val UN_LIST_URL = "https://www.un.org/en/observances/list-days-weeks"
        const val EVENT_COUNT = 93
        const val FIXED_COUNT = 89
        const val WEEKDAY_COUNT = 4
        val PERSIAN_SOURCES = listOf("https://www.unic-ir.org/", "https://iran.un.org/fa/")

        fun property(name: String): String = requireNotNull(System.getProperty(name)) { "$name is not set" }

        fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
    }
}
