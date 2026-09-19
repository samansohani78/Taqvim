/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.tools.dataset

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Planted canaries for [RuntimeDateScanner] (REVIEW R09). The review showed three per-year files passing the
 * no-per-year-data gate: a date in `res/xml`, a JSON date object, and (only the plain ISO text being caught) nothing in a
 * qualified or non-`main` resource directory or an innocently named Kotlin table. Each hole gets a canary here, in a
 * miniature repository, so the gate is proven to run against planted data rather than only against a clean tree.
 */
class RuntimeDateScannerTest {
    @TempDir
    lateinit var root: File

    private fun module(path: String): File =
        File(root, path).apply {
            mkdirs()
            File(this, "build.gradle.kts").writeText("")
        }

    private fun plant(
        path: String,
        text: String,
    ): File =
        File(root, path).apply {
            parentFile.mkdirs()
            writeText(text)
        }

    private fun dated(): List<String> = RuntimeDateScanner(root).datedData(emptyList())

    @Test
    fun `an ISO date in plain resource text is found, the negative control of the review`() {
        module("app")
        plant("app/src/main/resources/iso.txt", "1406-01-01\n")

        dated() shouldHaveSize 1
    }

    @Test
    fun `a date in an XML resource is found, the review's first bypass`() {
        module("app")
        plant("app/src/main/res/xml/year_1406.xml", "<dates>1406-01-01</dates>\n")

        dated().single() shouldContain "res/xml/year_1406.xml"
    }

    @Test
    fun `a numeric JSON date object is found, the review's second bypass`() {
        module("app")
        plant("app/src/main/resources/year_1406.json", """{"year":1406,"month":1,"day":1}""")

        dated().single() shouldContain "JSON date object"
    }

    @Test
    fun `a nested JSON date object in a list is found`() {
        module("app")
        plant("app/src/main/assets/holidays.json", """{"days":[{"name":"x","year":1406,"month":1,"day":13}]}""")

        dated().single() shouldContain "assets/holidays.json"
    }

    @Test
    fun `an XML element whose attributes give year, month and day is found`() {
        module("feature/x")
        plant("feature/x/src/main/res/xml/days.xml", """<days><day year="1406" month="1" day="1"/></days>""")

        dated().single() shouldContain "XML date element <day>"
    }

    @Test
    fun `resources under a language qualifier and in a non-main source set are found`() {
        module("app")
        plant("app/src/main/res/values-fa/arrays.xml", "<resources><item>1406-01-01</item></resources>\n")
        plant("app/src/debug/res/raw/dates.txt", "2027-03-21\n")

        dated() shouldHaveSize 2
    }

    @Test
    fun `test source sets and build-only modules are not runtime data`() {
        module("app")
        module("tools/dataset")
        plant("app/src/test/resources/golden.csv", "1406-01-01\n")
        plant("app/src/androidTest/assets/fixture.txt", "1406-01-01\n")
        plant("tools/dataset/src/main/resources/golden.txt", "1406-01-01\n")

        dated().shouldBeEmpty()
    }

    @Test
    fun `dates in comments and allowed files are not reported`() {
        module("app")
        plant("app/src/main/res/values/strings.xml", "<!-- 1406-01-01 -->\n<resources/>\n")
        plant("app/src/main/resources/official.json", """{"year":1406,"month":1,"day":1}""")

        RuntimeDateScanner(root).datedData(listOf(Regex("app/src/main/resources/official\\.json"))).shouldBeEmpty()
    }

    @Test
    fun `an innocently named Kotlin table of date literals is found by its content`() {
        module("core/x")
        plant(
            "core/x/src/main/kotlin/x/YearData.kt",
            """
            val days = listOf(
                CalendarDate(PERSIAN, 1406, 1, 1), CalendarDate(PERSIAN, 1406, 1, 13),
                CalendarDate(PERSIAN, 1407, 1, 1), CalendarDate(PERSIAN, 1407, 1, 13),
            )
            """.trimIndent(),
        )

        RuntimeDateScanner(root).dateTables(emptyList()).single() shouldContain "YearData.kt: 4 date literals"
    }

    @Test
    fun `a single epoch constant and commented dates are not a table`() {
        RuntimeDateScanner.dateLiteralCount(
            """
            val EPOCH = CalendarDate(GREGORIAN, 1970, 1, 1)
            // CalendarDate(PERSIAN, 1406, 1, 1), CalendarDate(PERSIAN, 1406, 1, 2), (1406, 1, 3)
            /* "1406-01-04" "1406-01-05" */
            """.trimIndent(),
        ) shouldBe 1
    }

    @Test
    fun `ISO date strings count as date literals`() {
        RuntimeDateScanner.dateLiteralCount("""val a = listOf("1406-01-01", "1406-01-13", "1407-01-01")""") shouldBe 3
    }
}
