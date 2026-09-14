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
 * D-06: ancient Iranian festivals. Only festivals whose Persian title and date are printed in the official
 * calendars of Iran are records; the others (Mehragān, Sada, Čahāršanba-sūrī, …) wait in docs/DATA_TODO.md for a
 * Persian primary source. Rules are spot-checked against the cited pages.
 */
class AncientIranianFestivalsTest {
    private val datasetText = File(property("taqvim.dataset.directory"), DATASET_FILE).readText()
    private val events: List<JsonObject> =
        ((Json.parseToJsonElement(datasetText) as? JsonObject)?.get("events") as? JsonArray)
            .orEmpty()
            .mapNotNull { it as? JsonObject }

    @Test
    fun `the ancient festivals dataset passes the validator`() {
        DatasetValidator(schemaText()).validate(mapOf(DATASET_FILE to datasetText)).shouldBeEmpty()
    }

    @Test
    fun `golden count and rules match the cited pages`() {
        events.size shouldBe EVENT_COUNT
        val byId = events.associateBy { it.text("id") }
        val golden = goldenRules()
        golden.keys shouldBe byId.keys
        golden.forEach { (id, expected) -> byId[id]?.let { ruleColumns(it) } shouldBe expected }
    }

    @Test
    fun `every record is a cultural Persian-calendar observance cited in the official calendars`() {
        events
            .filter { event ->
                val citations = event["citations"] as? JsonArray
                val official =
                    citations.orEmpty().mapNotNull { it as? JsonObject }.filter { citation ->
                        citation.text("url") == OFFICIAL_CALENDAR_URL && citation.text("page") != null
                    }
                event.text("calendar") != "PERSIAN" ||
                    event.text("source") != "ANCIENT_IRAN" ||
                    event.text("category") != "CULTURAL" ||
                    event["isHoliday"] != JsonPrimitive(false) ||
                    (event["title"] as? JsonObject)?.text("fa").isNullOrBlank() ||
                    official.size < OFFICIAL_YEARS
            }.map { it.text("id") }
            .shouldBeEmpty()
    }

    private fun goldenRules(): Map<String?, List<String>> =
        requireNotNull(javaClass.getResource(GOLDEN)) { "missing $GOLDEN" }
            .readText()
            .lines()
            .filterNot { it.startsWith("#") || it.isBlank() }
            .drop(1)
            .associate { line ->
                val columns = line.split(',')
                columns.first() to columns.drop(1)
            }

    private fun ruleColumns(event: JsonObject): List<String> {
        val rule = event["rule"] as? JsonObject ?: JsonObject(emptyMap())
        return listOf("type", "month", "day", "weekday", "n").map { key ->
            (rule[key] as? JsonPrimitive)?.content.orEmpty()
        }
    }

    private companion object {
        const val DATASET_FILE = "ancient-iran/ancient-iranian-festivals.json"
        const val GOLDEN = "/golden/ancient-iran/ancient-iranian-festivals-rules.csv"
        const val OFFICIAL_CALENDAR_URL = "https://calendar.ut.ac.ir/Fa/"
        const val EVENT_COUNT = 2
        const val OFFICIAL_YEARS = 2

        fun property(name: String): String = requireNotNull(System.getProperty(name)) { "$name is not set" }

        fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
    }
}
