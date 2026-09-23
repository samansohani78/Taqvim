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
import org.junit.jupiter.api.Test

/**
 * D-05: the UN international days compiled from the United Nations list (titles in the UN languages), with Persian
 * titles either from UN Information Centre Tehran and the United Nations in Iran, or — where no primary Persian
 * source exists — machine-translated from the official English title and marked `titleReview: ["fa"]` (owner decision
 * 2026-09-23, ADR-0042, R07/D-05: a missing Persian source no longer keeps a well-sourced day out of the dataset).
 * Rules are spot-checked against the cited pages; days that cannot yet be expressed as a rule (e.g. Vesak, DT-040)
 * are listed in `docs/DATA_TODO.md`, not in the dataset.
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
            mapOf(
                "Fixed" to FIXED_COUNT,
                "NthWeekdayOfMonth" to WEEKDAY_COUNT,
                "LastWeekdayOfMonth" to LAST_WEEKDAY_COUNT,
            )
    }

    @Test
    fun `rules match the cited pages`() {
        val byId = events.associateBy { it.text("id") }
        goldenRules().forEach { (id, expected) -> byId[id]?.rule()?.let(::ruleColumns) shouldBe expected }
    }

    @Test
    fun `every record is cited, has an English title, and a Persian source or a reviewable machine translation`() {
        events
            .filter { event ->
                val titles = event["title"] as? JsonObject
                val urls = event.citations().mapNotNull { it.text("url") }
                val hasPersianSource = urls.any { url -> PERSIAN_SOURCES.any(url::contains) }
                val faNeedsReview = "fa" in event.titleReview()
                event.text("source") != "INTERNATIONAL" ||
                    event.text("category") != "INTERNATIONAL" ||
                    event["isHoliday"] != JsonPrimitive(false) ||
                    titles?.containsKey("en") != true ||
                    UN_LIST_URL !in urls ||
                    (!hasPersianSource && !faNeedsReview)
            }.map { it.text("id") }
            .shouldBeEmpty()
    }

    @Test
    fun `machine-translated titles are marked for review and carry both an English and a Persian title`() {
        val marked = events.filter { "fa" in it.titleReview() }

        marked shouldHaveSize TITLE_REVIEW_COUNT
        marked
            .filter { event ->
                val titles = event["title"] as? JsonObject
                titles?.containsKey("en") != true || titles.containsKey("fa") != true
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

    private fun JsonObject.titleReview(): List<String> =
        (this["titleReview"] as? JsonArray)
            .orEmpty()
            .mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content }

    private companion object {
        const val DATASET_FILE = "international/un-international-days.json"
        const val GOLDEN = "/golden/international/un-international-days-rules.csv"
        const val UN_LIST_URL = "https://www.un.org/en/observances/list-days-weeks"
        const val EVENT_COUNT = 234
        const val FIXED_COUNT = 228
        const val WEEKDAY_COUNT = 5
        const val LAST_WEEKDAY_COUNT = 1

        /** R07/D-05, 2026-09-23: 132 days added with an official citation but no Persian source (ADR-0042). */
        const val TITLE_REVIEW_COUNT = 132

        /** Persian titles come from UN Information Centre Tehran (live or Internet Archive) or United Nations in Iran. */
        val PERSIAN_SOURCES = listOf("unic-ir.org", "https://iran.un.org/fa/")

        fun property(name: String): String = requireNotNull(System.getProperty(name)) { "$name is not set" }

        fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
    }
}
