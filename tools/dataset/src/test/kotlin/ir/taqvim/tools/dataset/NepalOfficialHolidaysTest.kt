/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.tools.dataset

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import ir.taqvim.core.calendar.GregorianCalendarSystem
import ir.taqvim.core.calendar.NepaliCalendarSystem
import ir.taqvim.core.calendar.NepaliLunarDays
import ir.taqvim.core.calendar.TithiObservance
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.core.model.Jdn
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/**
 * D-04: Nepal's public holidays and national days from the Ministry of Home Affairs notices for BS 2082 and 2083.
 * Every record is a recurring rule (ADR-0036; lunar festivals ADR-0038). Per year, the days the rules give must equal
 * the dated days of the notice in both directions; undated holidays (Eid) must be records of that year.
 */
class NepalOfficialHolidaysTest {
    private val datasetText = File(property("taqvim.dataset.directory"), DATASET_FILE).readText()
    private val events: List<JsonObject> =
        ((Json.parseToJsonElement(datasetText) as? JsonObject)?.get("events") as? JsonArray)
            .orEmpty()
            .mapNotNull { it as? JsonObject }

    @Test
    fun `the Nepal holiday dataset passes the validator`() {
        DatasetValidator(schemaText()).validate(mapOf(DATASET_FILE to datasetText)).shouldBeEmpty()
    }

    @TestFactory
    fun `rules give exactly the dated days of each notice`(): List<DynamicTest> =
        YEARS.map { year ->
            DynamicTest.dynamicTest("BS $year") {
                val expected = goldenRows().filter { it.year == year && it.day != null }.map { it.id to it.day }.toSet()
                val produced =
                    events
                        .filter { validIn(it, year) && it.text("calendar") != ISLAMIC }
                        .flatMap { event -> nepaliDays(event, year).map { event.text("id") to it } }
                        .toSet()
                produced shouldBe expected
            }
        }

    @TestFactory
    fun `each notice's holidays are exactly the records valid in its year`(): List<DynamicTest> =
        YEARS.map { year ->
            DynamicTest.dynamicTest("BS $year") {
                val listed = goldenRows().filter { it.year == year }.map { it.id }.toSet()
                val valid = events.filter { validIn(it, year) }.mapNotNull { it.text("id") }.toSet()
                // A lunar festival can skip a year (Ram Navami 2083 falls in Baisakh 2084); the notice omits it then.
                val unlistedWithoutDay =
                    events
                        .filter { it.text("id") !in listed && nepaliDays(it, year).isEmpty() }
                        .mapNotNull { it.text("id") }
                        .toSet()
                valid - unlistedWithoutDay shouldBe listed
            }
        }

    @Test
    fun `every record is an official Nepal record with a Nepali title and cited notice pages`() {
        events
            .filterNot { event ->
                val citations = (event["citations"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
                event.text("source") == "NEPAL_OFFICIAL" &&
                    (event["title"] as? JsonObject)?.keys == setOf("ne") &&
                    citations.isNotEmpty() &&
                    citations.all { it.text("url")?.startsWith(MOHA) == true && !it.text("page").isNullOrBlank() }
            }.map { it.text("id") }
            .shouldBeEmpty()
    }

    /** The Bikram Sambat days (month to day) that [event]'s rule gives in [year], ignoring validity. */
    private fun nepaliDays(
        event: JsonObject,
        year: Int,
    ): List<Pair<Int, Int>> {
        val rule = event["rule"] as? JsonObject ?: return emptyList()
        val month = rule.int("month") ?: return emptyList()
        val days =
            when (rule.text("type")) {
                "LunarTithi" -> lunarDays(rule, year, month)
                "Fixed" -> fixedDays(event.text("calendar"), year, month, rule.int("day") ?: 0)
                else -> emptyList()
            }
        return days.map { NepaliCalendarSystem.fromJdn(it) }.map { it.month to it.day }
    }

    private fun lunarDays(
        rule: JsonObject,
        year: Int,
        month: Int,
    ): List<Jdn> {
        val observance = TithiObservance.valueOf(rule.text("observance") ?: "")
        val tithi = rule.int("tithi") ?: 0
        return NepaliLunarDays.days(
            year,
            month,
            tithi,
            observance,
            rule.int("endTithi"),
            rule.int("endOffsetDays") ?: 0,
        )
    }

    private fun fixedDays(
        calendar: String?,
        year: Int,
        month: Int,
        day: Int,
    ): List<Jdn> =
        when (calendar) {
            "NEPALI" -> {
                listOf(NepaliCalendarSystem.toJdn(CalendarDate(CalendarSystem.NEPALI, year, month, day)))
            }

            "GREGORIAN" -> {
                (year - BS_TO_AD_MAX..year - BS_TO_AD_MIN)
                    .map { GregorianCalendarSystem.toJdn(CalendarDate(CalendarSystem.GREGORIAN, it, month, day)) }
                    .filter { NepaliCalendarSystem.fromJdn(it).year == year }
            }

            else -> {
                emptyList()
            }
        }

    private fun validIn(
        event: JsonObject,
        year: Int,
    ): Boolean {
        val validity = event["validity"] as? JsonObject ?: return true
        return validity.text("calendar") == "NEPALI" &&
            (validity.int("fromYear") ?: Int.MIN_VALUE) <= year &&
            year <= (validity.int("toYear") ?: Int.MAX_VALUE)
    }

    private fun goldenRows(): List<GoldenRow> =
        requireNotNull(javaClass.getResource(GOLDEN)) { "missing $GOLDEN" }
            .readText()
            .lines()
            .filterNot { it.startsWith("#") || it.isBlank() }
            .drop(1)
            .map { line ->
                val columns = line.split(',')
                val day = columns[3].toIntOrNull()?.let { columns[2].toInt() to it }
                GoldenRow(columns[0].toInt(), columns[1], day)
            }

    private data class GoldenRow(
        val year: Int,
        val id: String,
        val day: Pair<Int, Int>?,
    )

    private companion object {
        const val DATASET_FILE = "nepal/nepal-official-holidays.json"
        const val GOLDEN = "/golden/nepal/nepal-official-holidays.csv"
        const val MOHA = "https://moha.gov.np/upload/"
        const val ISLAMIC = "ISLAMIC"
        const val BS_TO_AD_MAX = 57
        const val BS_TO_AD_MIN = 56
        val YEARS = listOf(2082, 2083)

        fun property(name: String): String = requireNotNull(System.getProperty(name)) { "$name is not set" }

        fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

        fun JsonObject.int(key: String): Int? =
            (this[key] as? JsonPrimitive)?.takeUnless { it.isString }?.content?.toIntOrNull()
    }
}
