/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.calendar

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import org.junit.jupiter.api.Test

/** ADR-0037: computed Islamic default, optional official override file, validation. */
class IslamicMonthOverridesTest {
    private val official = OfficialIranMonths.overrides

    private fun hijri(
        year: Int,
        month: Int,
    ) = CalendarDate(CalendarSystem.ISLAMIC, year, month, 1)

    private fun problemOf(text: String): OverrideProblem? =
        (IslamicMonthOverrides.parse(text).exceptionOrNull() as? InvalidOverrideException)?.problem

    private fun file(vararg months: String) = """{"schemaVersion": 1, "months": [${months.joinToString(",")}]}"""

    private fun month(
        year: Int,
        month: Int,
        persian: Triple<Int, Int, Int>,
        citation: String = CITATION,
    ) = """{"hijriYear": $year, "hijriMonth": $month, "persianStart": {"year": ${persian.first}, """ +
        """"month": ${persian.second}, "day": ${persian.third}}, "citation": $citation}"""

    @Test
    fun `without an override the Iranian calendar is exactly the computed one`() {
        val computed = IranIslamicCalendar()

        computed.hasOverride shouldBe false
        (1400..1500).flatMap { year -> (1..12).map { year to it } }.forEach { (year, month) ->
            computed.toJdn(hijri(year, month)) shouldBe IranCrescentCalendar.toJdn(hijri(year, month))
            computed.isOfficial(computed.toJdn(hijri(year, month))) shouldBe false
        }
    }

    @Test
    fun `the bundled official file covers Ramadan 1446 to Ramadan 1448 with a source per month`() {
        official.first shouldBe (1446 to 9)
        official.last shouldBe (1448 to 9)
        official.table.monthCount shouldBe 25
        official.citations shouldHaveSize 25
        official.citations.all { it.url.startsWith("https://") && it.retrieved.length == 10 } shouldBe true
    }

    @Test
    fun `the computed months agree with 23 of the 25 official month starts`() {
        val calendar = IranIslamicCalendar(official.table)
        val months = (0 until official.table.monthCount).map { official.table.yearMonthAt(it) }
        val differing =
            months.filter { (year, month) ->
                calendar.toJdn(hijri(year, month)) != IranCrescentCalendar.toJdn(hijri(year, month))
            }

        differing shouldHaveSize 2
        months.all { (year, month) -> calendar.isOfficial(calendar.toJdn(hijri(year, month))) } shouldBe true
    }

    @Test
    fun `an override outside its covered years leaves the computed months untouched`() {
        val calendar = IranIslamicCalendar(official.table)

        listOf(1400, 1430, 1460, 1500).forEach { year ->
            (1..12).forEach { month ->
                calendar.toJdn(hijri(year, month)) shouldBe IranCrescentCalendar.toJdn(hijri(year, month))
            }
        }
    }

    @Test
    fun `a minimal valid file parses`() {
        val text = file(month(1450, 1, Triple(1407, 4, 20)), month(1450, 2, Triple(1407, 5, 19)))
        val parsed = IslamicMonthOverrides.parse(text)

        parsed.getOrNull()?.table?.monthCount shouldBe 1
    }

    @Test
    fun `invalid files are rejected with their problem and never throw`() {
        problemOf("not json") shouldBe OverrideProblem.NOT_JSON
        problemOf("[]") shouldBe OverrideProblem.NOT_JSON
        problemOf("""{"schemaVersion": 2, "months": []}""") shouldBe OverrideProblem.UNSUPPORTED_VERSION
        problemOf("""{"schemaVersion": 1}""") shouldBe OverrideProblem.NOT_JSON
        problemOf(file(month(1450, 1, Triple(1407, 4, 20)))) shouldBe OverrideProblem.TOO_FEW_MONTHS
        problemOf(file(month(1450, 1, Triple(1407, 4, 20)), month(1450, 3, Triple(1407, 5, 19)))) shouldBe
            OverrideProblem.INVALID_MONTH
        problemOf(file(month(1450, 1, Triple(1407, 4, 20)), month(1450, 2, Triple(1407, 5, 25)))) shouldBe
            OverrideProblem.INVALID_LENGTH
        problemOf(file(month(1450, 1, Triple(1407, 13, 1)), month(1450, 2, Triple(1407, 5, 19)))) shouldBe
            OverrideProblem.INVALID_MONTH
        problemOf(file(month(1450, 1, Triple(1407, 4, 20), "{}"), month(1450, 2, Triple(1407, 5, 19)))) shouldBe
            OverrideProblem.MISSING_CITATION
        problemOf("""{"schemaVersion": 1, "months": [{"hijriYear": 1}, {"hijriYear": 2}]}""") shouldBe
            OverrideProblem.INVALID_MONTH
        IslamicMonthOverrides.parse("{").exceptionOrNull().shouldBeInstanceOf<InvalidOverrideException>()
    }

    @Test
    fun `every malformed field is reported as its problem`() {
        val first = month(1450, 1, Triple(1407, 4, 20))
        val second = month(1450, 2, Triple(1407, 5, 19))

        fun withFirst(entry: String) = file(entry, second)
        mapOf(
            """{"schemaVersion": "one", "months": []}""" to OverrideProblem.UNSUPPORTED_VERSION,
            """{"schemaVersion": {}, "months": []}""" to OverrideProblem.UNSUPPORTED_VERSION,
            """{"months": []}""" to OverrideProblem.UNSUPPORTED_VERSION,
            """{"schemaVersion": 1, "months": {}}""" to OverrideProblem.NOT_JSON,
            """{"schemaVersion": 1, "months": [1, 2]}""" to OverrideProblem.NOT_JSON,
            withFirst(first.replace("\"hijriMonth\": 1", "\"hijriMonth\": 13")) to OverrideProblem.INVALID_MONTH,
            withFirst(first.replace("\"hijriMonth\": 1", "\"hijriMonth\": 0")) to OverrideProblem.INVALID_MONTH,
            withFirst(first.replace("\"hijriMonth\": 1", "\"hijriMonth\": \"x\"")) to OverrideProblem.INVALID_MONTH,
            withFirst(first.replace("\"hijriYear\": 1450", "\"hijriYear\": []")) to OverrideProblem.INVALID_MONTH,
            withFirst(first.replace("\"persianStart\"", "\"start\"")) to OverrideProblem.INVALID_MONTH,
            withFirst(first.replace("\"year\": 1407", "\"yr\": 1407")) to OverrideProblem.INVALID_MONTH,
            withFirst(first.replace("\"month\": 4", "\"mo\": 4")) to OverrideProblem.INVALID_MONTH,
            withFirst(first.replace("\"day\": 20", "\"d\": 20")) to OverrideProblem.INVALID_MONTH,
            withFirst(month(1450, 1, Triple(1407, 0, 20))) to OverrideProblem.INVALID_MONTH,
            withFirst(month(1450, 1, Triple(1407, 4, 0))) to OverrideProblem.INVALID_MONTH,
            withFirst(month(1450, 1, Triple(1407, 7, 31))) to OverrideProblem.INVALID_MONTH,
            file(first, month(1451, 2, Triple(1407, 5, 19))) to OverrideProblem.INVALID_MONTH,
            file(first, month(1450, 2, Triple(1407, 5, 17))) to OverrideProblem.INVALID_LENGTH,
            file(month(1450, 1, Triple(1407, 4, 20), "5"), second) to OverrideProblem.MISSING_CITATION,
            file(month(1450, 1, Triple(1407, 4, 20), CITATION.replace("\"1\"", "1")), second) to
                OverrideProblem.MISSING_CITATION,
            file(month(1450, 1, Triple(1407, 4, 20), CITATION.replace("Test calendar", " ")), second) to
                OverrideProblem.MISSING_CITATION,
        ).forEach { (text, problem) -> withClue(text) { problemOf(text) shouldBe problem } }
    }

    @Test
    fun `months run on across the end of a Hijri year`() {
        val text = file(month(1450, 12, Triple(1408, 2, 9)), month(1451, 1, Triple(1408, 3, 8)))
        val table = IslamicMonthOverrides.parse(text).getOrThrow().table

        table.next shouldBe (1451 to 1)
        IranIslamicCalendar(table).hasOverride shouldBe true
        IranIslamicCalendar().hasOverride shouldBe false
    }

    @Test
    fun `month tables validate their months and compare by value`() {
        val table = IslamicMonthTable(1450, 1, 2_400_000, listOf(29, 30))
        shouldThrow<IllegalArgumentException> { IslamicMonthTable(1450, 0, 2_400_000, listOf(29)) }
        shouldThrow<IllegalArgumentException> { IslamicMonthTable(1450, 13, 2_400_000, listOf(29)) }
        shouldThrow<IllegalArgumentException> { IslamicMonthTable(1450, 1, 2_400_000, emptyList()) }
        shouldThrow<IllegalArgumentException> { IslamicMonthTable(1450, 1, 2_400_000, listOf(28)) }

        table shouldBe IslamicMonthTable(1450, 1, 2_400_000, listOf(29, 30))
        table.hashCode() shouldBe IslamicMonthTable(1450, 1, 2_400_000, listOf(29, 30)).hashCode()
        listOf(
            IslamicMonthTable(1451, 1, 2_400_000, listOf(29, 30)),
            IslamicMonthTable(1450, 2, 2_400_000, listOf(29, 30)),
            IslamicMonthTable(1450, 1, 2_400_001, listOf(29, 30)),
            IslamicMonthTable(1450, 1, 2_400_000, listOf(30, 30)),
        ).forEach { table shouldNotBe it }
        table.equals("table") shouldBe false
    }

    private companion object {
        const val CITATION =
            """{"url": "https://example.org/", "title": "Test calendar", "page": "1", "retrieved": "2026-09-17"}"""
    }
}
