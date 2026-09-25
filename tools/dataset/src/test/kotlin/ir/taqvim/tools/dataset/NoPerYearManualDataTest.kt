/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.tools.dataset

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test

/**
 * "Computed, not typed" (owner directive 2026-09-17, ADR-0036): no runtime data may hold per-year date instances that a
 * rule could express, so nobody has to type next year's dates. Runtime data is the event dataset, the generated
 * `:data:events` sources, and everything a build packages from every non-test source set of every shipped module — all of
 * `res/` with its qualifiers, `assets/`, `resources/` and the Kotlin sources ([RuntimeDateScanner], REVIEW R09).
 */
class NoPerYearManualDataTest {
    private val root = File(property("taqvim.repository.root"))
    private val datasetDirectory = File(property("taqvim.dataset.directory"))

    @Test
    fun `every dataset event regenerates from a rule`() {
        val files =
            datasetDirectory
                .walkTopDown()
                .filter { it.isFile && it.extension == "json" && it.parentFile != datasetDirectory }
                .filterNot { it.name.endsWith(OVERRIDES_SUFFIX) }
                .associate { it.relativeTo(datasetDirectory).path to it.readText() }

        DatasetValidator(schemaText()).validate(files).shouldBeEmpty()
    }

    @Test
    fun `shipped datasets and generated events hold no single-year instances`() {
        val generated = File(property("taqvim.generated.events.directory")).listFiles().orEmpty()
        val generatedSingles =
            generated
                .filter { it.extension == "kt" }
                .flatMap { file -> file.readText().split(DEFINITION).drop(1) }
                .filter { SINGLE_RULE in it }
                .mapNotNull { ID.find(it)?.groupValues?.get(1) }

        withClue("convert these to recurring rules or remove them (ADR-0036)") {
            singleRecordIds().shouldBeEmpty()
            generatedSingles.shouldBeEmpty()
        }
    }

    @Test
    fun `runtime resources and assets hold no calendar dates outside comments`() {
        val overrides =
            datasetDirectory.walkTopDown().filter { it.isFile && it.name.endsWith(OVERRIDES_SUFFIX) }.toList()
        val dated = RuntimeDateScanner(root).datedData(ALLOWED_DATED_FILES.keys, overrides)

        withClue(dated.joinToString("\n")) { dated.shouldBeEmpty() }
    }

    @Test
    fun `table-like runtime sources are justified`() {
        val scanner = RuntimeDateScanner(root)
        val byName =
            scanner
                .runtimeKotlinRoots()
                .flatMap { it.walkTopDown().filter { file -> file.isFile && file.extension == "kt" }.toList() }
                .filter { TABLE_NAME.matches(it.name) }
                .map { it.relativeTo(root).invariantSeparatorsPath }
                .filterNot { path -> ALLOWED_TABLES.keys.any { it.matches(path) } }
        val byContent = scanner.dateTables(ALLOWED_TABLES.keys)

        withClue("compute these or justify them in ALLOWED_TABLES:\n" + (byName + byContent).joinToString("\n")) {
            byName.shouldBeEmpty()
            byContent.shouldBeEmpty()
        }
    }

    private fun singleRecordIds(): List<String> =
        datasetDirectory
            .walkTopDown()
            .filter { it.isFile && it.extension == "json" && it.parentFile != datasetDirectory }
            .filterNot { it.name.endsWith(OVERRIDES_SUFFIX) }
            .flatMap { file ->
                val document = runCatching { Json.parseToJsonElement(file.readText()) }.getOrNull() as? JsonObject
                (document?.get("events") as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
            }.filter { (it["rule"] as? JsonObject)?.get("type") == JsonPrimitive(SINGLE) }
            .mapNotNull { (it["id"] as? JsonPrimitive)?.content }
            .toList()

    private companion object {
        const val OVERRIDES_SUFFIX = "-overrides.json"
        const val DEFINITION = "EventDefinition("
        const val SINGLE = "Single"
        const val SINGLE_RULE = "EventRule.Single("
        val ID = Regex("""id = EventId\("([^"]+)"\)""")
        val TABLE_NAME = Regex(""".*(Table|MonthStarts|Override|Overrides|Leap)\w*\.kt""")

        /** Dated runtime data that no rule can express, each with its reason (see docs/DATA_AUDIT.md). */
        val ALLOWED_DATED_FILES: Map<Regex, String> =
            mapOf(
                Regex("dataset/iran/islamic-iran-overrides\\.json") to
                    "Officially announced Iranian Hijri month starts (moon-sighting decisions): optional on top of " +
                    "the computed crescent months (ADR-0027), never required for the app to work (ADR-0037).",
                Regex("core/calendar/src/main/resources/.*/islamic-iran-(official|overrides)[\\w.-]*\\.json") to
                    "The same announced Iranian month starts bundled as an optional, user-switchable override, and " +
                    "its JSON schema; the computed calendar is the default.",
            )

        /** Table-like runtime sources, each with its reason (see docs/DATA_AUDIT.md). */
        val ALLOWED_TABLES: Map<Regex, String> =
            mapOf(
                Regex(".*/IslamicMonthTable\\.kt") to "A container type for month lengths; it holds no dates.",
                Regex(".*/IslamicMonthOverrides?\\w*\\.kt") to
                    "Parses and applies an optional month-start override file; it holds no dates itself.",
                Regex(".*/IslamicOverride\\w*\\.kt") to
                    "Settings screen, store and file reader of the optional override (ADR-0037); no dates.",
                Regex(".*/UmmAlQura\\w*\\.kt") to
                    "The printed Umm al-Qura calendar for historical years that no published criterion reproduces " +
                    "(ADR-0028); every other year is computed.",
                Regex(".*/NepaliMonthStarts\\.kt") to "Computed from sankrantis at run time (ADR-0030); a cache only.",
                Regex(".*/ObservationalMonthStarts\\.kt") to "Computed from the crescent criterion; stores no dates.",
                Regex(".*/(LanguageTable|FormatTable)\\.kt") to "Language names and patterns (CLDR), not dates.",
                Regex(".*/CityTableParser\\.kt") to "Parses the city catalog (names, coordinates, zones), not dates.",
                Regex(".*/IranDivisionTableParser\\.kt") to
                    "Parses the Iran province/county catalog (names, coordinates), not dates (DT-021).",
            )

        fun property(name: String): String = requireNotNull(System.getProperty(name)) { "$name is not set" }
    }
}
