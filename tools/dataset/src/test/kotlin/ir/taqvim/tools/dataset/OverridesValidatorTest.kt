/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.tools.dataset

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import kotlin.random.Random
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.io.TempDir

/** D-07: the override schema plus cross-record rules. Fixtures are synthetic and contain no real-world data. */
class OverridesValidatorTest {
    private val validator = OverridesValidator(overridesSchemaText())

    @Test
    fun `the valid sample passes`() {
        validator.validate(mapOf("sample-overrides.json" to overridesResource("valid/sample.json"))).shouldBeEmpty()
    }

    @TestFactory
    fun `each invalid fixture reports exactly its one issue`(): List<DynamicTest> =
        INVALID_FIXTURES.map { (name, kind) ->
            DynamicTest.dynamicTest(name) {
                val issues = validator.validate(mapOf("$name.json" to overridesResource("invalid/$name.json")))
                withClue(issues.joinToString("\n")) { issues.map { it.kind } shouldBe listOf(kind) }
            }
        }

    @Test
    fun `the invalid fixtures cover every issue kind`() {
        INVALID_FIXTURES.values.toSet() shouldBe OverrideIssueKind.entries.toSet()
    }

    @Test
    fun `months are checked across files and the covered range is continuous`() {
        val sample = overridesResource("valid/sample.json")
        val later = sample.replace("\"hijriYear\": 1500", "\"hijriYear\": 1501")

        val duplicates = validator.validate(mapOf("a-overrides.json" to sample, "b-overrides.json" to sample))
        duplicates.map { it.kind }.toSet() shouldBe setOf(OverrideIssueKind.DUPLICATE_MONTH)
        duplicates.first().message shouldContain "a-overrides.json"
        val gap = validator.validate(mapOf("a-overrides.json" to sample, "b-overrides.json" to later))
        gap.map { it.kind } shouldContain OverrideIssueKind.MONTH_GAP
    }

    @Test
    fun `semantic checks are skipped for files with schema errors`() {
        val broken = overridesResource("invalid/02-month-13.json").replace("\"day\": 1 ", "\"day\": 30 ")

        validator.validate(mapOf("broken.json" to broken)).map { it.kind }.toSet() shouldBe
            setOf(OverrideIssueKind.SCHEMA)
    }

    @Test
    fun `2 000 random documents never crash the validator`() {
        val random = Random(SEED)
        val sample = overridesResource("valid/sample.json")

        repeat(FUZZ_DOCUMENTS) { index ->
            val text = if (index % 2 == 0) RandomJson(random).document() else RandomJson(random).mutate(sample)
            validator.validate(mapOf("fuzz-$index.json" to text))
        }
    }

    @Test
    fun `the CLI reports override files and event files leave them out`(
        @TempDir directory: File,
    ) {
        val schema = File(directory, "islamic-iran-overrides.v1.json").apply { writeText(overridesSchemaText()) }
        File(directory, "events.v1.json").writeText(schemaText())
        File(directory, "iran").mkdirs()
        File(directory, "iran/sample.json").writeText(resource("valid/sample.json"))
        File(directory, "iran/test-overrides.json").writeText(overridesResource("valid/sample.json"))
        val output = mutableListOf<String>()

        runOverridesCli(listOf(schema.path, directory.path)) { output += it } shouldBe 0
        output.last() shouldContain "1 override file(s) checked, 0 issue(s)"
        datasetFiles(File(directory, "events.v1.json"), directory).keys shouldBe setOf("iran/sample.json")

        File(directory, "iran/broken-overrides.json").writeText(overridesResource("invalid/05-missing-month.json"))
        runOverridesCli(listOf(schema.path, directory.path)) { output += it } shouldBe 1
        runOverridesCli(listOf(schema.path)) { output += it } shouldBe 2
    }

    private companion object {
        const val SEED = 1446
        const val FUZZ_DOCUMENTS = 2_000

        val INVALID_FIXTURES =
            linkedMapOf(
                "01-malformed-json" to OverrideIssueKind.MALFORMED_JSON,
                "02-month-13" to OverrideIssueKind.SCHEMA,
                "03-citation-without-page" to OverrideIssueKind.SCHEMA,
                "04-duplicate-month" to OverrideIssueKind.DUPLICATE_MONTH,
                "05-missing-month" to OverrideIssueKind.MONTH_GAP,
                "06-month-of-28-days" to OverrideIssueKind.MONTH_LENGTH,
                "07-month-of-31-days" to OverrideIssueKind.MONTH_LENGTH,
                "08-esfand-30-in-common-year" to OverrideIssueKind.INVALID_DATE,
            )
    }
}

/** The repository override schema, passed in by the build (`taqvim.overrides.schema`). */
internal fun overridesSchemaText(): String =
    File(requireNotNull(System.getProperty("taqvim.overrides.schema")) { "taqvim.overrides.schema is not set" })
        .readText()

/** A test resource under `overrides/`. */
internal fun overridesResource(path: String): String =
    requireNotNull(
        OverridesValidatorTest::class.java.getResource("/overrides/$path"),
    ) { "missing resource $path" }.readText()
