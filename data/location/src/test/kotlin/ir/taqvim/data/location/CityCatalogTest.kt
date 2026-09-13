/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.location

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ir.taqvim.core.model.Coordinates
import org.junit.jupiter.api.Test

/**
 * T-603: the bundled Natural Earth list (values checked against the source file, see the provenance header of
 * `cities.tsv`), search keys, ranking and nearest place.
 */
class CityCatalogTest {
    private fun city(
        id: Long,
        english: String,
        population: Long? = null,
        latitude: Double = 0.0,
        longitude: Double = 0.0,
    ) = City(id, english, emptyMap(), null, null, Coordinates(latitude, longitude), null, population)

    @Test
    fun `the bundled list loads every Natural Earth place`() {
        bundled.cities.size shouldBe NATURAL_EARTH_PLACES
        bundled.cities
            .map(City::id)
            .toSet()
            .size shouldBe NATURAL_EARTH_PLACES
        bundled.cities.none { it.englishName.isBlank() } shouldBe true

        val tehran = bundled.city(TEHRAN).shouldNotBeNull()
        tehran.englishName shouldBe "Tehran"
        tehran.countryCode shouldBe "IR"
        tehran.timeZoneId shouldBe "Asia/Tehran"
        tehran.population shouldBe 7_873_000L
        tehran.coordinates.latitude shouldBe (35.67389 plusOrMinus 1e-9)
        tehran.coordinates.longitude shouldBe (51.4224 plusOrMinus 1e-9)
        tehran.name("fa") shouldBe "تهران"
        tehran.name("tr") shouldBe "Tahran"
        tehran.name("ja") shouldBe "テヘラン"
        bundled.city(-1L).shouldBeNull()
    }

    @Test
    fun `search ignores case, diacritics and Arabic letter variants`() {
        bundled.search("tehran").first().id shouldBe TEHRAN
        bundled.search("TÉHÉRAN").first().id shouldBe TEHRAN
        // Arabic kaf (U+0643) finds the Persian spelling with keheh (U+06A9).
        bundled.search("كرج").first().id shouldBe KARAJ
        CityCatalog.searchKey("Zürich") shouldBe "zurich"
    }

    @Test
    fun `prefix matches rank first, then larger populations`() {
        val catalog =
            CityCatalog(
                listOf(city(1, "Newark", 10), city(2, "New York", 100), city(3, "Hanover New", 1000), city(4, "Paris")),
            )

        catalog.search("new").map(City::id) shouldBe listOf(2L, 1L, 3L)
        catalog.search("new", limit = 1).map(City::id) shouldBe listOf(2L)
        catalog.search("a").map(City::id) shouldBe listOf(3L, 1L, 4L)
        catalog.search("  ").shouldBeEmpty()
        catalog.search("zzz").shouldBeEmpty()
        shouldThrow<IllegalArgumentException> { catalog.search("new", limit = 0) }
    }

    @Test
    fun `nearest uses great-circle distance across the antimeridian and near the poles`() {
        val catalog =
            CityCatalog(
                listOf(
                    city(1, "East", longitude = 179.9),
                    city(2, "West", longitude = 170.0),
                    city(3, "Pole", null, 89.0),
                ),
            )

        catalog.nearest(Coordinates(0.0, -179.9))?.id shouldBe 1L
        catalog.nearest(Coordinates(85.0, 100.0))?.id shouldBe 3L
        CityCatalog(emptyList()).nearest(Coordinates(0.0, 0.0)).shouldBeNull()
        bundled.nearest(Coordinates(35.80, 50.97))?.id shouldBe KARAJ
    }

    private companion object {
        const val NATURAL_EARTH_PLACES = 7342
        const val TEHRAN = 1_159_151_551L
        const val KARAJ = 1_159_142_617L
        val bundled: CityCatalog by lazy { CityCatalog.loadBundled() }
    }
}
