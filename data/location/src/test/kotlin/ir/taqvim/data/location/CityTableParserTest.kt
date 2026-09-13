/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.location

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import ir.taqvim.core.model.Coordinates
import org.junit.jupiter.api.Test

/** T-603: the bundled table format — header, same-as-English columns, name fallback and rejection of bad rows. */
class CityTableParserTest {
    private val base = listOf("neId", "country", "region", "latitude", "longitude", "timeZone", "population", "en")
    private val columns = base + (City.PUBLISHED_LANGUAGES - City.ENGLISH).sorted()

    private val tehran =
        mapOf(
            "neId" to "1159151551",
            "country" to "IR",
            "region" to "Tehran",
            "latitude" to "35.67389",
            "longitude" to "51.42240",
            "timeZone" to "Asia/Tehran",
            "population" to "7873000",
            "en" to "Tehran",
            "fa" to "تهران",
            "de" to "Teheran",
        )

    private fun header(names: List<String> = columns) = "# columns: " + names.joinToString(",")

    private fun row(
        values: Map<String, String>,
        names: List<String> = columns,
    ) = names.joinToString("\t") { values[it].orEmpty() }

    private fun parse(vararg lines: String): List<City> = CityTableParser.parse(lines.asSequence())

    @Test
    fun `a row keeps published names and empty localized columns mean the English spelling`() {
        val city = parse("# source: test table", header(), row(tehran)).single()

        city.id shouldBe 1_159_151_551L
        city.countryCode shouldBe "IR"
        city.region shouldBe "Tehran"
        city.coordinates shouldBe Coordinates(35.67389, 51.4224)
        city.timeZoneId shouldBe "Asia/Tehran"
        city.population shouldBe 7_873_000L
        city.localizedNames shouldBe mapOf("fa" to "تهران", "de" to "Teheran")
        city.name("fa") shouldBe "تهران"
        city.name("de") shouldBe "Teheran"
        city.name("es") shouldBe "Tehran"
        city.name("en") shouldBe "Tehran"
        city.name("prs") shouldBe "تهران"
        city.name("ps") shouldBe "Tehran"
        city.hasPublishedName("es") shouldBe true
        city.hasPublishedName("ps") shouldBe false
    }

    @Test
    fun `optional fields may be empty and columns may come in any order`() {
        val reordered = columns.reversed()
        val values = mapOf("neId" to "7", "latitude" to "-33.544", "longitude" to "-56.901", "en" to "Trinidad")

        val city = parse(header(reordered), "", row(values, reordered)).single()

        city.englishName shouldBe "Trinidad"
        city.coordinates shouldBe Coordinates(-33.544, -56.901)
        city.countryCode.shouldBeNull()
        city.region.shouldBeNull()
        city.timeZoneId.shouldBeNull()
        city.population.shouldBeNull()
        city.localizedNames.keys.shouldBeEmpty()
    }

    @Test
    fun `malformed tables are rejected with the line number`() {
        shouldThrow<IllegalArgumentException> { parse(row(tehran)) }.message shouldContain
            "line 1: row before the columns header"
        shouldThrow<IllegalArgumentException> { parse(header(columns - "tr")) }.message shouldContain "lacks [tr]"
        shouldThrow<IllegalArgumentException> { parse(header(columns + "neId")) }.message shouldContain "repeats"
        shouldThrow<IllegalArgumentException> { parse(header(), row(tehran) + "\textra") }.message shouldContain
            "line 2: expected 21 fields, found 22"
        listOf(
            ("neId" to "x") to "invalid id",
            ("latitude" to "north") to "invalid latitude",
            ("longitude" to "181") to "longitude must be within",
            ("population" to "-99") to "invalid population",
            ("en" to " ") to "missing English name",
        ).forEach { (change, message) ->
            val error = shouldThrow<IllegalArgumentException> { parse(header(), row(tehran + change)) }
            error.message shouldContain "line 2: "
            error.message shouldContain message
        }
    }
}
