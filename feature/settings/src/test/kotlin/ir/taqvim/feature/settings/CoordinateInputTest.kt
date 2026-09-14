/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import ir.taqvim.core.i18n.NumeralSystem
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.testing.PropertyTesting
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-1502: typed coordinates in any digit script, their ranges, and their display. */
class CoordinateInputTest {
    @Test
    fun `displayed values parse back in every digit script`(): Unit =
        runBlocking {
            checkAll(
                PropertyTesting.iterations,
                Arb.int(-900_000..900_000),
                Arb.int(-1_800_000..1_800_000),
                Arb.enum<NumeralSystem>(),
            ) { latitude, longitude, numerals ->
                val lat = latitude / 10_000.0
                val lon = longitude / 10_000.0
                CoordinateInput.latitude(CoordinateInput.decimal(lat, numerals)) shouldBe lat
                CoordinateInput.longitude(CoordinateInput.decimal(lon, numerals)) shouldBe lon
                val text = CoordinateInput.format(Coordinates(lat, lon), numerals)
                val (latText, lonText) = text.split(CoordinateInput.separator(numerals))
                CoordinateInput.coordinates(latText, lonText) shouldBe Coordinates(lat, lon)
            }
        }

    @Test
    fun `ranges and malformed text are rejected`() {
        CoordinateInput.latitude("90") shouldBe 90.0
        CoordinateInput.latitude("-90") shouldBe -90.0
        CoordinateInput.longitude("180") shouldBe 180.0
        CoordinateInput.longitude(" -180 ") shouldBe -180.0
        listOf("90.0001", "-90.5", "", "  ", "abc", "1.2.3", "NaN", "1e3").forEach {
            CoordinateInput.latitude(it).shouldBeNull()
        }
        CoordinateInput.longitude("180.01").shouldBeNull()
        CoordinateInput.coordinates("35", "200").shouldBeNull()
        CoordinateInput.coordinates("north", "51").shouldBeNull()
    }

    @Test
    fun `Persian digits and the Arabic decimal separator are accepted and shown`() {
        val persianLatitude =
            Numerals.localizeDigits("29", NumeralSystem.PERSIAN) + ARABIC_DECIMAL_SEPARATOR +
                Numerals.localizeDigits("61", NumeralSystem.PERSIAN)
        CoordinateInput.latitude(persianLatitude) shouldBe 29.61
        CoordinateInput.format(Coordinates(29.61, 52.53), NumeralSystem.LATIN) shouldBe "29.6100, 52.5300"
        CoordinateInput.separator(NumeralSystem.PERSIAN) shouldBe "، "
        CoordinateInput.separator(NumeralSystem.EASTERN_ARABIC) shouldBe "، "
        CoordinateInput.separator(NumeralSystem.DEVANAGARI) shouldBe ", "
        CoordinateInput.decimal(-0.00005, NumeralSystem.LATIN) shouldBe "-0.0001"
        CoordinateInput.decimal(29.61, NumeralSystem.PERSIAN) shouldBe
            Numerals.format(java.math.BigDecimal("29.6100"), NumeralSystem.PERSIAN)
    }

    @Test
    fun `platform time zone ids`() {
        PlatformTimeZoneIds.isKnown("Asia/Tehran") shouldBe true
        PlatformTimeZoneIds.isKnown("Asia/Kabul") shouldBe true
        PlatformTimeZoneIds.isKnown("Mars/Olympus") shouldBe false
        PlatformTimeZoneIds.isKnown("") shouldBe false
    }

    private companion object {
        const val ARABIC_DECIMAL_SEPARATOR = '٫'
    }
}
