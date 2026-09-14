/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.preferences

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import ir.taqvim.core.i18n.LanguageTable
import org.junit.jupiter.api.Test

/** T-1501: device and app locales map to the launch language their users expect (ADR-0023). */
class DeviceLanguagesTest {
    @Test
    fun `every launch language's own locale and code map back to it`() {
        LanguageTable.languages.forEach { spec ->
            withClue(spec.code) {
                DeviceLanguages.match(spec.localeTag) shouldBe spec.code
                DeviceLanguages.match(spec.code) shouldBe spec.code
            }
        }
    }

    @Test
    fun `regions and macro-language tags pick the right language`() {
        mapOf(
            "fa" to "fa",
            "fa-IR" to "fa",
            "fa-AF" to "prs",
            "fa-DE" to "fa",
            "ps" to "ps",
            "ku" to "kmr",
            "ku-TR" to "kmr",
            "ckb" to "ckb",
            "ar-EG" to "ar",
            "ne-NP" to "ne",
            "zh-Hans-CN" to "zh",
            "zh-TW" to "zh",
            "en-GB" to "en",
            "pt-BR" to "en",
            "" to "en",
            "und" to "en",
            "fa_AF" to "prs",
        ).forEach { (tag, code) ->
            withClue(tag) { DeviceLanguages.match(tag) shouldBe code }
        }
        DeviceLanguages.find("pt-BR") shouldBe null
    }
}
