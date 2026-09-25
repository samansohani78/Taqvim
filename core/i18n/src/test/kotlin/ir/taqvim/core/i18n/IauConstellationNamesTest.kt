/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.i18n

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import org.junit.jupiter.api.Test

/**
 * [IauConstellationNames] holds exactly the 88 IAU constellations (DT-026), keyed by the abbreviation the astronomy
 * façade (`io.github.cosinekitty.astronomy`, via `core/astronomy`'s `Zodiac`) returns. The expected abbreviation set
 * below was read from that library's own bytecode (its `ConstellationText` table), not recalled from memory, so a
 * mismatch here means the app would fail to find a name for a real result.
 */
class IauConstellationNamesTest {
    @Test
    fun `every IAU constellation abbreviation the astronomy library returns has a name`() {
        IauConstellationNames.abbreviations shouldContainExactlyInAnyOrder EXPECTED_ABBREVIATIONS
    }

    @Test
    fun `there are exactly 88 constellations, each with a non-blank name`() {
        IauConstellationNames.abbreviations.size shouldBe EXPECTED_ABBREVIATIONS.size
        EXPECTED_ABBREVIATIONS.forEach { abbreviation -> IauConstellationNames.name(abbreviation).shouldNotBeBlank() }
    }

    @Test
    fun `spot-checked names match the IAU's official spelling`() {
        IauConstellationNames.name("Sco") shouldBe "Scorpius"
        IauConstellationNames.name("Leo") shouldBe "Leo"
        IauConstellationNames.name("Vir") shouldBe "Virgo"
        IauConstellationNames.name("Tau") shouldBe "Taurus"
        IauConstellationNames.name("UMi") shouldBe "Ursa Minor"
        IauConstellationNames.name("UMa") shouldBe "Ursa Major"
        IauConstellationNames.name("CVn") shouldBe "Canes Venatici"
        IauConstellationNames.name("PsA") shouldBe "Piscis Austrinus"
        IauConstellationNames.name("Cam") shouldBe "Camelopardalis"
        IauConstellationNames.name("Ant") shouldBe "Antlia"
    }

    @Test
    fun `an unknown abbreviation has no name`() {
        IauConstellationNames.name("Xyz") shouldBe null
    }

    @Test
    fun `Japanese has a sourced name for all 88 (NAOJ, DT-026), other languages fall back to Latin`() {
        IauConstellationNames.localizedLanguages shouldBe setOf("ja")
        EXPECTED_ABBREVIATIONS.forEach { abbreviation ->
            IauConstellationNames.hasLocalizedName(abbreviation, "ja") shouldBe true
            IauConstellationNames.name(abbreviation, "ja").shouldNotBeBlank()
        }
        IauConstellationNames.name("Sco", "ja") shouldBe "さそり座"
        IauConstellationNames.name("Ori", "ja") shouldBe "オリオン座"
        IauConstellationNames.name("Ser", "ja") shouldBe "へび座"
        IauConstellationNames.hasLocalizedName("Sco", "fa") shouldBe false
        IauConstellationNames.name("Sco", "fa") shouldBe "Scorpius"
    }

    private companion object {
        val EXPECTED_ABBREVIATIONS =
            setOf(
                "And",
                "Ant",
                "Aps",
                "Aql",
                "Aqr",
                "Ara",
                "Ari",
                "Aur",
                "Boo",
                "Cae",
                "Cam",
                "Cap",
                "Car",
                "Cas",
                "Cen",
                "Cep",
                "Cet",
                "Cha",
                "Cir",
                "CMa",
                "CMi",
                "Cnc",
                "Col",
                "Com",
                "CrA",
                "CrB",
                "Crt",
                "Cru",
                "Crv",
                "CVn",
                "Cyg",
                "Del",
                "Dor",
                "Dra",
                "Equ",
                "Eri",
                "For",
                "Gem",
                "Gru",
                "Her",
                "Hor",
                "Hya",
                "Hyi",
                "Ind",
                "Lac",
                "Leo",
                "Lep",
                "Lib",
                "LMi",
                "Lup",
                "Lyn",
                "Lyr",
                "Men",
                "Mic",
                "Mon",
                "Mus",
                "Nor",
                "Oct",
                "Oph",
                "Ori",
                "Pav",
                "Peg",
                "Per",
                "Phe",
                "Pic",
                "PsA",
                "Psc",
                "Pup",
                "Pyx",
                "Ret",
                "Scl",
                "Sco",
                "Sct",
                "Ser",
                "Sex",
                "Sge",
                "Sgr",
                "Tau",
                "Tel",
                "TrA",
                "Tri",
                "Tuc",
                "UMa",
                "UMi",
                "Vel",
                "Vir",
                "Vol",
                "Vul",
            )
    }
}
