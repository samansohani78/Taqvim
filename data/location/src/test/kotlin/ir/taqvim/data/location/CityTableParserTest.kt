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

/** T-603: the bundled table format — header, column-major body, same-as-English columns and rejection of bad input. */
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

    private val karaj =
        mapOf(
            "neId" to "1159144071",
            "country" to "IR",
            "region" to "Alborz",
            "latitude" to "35.83266",
            "longitude" to "50.99155",
            "timeZone" to "Asia/Tehran",
            "population" to "1967005",
            "en" to "Karaj",
            "fa" to "کرج",
            "kmr" to "Kerec",
        )

    private fun header(names: List<String> = columns) = "# columns: " + names.joinToString(",")

    /** The body of a table: one line per column, holding that column's value for every place. */
    private fun body(
        places: List<Map<String, String>>,
        names: List<String> = columns,
    ) = names.map { name -> places.joinToString("\t") { it[name].orEmpty() } }

    private fun parse(vararg lines: String): List<City> = CityTableParser.parse(lines.asSequence())

    private fun parse(
        places: List<Map<String, String>>,
        names: List<String> = columns,
    ): List<City> = CityTableParser.parse((listOf(header(names)) + body(places, names)).asSequence())

    @Test
    fun `a place keeps published names and empty localized columns mean the English spelling`() {
        val city = parse(listOf(tehran)).single()

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
        city.name("kmr") shouldBe "Tehran"
        city.hasPublishedName("es") shouldBe true
        city.hasPublishedName("ps") shouldBe true
        city.hasPublishedName("kmr") shouldBe true
    }

    @Test
    fun `a place names in Kurmanji, which DT-020 reads from the Latin-script half of GeoNames' ku tag`() {
        val karaj = parse(listOf(karaj)).single()

        karaj.name("kmr") shouldBe "Kerec"
        karaj.name("ckb") shouldBe "Karaj"
        karaj.hasPublishedName("kmr") shouldBe true
    }

    @Test
    fun `the column lines stay aligned, so every place keeps its own values`() {
        val cities = parse(listOf(tehran, karaj))

        cities.map(City::englishName) shouldBe listOf("Tehran", "Karaj")
        cities.map { it.name("fa") } shouldBe listOf("تهران", "کرج")
        cities.map(City::region) shouldBe listOf("Tehran", "Alborz")
        cities.map(City::population) shouldBe listOf(7_873_000L, 1_967_005L)
        cities.map { it.coordinates.longitude } shouldBe listOf(51.4224, 50.99155)
    }

    @Test
    fun `optional fields may be empty and columns may come in any order`() {
        val reordered = columns.reversed()
        val values = mapOf("neId" to "7", "latitude" to "-33.544", "longitude" to "-56.901", "en" to "Trinidad")

        val city = parse(listOf(values), reordered).single()

        city.englishName shouldBe "Trinidad"
        city.coordinates shouldBe Coordinates(-33.544, -56.901)
        city.countryCode.shouldBeNull()
        city.region.shouldBeNull()
        city.timeZoneId.shouldBeNull()
        city.population.shouldBeNull()
        city.localizedNames.keys.shouldBeEmpty()
    }

    @Test
    fun `a malformed header or body shape is rejected`() {
        shouldThrow<IllegalArgumentException> { parse(body(listOf(tehran)).first()) }.message shouldContain
            "line 1: a column before the columns header"
        shouldThrow<IllegalArgumentException> { parse("# source: test table") }.message shouldContain
            "the columns header is missing"
        shouldThrow<IllegalArgumentException> { parse(header(columns - "tr")) }.message shouldContain "lacks [tr]"
        shouldThrow<IllegalArgumentException> { parse(header(columns + "neId")) }.message shouldContain "repeats"
        shouldThrow<IllegalArgumentException> { parse(header()) }.message shouldContain
            "expected ${columns.size} column lines, found 0"
        shouldThrow<IllegalArgumentException> {
            parse(*(listOf(header()) + body(listOf(tehran)) + "extra").toTypedArray())
        }.message shouldContain "expected ${columns.size} column lines, found ${columns.size + 1}"
        shouldThrow<IllegalArgumentException> {
            val ragged = body(listOf(tehran)).toMutableList().also { it[1] = it[1] + "\tIR" }
            parse(*(listOf(header()) + ragged).toTypedArray())
        }.message shouldContain "column 'country' has 2 places, expected 1"
    }

    @Test
    fun `a column that is empty for every place is a line of its own, not a blank to skip`() {
        val nameless = mapOf("neId" to "7", "latitude" to "1.0", "longitude" to "2.0", "en" to "Trinidad")

        val city = parse(listOf(nameless)).single()

        city.englishName shouldBe "Trinidad"
        city.localizedNames.keys.shouldBeEmpty()
    }

    @Test
    fun `a blank line after the last column is ignored, as a final newline produces one`() {
        val cities = parse(*(listOf(header()) + body(listOf(tehran, karaj)) + "").toTypedArray())

        cities.map(City::englishName) shouldBe listOf("Tehran", "Karaj")
    }

    @Test
    fun `an unreadable value names the place it belongs to`() {
        listOf(
            ("neId" to "x") to "invalid id",
            ("latitude" to "north") to "invalid latitude",
            ("longitude" to "181") to "longitude must be within",
            ("population" to "-99") to "invalid population",
            ("en" to " ") to "missing English name",
        ).forEach { (change, message) ->
            val error = shouldThrow<IllegalArgumentException> { parse(listOf(karaj, tehran + change)) }
            error.message shouldContain "place 2: "
            error.message shouldContain message
        }
    }
}
