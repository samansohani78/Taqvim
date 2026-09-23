/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.tools.dataset

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import kotlin.random.Random
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/** D-01: the schema plus cross-record rules. Fixtures are synthetic and contain no real-world data. */
class DatasetValidatorTest {
    private val validator = DatasetValidator(schemaText())

    @Test
    fun `the valid sample passes and covers every rule type`() {
        val sample = resource("valid/sample.json")

        validator.validate(mapOf("sample.json" to sample)).shouldBeEmpty()
        RULE_TYPES.forEach { sample shouldContain "\"$it\"" }
    }

    @Test
    fun `an Astronomical FULL_MOON rule with a month passes (ADR-0044, DT-040)`() {
        val fullMoonInMay = resource("valid/full-moon-in-may.json")

        validator.validate(mapOf("full-moon-in-may.json" to fullMoonInMay)).shouldBeEmpty()
    }

    @TestFactory
    fun `each invalid fixture reports exactly its one issue`(): List<DynamicTest> =
        INVALID_FIXTURES.map { (name, kind) ->
            DynamicTest.dynamicTest(name) {
                val issues = validator.validate(mapOf("$name.json" to resource("invalid/$name.json")))
                withClue(issues.joinToString("\n")) { issues.map { it.kind } shouldBe listOf(kind) }
            }
        }

    @Test
    fun `there are 34 invalid fixtures and they cover every issue kind`() {
        INVALID_FIXTURES.keys shouldHaveSize 34
        INVALID_FIXTURES.values.toSet() shouldBe IssueKind.entries.toSet()
    }

    @Test
    fun `ids and references are checked across files`() {
        val base = resource("valid/sample.json")
        val relative = base.replace("\"test.base\"", "\"test.other-base\"")

        val duplicates = validator.validate(mapOf("a.json" to base, "b.json" to base))
        duplicates.map { it.kind }.toSet() shouldBe setOf(IssueKind.DUPLICATE_ID)
        duplicates.first().message shouldContain "a.json"
        validator.validate(mapOf("a.json" to base, "b.json" to relative.replace("test.", "other."))).shouldBeEmpty()
    }

    @Test
    fun `a one-off reason is only allowed on a Single rule`() {
        val single = resource("invalid/31-single-without-reason.json")
        val reason = "\"oneOffReason\": \"Synthetic one-off decision.\",\n      \"citations\""
        val withReason = single.replace("\"citations\"", reason)
        val fixed =
            withReason.replace(
                "\"type\": \"Single\",\n        \"year\": 1405,",
                "\"type\": \"Fixed\",",
            )

        validator.validate(mapOf("single.json" to withReason)).shouldBeEmpty()
        validator.validate(mapOf("fixed.json" to fixed)).map { it.kind } shouldBe listOf(IssueKind.ONE_OFF_RULE)
    }

    @Test
    fun `semantic checks are skipped for files with schema errors`() {
        val broken = resource("invalid/03-wrong-schema-version.json").replace("\"day\": 1", "\"day\": 31")

        validator.validate(mapOf("broken.json" to broken)).map { it.kind } shouldBe listOf(IssueKind.SCHEMA)
    }

    @Test
    fun `10 000 random documents never crash the validator`() {
        val random = Random(SEED)
        val sample = resource("valid/sample.json")

        repeat(FUZZ_DOCUMENTS) { index ->
            val text = if (index % 2 == 0) RandomJson(random).document() else RandomJson(random).mutate(sample)
            validator.validate(mapOf("fuzz-$index.json" to text))
        }
    }

    private companion object {
        const val SEED = 1405
        const val FUZZ_DOCUMENTS = 10_000

        val RULE_TYPES =
            listOf(
                "Fixed",
                "NthWeekdayOfMonth",
                "LastWeekdayOfMonth",
                "LastDayOfMonth",
                "Single",
                "NthDayOfYear",
                "RelativeToEvent",
                "Astronomical",
            )

        val INVALID_FIXTURES: Map<String, IssueKind> =
            listOf(
                "01-malformed-json" to IssueKind.MALFORMED_JSON,
                "02-not-an-object" to IssueKind.SCHEMA,
                "03-wrong-schema-version" to IssueKind.SCHEMA,
                "04-missing-events" to IssueKind.SCHEMA,
                "05-unknown-top-level-field" to IssueKind.SCHEMA,
                "06-bad-id-pattern" to IssueKind.SCHEMA,
                "07-unknown-calendar" to IssueKind.SCHEMA,
                "08-unknown-source" to IssueKind.SCHEMA,
                "09-unknown-category" to IssueKind.SCHEMA,
                "10-holiday-not-boolean" to IssueKind.SCHEMA,
                "11-missing-fa" to IssueKind.SCHEMA,
                "12-unknown-event-field" to IssueKind.SCHEMA,
                "13-missing-citation" to IssueKind.SCHEMA,
                "14-citation-not-http" to IssueKind.SCHEMA,
                "15-unknown-rule-type" to IssueKind.SCHEMA,
                "16-month-13" to IssueKind.SCHEMA,
                "17-day-zero" to IssueKind.SCHEMA,
                "18-nth-weekday-n-6" to IssueKind.SCHEMA,
                "19-bad-weekday" to IssueKind.SCHEMA,
                "20-astronomical-missing-time-zone" to IssueKind.SCHEMA,
                "21-offset-too-large" to IssueKind.SCHEMA,
                "22-bad-updated-date" to IssueKind.SCHEMA,
                "23-duplicate-id" to IssueKind.DUPLICATE_ID,
                "24-unknown-event-reference" to IssueKind.UNKNOWN_EVENT_REFERENCE,
                "25-self-reference" to IssueKind.UNKNOWN_EVENT_REFERENCE,
                "26-persian-day-out-of-range" to IssueKind.DAY_OUT_OF_RANGE,
                "27-gregorian-february-30" to IssueKind.DAY_OUT_OF_RANGE,
                "28-islamic-nth-day-360" to IssueKind.DAY_OUT_OF_RANGE,
                "29-validity-without-years" to IssueKind.INVALID_VALIDITY,
                "30-validity-reversed" to IssueKind.INVALID_VALIDITY,
                "31-single-without-reason" to IssueKind.ONE_OFF_RULE,
                "32-repeated-single-day" to IssueKind.ONE_OFF_RULE,
                "33-lunar-tithi-gregorian" to IssueKind.SCHEMA,
                "34-title-review-unknown-language" to IssueKind.TITLE_REVIEW_UNKNOWN_LANGUAGE,
            ).toMap()
    }
}

/** The repository schema, passed in by the build (`taqvim.dataset.schema`). */
internal fun schemaText(): String =
    File(requireNotNull(System.getProperty("taqvim.dataset.schema")) { "taqvim.dataset.schema is not set" }).readText()

/** A test resource under `dataset/`. */
internal fun resource(path: String): String =
    requireNotNull(
        DatasetValidatorTest::class.java.getResource("/dataset/$path"),
    ) { "missing resource $path" }.readText()
