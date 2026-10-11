/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.events

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class LocalizedTextFallbackTest {
    @Test
    fun `exact translation wins over reference and original`() {
        val text = LocalizedText(mapOf("fa" to "original", "en" to "reference", "fr" to "translation"))
        text.forLanguage("fr") shouldBe "translation"
        text.forLanguage("fr-CA") shouldBe "translation"
        text.forLanguage("FR_ca") shouldBe "translation"
        text.forLanguage("de") shouldBe "reference"
    }

    @Test
    fun `Dari prefers Persian before English but unrelated Arabic does not`() {
        val text = LocalizedText(mapOf("fa" to "Persian", "en" to "English"))
        text.forLanguage("prs") shouldBe "Persian"
        text.forLanguage("ar") shouldBe "English"
        text.copy(texts = text.texts + ("prs" to "Dari")).forLanguage("fa-AF") shouldBe "Dari"
    }

    @Test
    fun `original Nepali remains the last resort`() {
        val text = LocalizedText(mapOf("ne" to "original"))
        text.forLanguage("fr") shouldBe "original"
        text.copy(texts = text.texts + ("en" to "reference")).forLanguage("fr") shouldBe "reference"
    }

    @Test
    fun `Android aliases resolve without conflating Kurdish scripts`() {
        val text =
            LocalizedText(
                mapOf(
                    "fa" to "original",
                    "kmr" to "Kurmanji",
                    "id" to "Indonesian",
                    "en" to "reference",
                ),
            )
        text.forLanguage("ku") shouldBe "Kurmanji"
        text.forLanguage("in-ID") shouldBe "Indonesian"
        text.forLanguage("ckb") shouldBe "reference"
    }
}
